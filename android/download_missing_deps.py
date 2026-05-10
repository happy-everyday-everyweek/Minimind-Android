#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
递归解析 AGP 8.2.0 POM 文件，下载所有缺失的传递依赖到本地 Maven 仓库
"""

import os
import sys
import urllib.request
import xml.etree.ElementTree as ET
from urllib.error import URLError, HTTPError
import time
import ssl
import re

LOCAL_REPO = os.path.join(os.path.dirname(os.path.abspath(__file__)), "local-maven-repo")

REPO_URLS = [
    "https://maven.aliyun.com/repository/google",
    "https://maven.aliyun.com/repository/central",
    "https://repo1.maven.org/maven2",
]

TIMEOUT = 30
MAX_DEPTH = 10
MAX_RETRIES = 3
RETRY_DELAY = 2

MAVEN_NS = "http://maven.apache.org/POM/4.0.0"

downloaded_artifacts = set()
skipped_artifacts = set()
visited_poms = set()
stats = {"downloaded": 0, "skipped": 0, "existing": 0, "pom_parsed": 0}

ssl_ctx = ssl.create_default_context()


def maven_path(group_id, artifact_id, version):
    group_path = group_id.replace(".", os.sep)
    return os.path.join(LOCAL_REPO, group_path, artifact_id, version)


def maven_url(repo_url, group_id, artifact_id, version, ext):
    group_path = group_id.replace(".", "/")
    filename = "{}-{}.{}".format(artifact_id, version, ext)
    return "{}/{}/{}/{}/{}".format(repo_url, group_path, artifact_id, version, filename)


def metadata_url(repo_url, group_id, artifact_id):
    group_path = group_id.replace(".", "/")
    return "{}/{}/{}/maven-metadata.xml".format(repo_url, group_path, artifact_id)


def download_file(url, dest_path, retries=MAX_RETRIES):
    dest_dir = os.path.dirname(dest_path)
    if not os.path.exists(dest_dir):
        os.makedirs(dest_dir, exist_ok=True)
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url)
            req.add_header("User-Agent", "Mozilla/5.0")
            resp = urllib.request.urlopen(req, timeout=TIMEOUT, context=ssl_ctx)
            data = resp.read()
            with open(dest_path, "wb") as f:
                f.write(data)
            return True
        except HTTPError as e:
            if e.code == 429:
                wait = RETRY_DELAY * (attempt + 2)
                time.sleep(wait)
                continue
            return False
        except (URLError, OSError):
            return False
    return False


def try_download(group_id, artifact_id, version, ext):
    key = "{}:{}:{}:{}".format(group_id, artifact_id, version, ext)
    if key in downloaded_artifacts:
        return True
    if key in skipped_artifacts:
        return False

    base_dir = maven_path(group_id, artifact_id, version)
    filename = "{}-{}.{}".format(artifact_id, version, ext)
    dest_path = os.path.join(base_dir, filename)

    if os.path.exists(dest_path) and os.path.getsize(dest_path) > 0:
        downloaded_artifacts.add(key)
        stats["existing"] += 1
        return True

    for repo_url in REPO_URLS:
        url = maven_url(repo_url, group_id, artifact_id, version, ext)
        if download_file(url, dest_path):
            downloaded_artifacts.add(key)
            stats["downloaded"] += 1
            return True

    skipped_artifacts.add(key)
    stats["skipped"] += 1
    return False


def resolve_version_range(group_id, artifact_id, version_spec):
    if not (version_spec.startswith("[") or version_spec.startswith("(")):
        return version_spec

    match = re.match(r'^\[(.+)\]$', version_spec)
    if match:
        return match.group(1)

    for repo_url in REPO_URLS:
        url = metadata_url(repo_url, group_id, artifact_id)
        try:
            req = urllib.request.Request(url)
            req.add_header("User-Agent", "Mozilla/5.0")
            resp = urllib.request.urlopen(req, timeout=TIMEOUT, context=ssl_ctx)
            data = resp.read()
            root = ET.fromstring(data)
            ns = ""
            if root.tag.startswith("{"):
                ns = root.tag.split("}")[0] + "}"

            release = root.find(".//{}release".format(ns))
            if release is not None and release.text:
                return release.text

            latest = root.find(".//{}latest".format(ns))
            if latest is not None and latest.text:
                return latest.text

            versioning = root.find("{}versioning".format(ns))
            if versioning is not None:
                versions_el = versioning.find("{}versions".format(ns))
                if versions_el is not None:
                    versions = [v.text for v in versions_el.findall("{}version".format(ns))]
                    if versions:
                        return versions[-1]
        except (HTTPError, URLError, OSError, ET.ParseError):
            continue

    return version_spec


def parse_pom(pom_path):
    deps = []
    packaging_type = "jar"
    parent_info = (None, None, None)
    props = {}

    if not os.path.exists(pom_path):
        return deps, packaging_type, parent_info, props

    try:
        tree = ET.parse(pom_path)
        root = tree.getroot()
        ns = ""
        if root.tag.startswith("{"):
            ns = root.tag.split("}")[0] + "}"

        parent = root.find("{}parent".format(ns))
        if parent is not None:
            pg = parent.find("{}groupId".format(ns))
            pa = parent.find("{}artifactId".format(ns))
            pv = parent.find("{}version".format(ns))
            parent_info = (
                pg.text if pg is not None else None,
                pa.text if pa is not None else None,
                pv.text if pv is not None else None,
            )

        project_group = root.find("{}groupId".format(ns))
        project_version = root.find("{}version".format(ns))

        effective_group = project_group.text if project_group is not None else parent_info[0]
        effective_version = project_version.text if project_version is not None else parent_info[2]

        properties_el = root.find("{}properties".format(ns))
        if properties_el is not None:
            for child in properties_el:
                tag = child.tag
                if tag.startswith("{"):
                    tag = tag.split("}")[1]
                if child.text:
                    props[tag] = child.text

        if effective_version:
            props["project.version"] = effective_version
        if effective_group:
            props["project.groupId"] = effective_group

        packaging = root.find("{}packaging".format(ns))
        if packaging is not None:
            packaging_type = packaging.text

        deps_section = root.find("{}dependencies".format(ns))
        if deps_section is None:
            return deps, packaging_type, parent_info, props

        for dep in deps_section.findall("{}dependency".format(ns)):
            g = dep.find("{}groupId".format(ns))
            a = dep.find("{}artifactId".format(ns))
            v = dep.find("{}version".format(ns))
            scope_el = dep.find("{}scope".format(ns))
            opt_el = dep.find("{}optional".format(ns))

            if g is None or a is None:
                continue

            scope = scope_el.text if scope_el is not None else "compile"
            optional = opt_el.text if opt_el is not None else "false"

            if scope in ("test", "provided", "system"):
                continue
            if optional == "true":
                continue

            dep_group = g.text
            dep_artifact = a.text
            dep_version = v.text if v is not None else None

            if dep_group and dep_group.startswith("$"):
                prop_name = dep_group[2:-1] if dep_group.endswith("}") else dep_group[1:]
                if prop_name in props:
                    dep_group = props[prop_name]
                else:
                    continue

            if dep_artifact and dep_artifact.startswith("$"):
                prop_name = dep_artifact[2:-1] if dep_artifact.endswith("}") else dep_artifact[1:]
                if prop_name in props:
                    dep_artifact = props[prop_name]
                else:
                    continue

            if dep_version and dep_version.startswith("$"):
                prop_name = dep_version[2:-1] if dep_version.endswith("}") else dep_version[1:]
                if prop_name in props:
                    dep_version = props[prop_name]
                else:
                    continue

            if dep_version is None:
                continue

            if dep_version.startswith("[") or dep_version.startswith("("):
                dep_version = resolve_version_range(dep_group, dep_artifact, dep_version)

            dep_type_el = dep.find("{}type".format(ns))
            dep_type = dep_type_el.text if dep_type_el is not None else "jar"

            deps.append((dep_group, dep_artifact, dep_version, dep_type))

    except (ET.ParseError, Exception):
        pass

    return deps, packaging_type, parent_info, props


def download_dep(group_id, artifact_id, version, depth=0):
    key = "{}:{}:{}".format(group_id, artifact_id, version)
    if key in visited_poms:
        return
    visited_poms.add(key)

    prefix = "  " * min(depth, 6)
    print("{}[深度{}] 处理: {}:{}:{}".format(prefix, depth, group_id, artifact_id, version))
    sys.stdout.flush()

    pom_ok = try_download(group_id, artifact_id, version, "pom")

    packaging_type = "jar"
    if pom_ok:
        base_dir = maven_path(group_id, artifact_id, version)
        pom_path = os.path.join(base_dir, "{}-{}.pom".format(artifact_id, version))
        _, packaging_type, _, _ = parse_pom(pom_path)
        stats["pom_parsed"] += 1

    if packaging_type == "pom":
        if pom_ok and depth < MAX_DEPTH:
            base_dir = maven_path(group_id, artifact_id, version)
            pom_path = os.path.join(base_dir, "{}-{}.pom".format(artifact_id, version))
            deps, _, parent_info, _ = parse_pom(pom_path)

            if parent_info[0] and parent_info[1] and parent_info[2]:
                download_dep(parent_info[0], parent_info[1], parent_info[2], depth + 1)

            for dep_group, dep_artifact, dep_version, dep_type in deps:
                download_dep(dep_group, dep_artifact, dep_version, depth + 1)
        return

    artifact_ok = False
    for ext in ["jar", "aar"]:
        result = try_download(group_id, artifact_id, version, ext)
        if result:
            artifact_ok = True
            break

    if not artifact_ok:
        print("{}  [警告] 构件下载失败: {}:{}:{}".format(prefix, group_id, artifact_id, version))

    if pom_ok and depth < MAX_DEPTH:
        base_dir = maven_path(group_id, artifact_id, version)
        pom_path = os.path.join(base_dir, "{}-{}.pom".format(artifact_id, version))
        deps, _, parent_info, _ = parse_pom(pom_path)

        if parent_info[0] and parent_info[1] and parent_info[2]:
            download_dep(parent_info[0], parent_info[1], parent_info[2], depth + 1)

        for dep_group, dep_artifact, dep_version, dep_type in deps:
            download_dep(dep_group, dep_artifact, dep_version, depth + 1)


AGP_POM = os.path.join(
    LOCAL_REPO,
    "com", "android", "tools", "build", "gradle", "8.2.0", "gradle-8.2.0.pom"
)

EXPLICIT_DEPS = [
    ("org.codehaus.mojo", "animal-sniffer-annotations", "1.19"),
    ("com.google.errorprone", "error_prone_annotations", "2.10.0"),
    ("com.google.guava", "guava", "31.1-jre"),
    ("com.google.guava", "guava-parent", "31.1-jre"),
    ("io.perfmark", "perfmark-api", "0.23.0"),
    ("io.netty", "netty-codec-http2", "4.1.72.Final"),
    ("io.netty", "netty-handler-proxy", "4.1.72.Final"),
    ("com.google.api.grpc", "proto-google-common-protos", "2.0.1"),
    ("io.grpc", "grpc-protobuf-lite", "1.45.1"),
]


def parse_pom_and_queue(pom_path, depth=0):
    if not os.path.exists(pom_path):
        return []

    deps, packaging_type, parent_info, props = parse_pom(pom_path)
    result = []

    if parent_info[0] and parent_info[1] and parent_info[2]:
        result.append((parent_info[0], parent_info[1], parent_info[2], depth))

    for dep_group, dep_artifact, dep_version, dep_type in deps:
        result.append((dep_group, dep_artifact, dep_version, depth))

    return result


def main():
    print("=" * 70)
    print("AGP 8.2.0 传递依赖递归下载工具")
    print("本地仓库路径: {}".format(LOCAL_REPO))
    print("最大递归深度: {}".format(MAX_DEPTH))
    print("仓库源列表:")
    for repo in REPO_URLS:
        print("  - {}".format(repo))
    print("=" * 70)

    if not os.path.exists(LOCAL_REPO):
        os.makedirs(LOCAL_REPO, exist_ok=True)

    start_time = time.time()

    print("\n[阶段1] 下载显式指定的缺失依赖")
    print("-" * 50)
    for i, (group_id, artifact_id, version) in enumerate(EXPLICIT_DEPS, 1):
        print("\n[{}/{}] {}:{}:{}".format(i, len(EXPLICIT_DEPS), group_id, artifact_id, version))
        sys.stdout.flush()
        download_dep(group_id, artifact_id, version, depth=0)

    print("\n\n[阶段2] 从 AGP 8.2.0 POM 递归解析并下载所有传递依赖")
    print("-" * 50)

    if os.path.exists(AGP_POM):
        print("找到 AGP POM: {}".format(AGP_POM))
        deps, _, parent_info, _ = parse_pom(AGP_POM)
        print("AGP 直接依赖数量: {}".format(len(deps)))

        for i, (dep_group, dep_artifact, dep_version, dep_type) in enumerate(deps, 1):
            print("\n[{}/{}] AGP直接依赖: {}:{}:{}".format(
                i, len(deps), dep_group, dep_artifact, dep_version))
            sys.stdout.flush()
            download_dep(dep_group, dep_artifact, dep_version, depth=0)
    else:
        print("[警告] 未找到 AGP POM 文件: {}".format(AGP_POM))
        print("将直接从 AGP 8.2.0 开始递归下载")
        download_dep("com.android.tools.build", "gradle", "8.2.0", depth=0)

    elapsed = time.time() - start_time

    print("\n" + "=" * 70)
    print("下载完成!")
    print("耗时: {:.1f} 秒".format(elapsed))
    print("本次新下载: {} 个文件".format(stats["downloaded"]))
    print("已存在跳过: {} 个文件".format(stats["existing"]))
    print("解析 POM: {} 个".format(stats["pom_parsed"]))
    print("访问唯一构件: {} 个".format(len(visited_poms)))
    if skipped_artifacts:
        print("下载失败: {} 个文件".format(stats["skipped"]))
        print("\n失败列表:")
        for s in sorted(skipped_artifacts):
            print("  - {}".format(s))
    print("本地仓库: {}".format(LOCAL_REPO))
    print("=" * 70)


if __name__ == "__main__":
    main()
