#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Round 3: 补充下载所有缺失的 Maven 传递依赖到本地仓库
- 从显式缺失列表 + 已有 POM 递归解析 + AGP 8.2.0 完整依赖树
- 递归解析 POM 中的依赖，深度限制 10
- 跳过 optional 和 provided/test/system 作用域
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


def resolve_property(value, props):
    if not value or not value.startswith("$"):
        return value
    prop_name = value[2:-1] if value.endswith("}") else value[1:]
    if prop_name in props:
        return props[prop_name]
    return None


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

            dep_group = resolve_property(g.text, props)
            dep_artifact = resolve_property(a.text, props)
            dep_version = resolve_property(v.text, props) if v is not None else None

            if dep_group is None or dep_artifact is None:
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

    prefix = "  " * min(depth, 8)
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


def scan_existing_poms_for_missing():
    missing = []
    for dirpath, dirnames, filenames in os.walk(LOCAL_REPO):
        for fname in filenames:
            if not fname.endswith(".pom"):
                continue
            pom_path = os.path.join(dirpath, fname)
            deps, _, parent_info, _ = parse_pom(pom_path)
            for dep_group, dep_artifact, dep_version, dep_type in deps:
                dep_key = "{}:{}:{}".format(dep_group, dep_artifact, dep_version)
                if dep_key in visited_poms:
                    continue
                group_path = dep_group.replace(".", os.sep)
                dep_dir = os.path.join(LOCAL_REPO, group_path, dep_artifact, dep_version)
                if not os.path.exists(dep_dir):
                    missing.append((dep_group, dep_artifact, dep_version))
            if parent_info[0] and parent_info[1] and parent_info[2]:
                pg, pa, pv = parent_info
                parent_key = "{}:{}:{}".format(pg, pa, pv)
                if parent_key not in visited_poms:
                    group_path = pg.replace(".", os.sep)
                    parent_dir = os.path.join(LOCAL_REPO, group_path, pa, pv)
                    if not os.path.exists(parent_dir):
                        missing.append((pg, pa, pv))
    return missing


EXPLICIT_MISSING = [
    ("com.google.errorprone", "error_prone_annotations", "2.11.0"),
    ("org.checkerframework", "checker-qual", "3.12.0"),
    ("commons-logging", "commons-logging", "1.2"),
    ("commons-codec", "commons-codec", "1.11"),
    ("com.google.protobuf", "protobuf-bom", "3.19.3"),
    ("com.google.protobuf", "protobuf-parent", "3.19.3"),
    ("io.netty", "netty-tcnative-classes", "2.0.46.Final"),
]

AGP_CORE_DEPS = [
    ("com.android.tools.build", "gradle", "8.2.0"),
    ("com.android.tools.build", "gradle-api", "8.2.0"),
    ("com.android.tools.build", "gradle-settings-api", "8.2.0"),
    ("com.android.tools.build", "builder", "8.2.0"),
    ("com.android.tools.build", "builder-model", "8.2.0"),
    ("com.android.tools.build", "aaptcompiler", "8.2.0"),
    ("com.android.tools.build", "apksig", "8.2.0"),
    ("com.android.tools.build", "apkzlib", "8.2.0"),
    ("com.android.tools", "common", "31.2.0"),
    ("com.android.tools", "annotations", "31.2.0"),
    ("com.android.tools", "sdk-common", "31.2.0"),
    ("com.android.tools", "sdklib", "31.2.0"),
    ("com.android.tools", "repository", "31.2.0"),
    ("com.android.tools.ddms", "ddmlib", "31.2.0"),
    ("com.android.tools.analytics-library", "shared", "31.2.0"),
    ("com.android.tools.analytics-library", "protos", "31.2.0"),
    ("com.android.tools.analytics-library", "tracker", "31.2.0"),
    ("com.android.tools.analytics-library", "crash", "31.2.0"),
    ("com.android.tools.lint", "lint", "31.2.0"),
    ("com.android.tools.lint", "lint-api", "31.2.0"),
    ("com.android.tools.lint", "lint-checks", "31.2.0"),
    ("com.android.tools.lint", "lint-model", "31.2.0"),
    ("com.android.databinding", "baseLibrary", "8.2.0"),
    ("com.android", "signflinger", "8.2.0"),
    ("com.android", "zipflinger", "8.2.0"),
]

ADDITIONAL_KNOWN_DEPS = [
    ("com.google.guava", "guava", "31.1-jre"),
    ("com.google.guava", "guava-parent", "31.1-jre"),
    ("com.google.errorprone", "error_prone_annotations", "2.10.0"),
    ("org.codehaus.mojo", "animal-sniffer-annotations", "1.19"),
    ("com.google.j2objc", "j2objc-annotations", "1.3"),
    ("com.google.code.findbugs", "jsr305", "3.0.2"),
    ("com.google.protobuf", "protobuf-java", "3.19.3"),
    ("com.google.protobuf", "protobuf-java-util", "3.19.3"),
    ("com.google.protobuf", "protobuf-parent", "3.19.3"),
    ("com.google.protobuf", "protobuf-bom", "3.19.3"),
    ("com.squareup.okhttp3", "okhttp", "4.12.0"),
    ("com.squareup.okhttp3", "logging-interceptor", "4.12.0"),
    ("com.squareup.okio", "okio", "3.6.0"),
    ("com.squareup.okio", "okio-parent", "3.6.0"),
    ("org.jetbrains.kotlin", "kotlin-stdlib", "1.9.20"),
    ("org.jetbrains.kotlin", "kotlin-stdlib-common", "1.9.20"),
    ("org.jetbrains.kotlin", "kotlin-stdlib-jdk7", "1.9.20"),
    ("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", "1.9.20"),
    ("org.jetbrains.kotlin", "kotlin-reflect", "1.9.20"),
    ("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.7.3"),
    ("org.jetbrains.kotlinx", "kotlinx-coroutines-android", "1.7.3"),
    ("org.jetbrains.kotlinx", "kotlinx-coroutines-bom", "1.7.3"),
    ("org.jetbrains", "annotations", "23.0.0"),
    ("com.google.code.gson", "gson", "2.10.1"),
    ("com.google.code.gson", "gson-parent", "2.10.1"),
    ("com.squareup.retrofit2", "retrofit", "2.9.0"),
    ("com.squareup.retrofit2", "converter-gson", "2.9.0"),
    ("com.squareup.retrofit2", "retrofit-parent", "2.9.0"),
    ("com.squareup.javapoet", "javapoet", "1.10.0"),
    ("io.grpc", "grpc-api", "1.45.1"),
    ("io.grpc", "grpc-context", "1.45.1"),
    ("io.grpc", "grpc-core", "1.45.1"),
    ("io.grpc", "grpc-netty", "1.45.1"),
    ("io.grpc", "grpc-protobuf", "1.45.1"),
    ("io.grpc", "grpc-protobuf-lite", "1.45.1"),
    ("io.grpc", "grpc-stub", "1.45.1"),
    ("io.grpc", "grpc-bom", "1.45.1"),
    ("io.grpc", "grpc-core", "1.45.1"),
    ("io.netty", "netty-common", "4.1.72.Final"),
    ("io.netty", "netty-buffer", "4.1.72.Final"),
    ("io.netty", "netty-codec", "4.1.72.Final"),
    ("io.netty", "netty-codec-http", "4.1.72.Final"),
    ("io.netty", "netty-codec-http2", "4.1.72.Final"),
    ("io.netty", "netty-codec-socks", "4.1.72.Final"),
    ("io.netty", "netty-handler", "4.1.72.Final"),
    ("io.netty", "netty-handler-proxy", "4.1.72.Final"),
    ("io.netty", "netty-resolver", "4.1.72.Final"),
    ("io.netty", "netty-transport", "4.1.72.Final"),
    ("io.netty", "netty-parent", "4.1.72.Final"),
    ("io.netty", "netty-tcnative-classes", "2.0.46.Final"),
    ("io.perfmark", "perfmark-api", "0.23.0"),
    ("javax.annotation", "javax.annotation-api", "1.3.2"),
    ("javax.inject", "javax.inject", "1"),
    ("com.google.api.grpc", "proto-google-common-protos", "2.0.1"),
    ("org.apache.httpcomponents", "httpclient", "4.5.6"),
    ("org.apache.httpcomponents", "httpcore", "4.4.13"),
    ("org.apache.httpcomponents", "httpcomponents-client", "4.5.6"),
    ("org.apache.httpcomponents", "httpcomponents-core", "4.4.13"),
    ("commons-logging", "commons-logging", "1.2"),
    ("commons-codec", "commons-codec", "1.11"),
    ("commons-codec", "commons-codec", "1.10"),
    ("commons-io", "commons-io", "2.4"),
    ("org.checkerframework", "checker-qual", "3.12.0"),
    ("com.google.errorprone", "error_prone_annotations", "2.11.0"),
    ("com.google.errorprone", "error_prone_annotations", "2.10.0"),
    ("com.google.errorprone", "error_prone_parent", "2.11.0"),
    ("com.google.errorprone", "error_prone_parent", "2.10.0"),
    ("org.ow2.asm", "asm", "9.5"),
    ("org.ow2.asm", "asm-commons", "9.5"),
    ("org.ow2.asm", "asm-tree", "9.5"),
    ("org.ow2.asm", "asm-util", "9.5"),
    ("org.ow2.asm", "asm-analysis", "9.5"),
    ("org.intellij.lang", "annotations", "12.0"),
    ("net.sf.kxml", "kxml2", "2.3.0"),
    ("com.android.tools", "dvlib", "31.2.0"),
    ("com.android.tools", "layoutlib-api", "31.2.0"),
    ("com.android.tools", "patcher", "31.2.0"),
    ("com.android.tools", "bintray", "31.2.0"),
    ("com.android.tools.jack", "jack-api", "31.2.0"),
    ("com.android.tools.jill", "jill-api", "31.2.0"),
    ("com.android.tools.build", "manifest-merger", "31.2.0"),
    ("com.android.tools.build", "aapt2-proto", "8.2.0-10154469"),
    ("com.android.tools.build", "aapt2", "8.2.0-10154469"),
    ("com.android.tools.build", "gradle-settings-api", "8.2.0"),
    ("com.android.tools.build", "transform-api", "2.0.0"),
    ("com.android.tools.build", "builder-test-api", "8.2.0"),
    ("com.google.flatbuffers", "flatbuffers-java", "2.0.3"),
    ("com.sun.activation", "jakarta.activation", "1.2.2"),
    ("jakarta.activation", "jakarta.activation-api", "1.2.2"),
    ("jakarta.xml.bind", "jakarta.xml.bind-api", "2.3.3"),
    ("com.sun.xml.bind", "jaxb-core", "2.3.0.1"),
    ("com.sun.xml.bind", "jaxb-impl", "2.3.0.1"),
    ("org.glassfish.jaxb", "jaxb-core", "2.3.0.1"),
    ("org.glassfish.jaxb", "jaxb-runtime", "2.3.0.1"),
    ("org.glassfish.jaxb", "txw2", "2.3.0.1"),
    ("org.glassfish.jaxb", "jaxb-parent", "2.3.0.1"),
    ("com.squareup", "javawriter", "2.5.0"),
    ("org.antlr", "antlr4", "4.5.3"),
    ("org.antlr", "antlr4-runtime", "4.5.3"),
    ("com.android.tools.lint", "lint-gradle", "31.2.0"),
    ("com.android.tools.lint", "lint-gradle-api", "31.2.0"),
    ("com.android.tools.build", "data-binding-compiler-common", "8.2.0"),
    ("androidx.databinding", "databinding-common", "8.2.0"),
]


def main():
    print("=" * 70)
    print("Round 3: Maven 传递依赖补充下载工具")
    print("本地仓库路径: {}".format(LOCAL_REPO))
    print("最大递归深度: {}".format(MAX_DEPTH))
    print("仓库源列表:")
    for repo in REPO_URLS:
        print("  - {}".format(repo))
    print("=" * 70)

    if not os.path.exists(LOCAL_REPO):
        os.makedirs(LOCAL_REPO, exist_ok=True)

    start_time = time.time()

    print("\n[阶段1] 下载显式指定的 7 个缺失依赖及其传递依赖")
    print("-" * 50)
    for i, (group_id, artifact_id, version) in enumerate(EXPLICIT_MISSING, 1):
        print("\n[{}/{}] {}:{}:{}".format(i, len(EXPLICIT_MISSING), group_id, artifact_id, version))
        sys.stdout.flush()
        download_dep(group_id, artifact_id, version, depth=0)

    print("\n\n[阶段2] 从 AGP 8.2.0 核心依赖递归解析并下载")
    print("-" * 50)
    for i, (group_id, artifact_id, version) in enumerate(AGP_CORE_DEPS, 1):
        print("\n[{}/{}] AGP核心: {}:{}:{}".format(i, len(AGP_CORE_DEPS), group_id, artifact_id, version))
        sys.stdout.flush()
        download_dep(group_id, artifact_id, version, depth=0)

    print("\n\n[阶段3] 下载其他已知依赖及其传递依赖")
    print("-" * 50)
    for i, (group_id, artifact_id, version) in enumerate(ADDITIONAL_KNOWN_DEPS, 1):
        print("\n[{}/{}] 已知依赖: {}:{}:{}".format(i, len(ADDITIONAL_KNOWN_DEPS), group_id, artifact_id, version))
        sys.stdout.flush()
        download_dep(group_id, artifact_id, version, depth=0)

    print("\n\n[阶段4] 扫描已有 POM 文件，发现并下载缺失的传递依赖")
    print("-" * 50)
    missing_from_poms = scan_existing_poms_for_missing()
    if missing_from_poms:
        deduped = list(dict.fromkeys(missing_from_poms))
        print("从已有 POM 中发现 {} 个缺失依赖".format(len(deduped)))
        for i, (group_id, artifact_id, version) in enumerate(deduped, 1):
            print("\n[{}/{}] POM缺失: {}:{}:{}".format(i, len(deduped), group_id, artifact_id, version))
            sys.stdout.flush()
            download_dep(group_id, artifact_id, version, depth=0)
    else:
        print("未发现额外的缺失依赖")

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
