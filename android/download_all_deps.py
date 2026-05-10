import xml.etree.ElementTree as ET
import urllib.request
import os
import sys
import time

LOCAL_REPO = "/workspace/android/local-maven-repo"
GOOGLE_MAVEN = "https://dl.google.com/dl/android/maven2"
MAVEN_CENTRAL = "https://repo1.maven.org/maven2"

def parse_pom_deps(pom_path):
    deps = []
    try:
        tree = ET.parse(pom_path)
        root = tree.getroot()
        ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
        for dep in root.findall('.//m:dependency', ns):
            group = dep.find('m:groupId', ns)
            artifact = dep.find('m:artifactId', ns)
            version_el = dep.find('m:version', ns)
            scope = dep.find('m:scope', ns)
            optional = dep.find('m:optional', ns)
            if group is not None and artifact is not None and version_el is not None:
                scope_val = scope.text if scope is not None else "compile"
                opt_val = optional.text if optional is not None else "false"
                if scope_val in ("compile", "runtime") and opt_val == "false":
                    version = version_el.text
                    # Handle property references like ${project.version}
                    if version.startswith("$"):
                        parent_version = root.find('.//m:parent/m:version', ns)
                        if parent_version is not None:
                            version = parent_version.text
                        else:
                            continue
                    deps.append(f"{group.text}:{artifact.text}:{version}")
    except Exception as e:
        pass
    return deps

def download_file(url, filename, timeout=30):
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            with open(filename, 'wb') as f:
                f.write(resp.read())
        return os.path.getsize(filename) > 0
    except:
        if os.path.exists(filename):
            os.remove(filename)
        return False

def download_artifact(artifact_str):
    parts = artifact_str.split(":")
    group_id, artifact_id, version = parts[0], parts[1], parts[2]
    group_path = group_id.replace(".", "/")
    artifact_dir = os.path.join(LOCAL_REPO, group_path, artifact_id, version)
    os.makedirs(artifact_dir, exist_ok=True)
    
    aar_path = os.path.join(artifact_dir, f"{artifact_id}-{version}.aar")
    jar_path = os.path.join(artifact_dir, f"{artifact_id}-{version}.jar")
    pom_path = os.path.join(artifact_dir, f"{artifact_id}-{version}.pom")
    
    has_binary = (os.path.exists(aar_path) and os.path.getsize(aar_path) > 0) or \
                 (os.path.exists(jar_path) and os.path.getsize(jar_path) > 0)
    has_pom = os.path.exists(pom_path) and os.path.getsize(pom_path) > 0
    
    if has_binary and has_pom:
        return True
    
    for base_url in [GOOGLE_MAVEN, MAVEN_CENTRAL]:
        base_url_str = f"{base_url}/{group_path}/{artifact_id}/{version}/{artifact_id}-{version}"
        
        if not has_pom:
            if download_file(base_url_str + ".pom", pom_path):
                has_pom = True
        
        if not has_binary:
            if download_file(base_url_str + ".aar", aar_path):
                has_binary = True
            elif download_file(base_url_str + ".jar", jar_path):
                has_binary = True
        
        if has_binary and has_pom:
            return True
    
    # Create empty jar if nothing found (for parent POMs etc)
    if has_pom and not has_binary:
        with open(jar_path, 'wb') as f:
            f.write(b'')
        return True
    
    return has_pom or has_binary

# Direct dependencies from build.gradle.kts
direct_deps = [
    "androidx.core:core-ktx:1.12.0",
    "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0",
    "androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0",
    "androidx.activity:activity-compose:1.8.2",
    "androidx.compose.ui:ui-android:1.5.4",
    "androidx.compose.ui:ui-graphics-android:1.5.4",
    "androidx.compose.ui:ui-tooling-preview-android:1.5.4",
    "androidx.compose.material3:material3-android:1.2.0",
    "androidx.compose.material:material-icons-extended-android:1.5.4",
    "androidx.compose.runtime:runtime-android:1.5.4",
    "androidx.compose.foundation:foundation-android:1.5.4",
    "androidx.compose.material:material-android:1.5.4",
    "androidx.navigation:navigation-compose:2.7.7",
    "com.squareup.okhttp3:okhttp:4.12.0",
    "com.squareup.retrofit2:retrofit:2.9.0",
    "com.squareup.retrofit2:converter-gson:2.9.0",
    "com.google.code.gson:gson:2.10.1",
    "androidx.datastore:datastore-preferences:1.0.0",
]

queue = list(direct_deps)
processed = set()
failed = []
count = 0

while queue:
    artifact = queue.pop(0)
    if artifact in processed:
        continue
    processed.add(artifact)
    count += 1
    
    parts = artifact.split(":")
    group_id, artifact_id, version = parts[0], parts[1], parts[2]
    group_path = group_id.replace(".", "/")
    
    print(f"[{count}] {artifact}", end=" ", flush=True)
    
    ok = download_artifact(artifact)
    
    if ok:
        # Parse POM for transitive deps
        pom_path = os.path.join(LOCAL_REPO, group_path, artifact_id, version, f"{artifact_id}-{version}.pom")
        if os.path.exists(pom_path):
            deps = parse_pom_deps(pom_path)
            for dep in deps:
                if dep not in processed:
                    queue.append(dep)
        print("OK")
    else:
        print("FAILED")
        failed.append(artifact)

print(f"\nProcessed: {count}, Failed: {len(failed)}")
if failed:
    print("Failed artifacts:")
    for f in failed:
        print(f"  {f}")
