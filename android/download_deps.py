#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
预下载 Android 项目的所有 Gradle 依赖到本地 Maven 仓库
由于 Gradle HTTP 客户端无法连接外部 Maven 仓库，使用 Python 预下载
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
    "https://dl.google.com/dl/android/maven2",
    "https://maven.aliyun.com/repository/google",
    "https://maven.aliyun.com/repository/central",
    "https://repo1.maven.org/maven2",
]

TIMEOUT = 30
MAX_DEPTH = 3
MAX_RETRIES = 3
RETRY_DELAY = 2

downloaded = set()
skipped = set()
version_cache = {}
dep_queue = []

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
    if key in downloaded or key in skipped:
        return key in downloaded

    base_dir = maven_path(group_id, artifact_id, version)
    filename = "{}-{}.{}".format(artifact_id, version, ext)
    dest_path = os.path.join(base_dir, filename)

    if os.path.exists(dest_path) and os.path.getsize(dest_path) > 0:
        downloaded.add(key)
        return True

    for repo_url in REPO_URLS:
        url = maven_url(repo_url, group_id, artifact_id, version, ext)
        if download_file(url, dest_path):
            downloaded.add(key)
            return True

    skipped.add(key)
    return False


def resolve_version_range(group_id, artifact_id, version_spec):
    cache_key = "{}:{}:{}".format(group_id, artifact_id, version_spec)
    if cache_key in version_cache:
        return version_cache[cache_key]

    if not (version_spec.startswith("[") or version_spec.startswith("(")):
        version_cache[cache_key] = version_spec
        return version_spec

    match = re.match(r'^\[(.+)\]$', version_spec)
    if match:
        resolved = match.group(1)
        version_cache[cache_key] = resolved
        return resolved

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
                version_cache[cache_key] = release.text
                return release.text

            latest = root.find(".//{}latest".format(ns))
            if latest is not None and latest.text:
                version_cache[cache_key] = latest.text
                return latest.text

            versioning = root.find("{}versioning".format(ns))
            if versioning is not None:
                versions_el = versioning.find("{}versions".format(ns))
                if versions_el is not None:
                    versions = [v.text for v in versions_el.findall("{}version".format(ns))]
                    if versions:
                        version_cache[cache_key] = versions[-1]
                        return versions[-1]
        except (HTTPError, URLError, OSError, ET.ParseError):
            continue

    version_cache[cache_key] = version_spec
    return version_spec


def try_download_artifact(group_id, artifact_id, version, ext):
    result = try_download(group_id, artifact_id, version, ext)
    if result:
        return True

    if group_id.startswith("androidx.compose.") or group_id.startswith("androidx.navigation"):
        if not artifact_id.endswith("-android"):
            android_artifact = artifact_id + "-android"
            if ext == "jar":
                result = try_download(group_id, android_artifact, version, "aar")
                if result:
                    return True
            result = try_download(group_id, android_artifact, version, ext)
            if result:
                return True

    return False


def parse_pom(pom_path):
    deps = []
    packaging_type = "jar"
    parent_info = (None, None, None)
    props = {}

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

        packaging = root.find("{}packaging".format(ns))
        if packaging is not None:
            packaging_type = packaging.text

        deps_section = root.find("{}dependencies".format(ns))
        if deps_section is None:
            return deps, packaging_type, parent_info, props, effective_group, effective_version

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

            if dep_version and dep_version.startswith("$"):
                prop_name = dep_version[2:-1] if dep_version.endswith("}") else dep_version[1:]
                if prop_name in props:
                    dep_version = props[prop_name]
                elif dep_version == "${project.version}":
                    dep_version = effective_version
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

    return deps, packaging_type, parent_info, props, None, None


def download_dep(group_id, artifact_id, version, types, depth=0):
    key = "{}:{}:{}".format(group_id, artifact_id, version)
    if key in downloaded:
        return

    prefix = "  " * depth
    print("{}[下载] {}:{}:{}".format(prefix, group_id, artifact_id, version))

    pom_ok = try_download(group_id, artifact_id, version, "pom")

    packaging_type = "jar"
    if pom_ok:
        base_dir = maven_path(group_id, artifact_id, version)
        pom_path = os.path.join(base_dir, "{}-{}.pom".format(artifact_id, version))
        _, packaging_type, _, _, _, _ = parse_pom(pom_path)

    if packaging_type == "pom":
        downloaded.add(key)
        if pom_ok and depth < MAX_DEPTH:
            base_dir = maven_path(group_id, artifact_id, version)
            pom_path = os.path.join(base_dir, "{}-{}.pom".format(artifact_id, version))
            deps, _, parent_info, _, eff_group, eff_version = parse_pom(pom_path)

            if parent_info[0] and parent_info[1] and parent_info[2]:
                pg, pa, pv = parent_info
                parent_key = "{}:{}:{}".format(pg, pa, pv)
                if parent_key not in downloaded:
                    download_dep(pg, pa, pv, ["pom"], depth + 1)

            for dep_group, dep_artifact, dep_version, dep_type in deps:
                dep_types = ["pom", "aar"] if dep_type == "aar" else ["pom", "jar"]
                download_dep(dep_group, dep_artifact, dep_version, dep_types, depth + 1)
        return

    for t in types:
        if t == "pom":
            continue
        result = try_download_artifact(group_id, artifact_id, version, t)
        if not result:
            if packaging_type == "aar" and t == "jar":
                result = try_download_artifact(group_id, artifact_id, version, "aar")
                if result:
                    continue
            print("{}  [警告] 下载失败: {}:{}:{} .{}".format(prefix, group_id, artifact_id, version, t))

    downloaded.add(key)

    if pom_ok and depth < MAX_DEPTH:
        base_dir = maven_path(group_id, artifact_id, version)
        pom_path = os.path.join(base_dir, "{}-{}.pom".format(artifact_id, version))
        deps, _, parent_info, _, eff_group, eff_version = parse_pom(pom_path)

        if parent_info[0] and parent_info[1] and parent_info[2]:
            pg, pa, pv = parent_info
            parent_key = "{}:{}:{}".format(pg, pa, pv)
            if parent_key not in downloaded:
                download_dep(pg, pa, pv, ["pom"], depth + 1)

        for dep_group, dep_artifact, dep_version, dep_type in deps:
            dep_types = ["pom", "aar"] if dep_type == "aar" else ["pom", "jar"]
            download_dep(dep_group, dep_artifact, dep_version, dep_types, depth + 1)


INITIAL_DEPS = [
    ("com.android.application", "com.android.application.gradle.plugin", "8.2.0", ["pom"]),
    ("com.android.tools.build", "gradle", "8.2.0", ["pom", "jar"]),
    ("com.android.tools.build", "builder", "8.2.0", ["pom", "jar"]),
    ("com.android.tools.build", "builder-model", "8.2.0", ["pom", "jar"]),
    ("com.android.tools.build", "gradle-api", "8.2.0", ["pom", "jar"]),
    ("com.android.tools.build", "gradle-settings-api", "8.2.0", ["pom", "jar"]),
    ("com.android.tools.build", "aaptcompiler", "8.2.0", ["pom", "jar"]),
    ("com.android.tools.build", "apksig", "8.2.0", ["pom", "jar"]),
    ("com.android.tools.build", "apkzlib", "8.2.0", ["pom", "jar"]),
    ("com.android.tools.build", "aapt2", "8.2.0-10154469", ["pom"]),
    ("com.android.tools.build", "aapt2-proto", "8.2.0-10154469", ["pom", "jar"]),
    ("com.android.tools", "common", "31.2.0", ["pom", "jar"]),
    ("com.android.tools", "annotations", "31.2.0", ["pom", "jar"]),
    ("com.android.tools", "sdk-common", "31.2.0", ["pom", "jar"]),
    ("com.android.tools", "sdklib", "31.2.0", ["pom", "jar"]),
    ("com.android.tools", "repository", "31.2.0", ["pom", "jar"]),
    ("com.android.tools.ddms", "ddmlib", "31.2.0", ["pom", "jar"]),
    ("com.android.tools.analytics-library", "shared", "31.2.0", ["pom", "jar"]),
    ("com.android.tools.analytics-library", "protos", "31.2.0", ["pom", "jar"]),
    ("com.android.tools.analytics-library", "tracker", "31.2.0", ["pom", "jar"]),
    ("com.android.tools.analytics-library", "crash", "31.2.0", ["pom", "jar"]),
    ("com.android.tools.lint", "lint", "31.2.0", ["pom", "jar"]),
    ("com.android.tools.lint", "lint-api", "31.2.0", ["pom", "jar"]),
    ("com.android.tools.lint", "lint-checks", "31.2.0", ["pom", "jar"]),
    ("com.android.tools.lint", "lint-model", "31.2.0", ["pom", "jar"]),
    ("com.android.databinding", "baseLibrary", "8.2.0", ["pom", "jar"]),
    ("com.android", "signflinger", "8.2.0", ["pom", "jar"]),
    ("com.android", "zipflinger", "8.2.0", ["pom", "jar"]),

    ("org.jetbrains.kotlin", "kotlin-gradle-plugin", "1.9.20", ["pom", "jar"]),
    ("org.jetbrains.kotlin", "kotlin-gradle-plugin-api", "1.9.20", ["pom", "jar"]),
    ("org.jetbrains.kotlin", "kotlin-stdlib", "1.9.20", ["pom", "jar"]),
    ("org.jetbrains.kotlin", "kotlin-stdlib-common", "1.9.20", ["pom", "jar"]),
    ("org.jetbrains.kotlin", "kotlin-stdlib-jdk7", "1.9.20", ["pom", "jar"]),
    ("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", "1.9.20", ["pom", "jar"]),
    ("org.jetbrains.kotlin", "kotlin-reflect", "1.9.20", ["pom", "jar"]),
    ("org.jetbrains.kotlin", "kotlin-compiler-embeddable", "1.9.20", ["pom", "jar"]),
    ("org.jetbrains.kotlin", "kotlin-daemon-embeddable", "1.9.20", ["pom", "jar"]),
    ("org.jetbrains.kotlin", "kotlin-scripting-compiler-embeddable", "1.9.20", ["pom", "jar"]),
    ("org.jetbrains.kotlin", "kotlin-scripting-compiler-impl-embeddable", "1.9.20", ["pom", "jar"]),
    ("org.jetbrains.kotlin", "kotlin-sam-with-receiver-compiler-plugin", "1.9.20", ["pom", "jar"]),
    ("org.jetbrains.kotlin.android", "org.jetbrains.kotlin.android.gradle.plugin", "1.9.20", ["pom"]),

    ("androidx.core", "core-ktx", "1.12.0", ["pom", "aar"]),
    ("androidx.lifecycle", "lifecycle-runtime-ktx", "2.7.0", ["pom", "aar"]),
    ("androidx.lifecycle", "lifecycle-viewmodel-compose", "2.7.0", ["pom", "aar"]),
    ("androidx.activity", "activity-compose", "1.8.2", ["pom", "aar"]),
    ("androidx.compose.ui", "ui", "1.5.4", ["pom", "aar"]),
    ("androidx.compose.ui", "ui-graphics", "1.5.4", ["pom", "aar"]),
    ("androidx.compose.ui", "ui-tooling", "1.5.4", ["pom", "aar"]),
    ("androidx.compose.ui", "ui-tooling-preview", "1.5.4", ["pom", "aar"]),
    ("androidx.compose.material3", "material3", "1.1.2", ["pom", "aar"]),
    ("androidx.compose.material", "material-icons-extended", "1.5.4", ["pom", "aar"]),
    ("androidx.navigation", "navigation-compose", "2.7.7", ["pom", "aar"]),
    ("androidx.datastore", "datastore-preferences", "1.0.0", ["pom", "aar"]),
    ("com.squareup.okhttp3", "okhttp", "4.12.0", ["pom", "jar"]),
    ("com.squareup.okhttp3", "logging-interceptor", "4.12.0", ["pom", "jar"]),
    ("com.squareup.retrofit2", "retrofit", "2.9.0", ["pom", "jar"]),
    ("com.squareup.retrofit2", "converter-gson", "2.9.0", ["pom", "jar"]),
    ("com.google.code.gson", "gson", "2.10.1", ["pom", "jar"]),
]


def main():
    print("=" * 60)
    print("Gradle 依赖预下载工具")
    print("本地仓库路径: {}".format(LOCAL_REPO))
    print("=" * 60)

    if not os.path.exists(LOCAL_REPO):
        os.makedirs(LOCAL_REPO, exist_ok=True)

    total = len(INITIAL_DEPS)
    start_time = time.time()

    for i, (group_id, artifact_id, version, types) in enumerate(INITIAL_DEPS, 1):
        print("\n[{}/{}] 处理: {}:{}:{}".format(i, total, group_id, artifact_id, version))
        download_dep(group_id, artifact_id, version, types, depth=0)

    elapsed = time.time() - start_time

    print("\n" + "=" * 60)
    print("下载完成!")
    print("耗时: {:.1f} 秒".format(elapsed))
    print("成功下载: {} 个构件".format(len(downloaded)))
    print("文件总数: {} 个".format(len([k for k in downloaded if True])))
    if skipped:
        print("跳过/失败: {} 个文件".format(len(skipped)))
        for s in sorted(skipped):
            print("  - {}".format(s))
    print("本地仓库: {}".format(LOCAL_REPO))
    print("=" * 60)


if __name__ == "__main__":
    main()
