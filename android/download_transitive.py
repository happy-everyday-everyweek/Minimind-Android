import xml.etree.ElementTree as ET
import urllib.request
import os

LOCAL_REPO = "/workspace/android/local-maven-repo"
GOOGLE_MAVEN = "https://dl.google.com/dl/android/maven2"
MAVEN_CENTRAL = "https://repo1.maven.org/maven2"

def parse_pom_deps(pom_path):
    """Parse a POM file and extract dependencies."""
    deps = []
    try:
        tree = ET.parse(pom_path)
        root = tree.getroot()
        ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
        for dep in root.findall('.//m:dependency', ns):
            group = dep.find('m:groupId', ns)
            artifact = dep.find('m:artifactId', ns)
            version = dep.find('m:version', ns)
            scope = dep.find('m:scope', ns)
            if group is not None and artifact is not None and version is not None:
                scope_val = scope.text if scope is not None else "compile"
                if scope_val in ("compile", "runtime"):
                    deps.append(f"{group.text}:{artifact.text}:{version.text}")
    except Exception as e:
        print(f"  Error parsing {pom_path}: {e}")
    return deps

def download_artifact(artifact, base_url):
    parts = artifact.split(":")
    group_id, artifact_id, version = parts[0], parts[1], parts[2]
    group_path = group_id.replace(".", "/")
    artifact_dir = os.path.join(LOCAL_REPO, group_path, artifact_id, version)
    os.makedirs(artifact_dir, exist_ok=True)
    
    base_artifact_url = f"{base_url}/{group_path}/{artifact_id}/{version}/{artifact_id}-{version}"
    
    downloaded_any = False
    for ext in [".pom", ".aar", ".jar"]:
        url = base_artifact_url + ext
        filename = os.path.join(artifact_dir, artifact_id + "-" + version + ext)
        if os.path.exists(filename) and os.path.getsize(filename) > 0:
            downloaded_any = True
            continue
        try:
            urllib.request.urlretrieve(url, filename)
            size = os.path.getsize(filename)
            if size > 0:
                downloaded_any = True
            else:
                os.remove(filename)
        except:
            if os.path.exists(filename):
                os.remove(filename)
    return downloaded_any

# Start with the 3 main artifacts we just downloaded
initial_artifacts = [
    "androidx.compose.material3:material3-android:1.2.0",
    "androidx.compose.foundation:foundation-android:1.5.4",
    "androidx.compose.material:material-android:1.5.4",
]

all_deps = set()
queue = list(initial_artifacts)
processed = set()

while queue:
    artifact = queue.pop(0)
    if artifact in processed:
        continue
    processed.add(artifact)
    
    parts = artifact.split(":")
    group_id, artifact_id, version = parts[0], parts[1], parts[2]
    group_path = group_id.replace(".", "/")
    
    pom_path = os.path.join(LOCAL_REPO, group_path, artifact_id, version, f"{artifact_id}-{version}.pom")
    
    if not os.path.exists(pom_path):
        # Try downloading the POM first
        print(f"Downloading POM for {artifact}")
        for base_url in [GOOGLE_MAVEN, MAVEN_CENTRAL]:
            if download_artifact(artifact, base_url):
                break
    
    if os.path.exists(pom_path):
        deps = parse_pom_deps(pom_path)
        for dep in deps:
            if dep not in processed:
                queue.append(dep)
                all_deps.add(dep)

print(f"\nFound {len(all_deps)} transitive dependencies to download")

for dep in sorted(all_deps):
    parts = dep.split(":")
    group_id, artifact_id, version = parts[0], parts[1], parts[2]
    group_path = group_id.replace(".", "/")
    artifact_dir = os.path.join(LOCAL_REPO, group_path, artifact_id, version)
    
    # Check if we already have the AAR or JAR
    aar_path = os.path.join(artifact_dir, f"{artifact_id}-{version}.aar")
    jar_path = os.path.join(artifact_dir, f"{artifact_id}-{version}.jar")
    pom_path = os.path.join(artifact_dir, f"{artifact_id}-{version}.pom")
    
    has_artifact = (os.path.exists(aar_path) and os.path.getsize(aar_path) > 0) or \
                   (os.path.exists(jar_path) and os.path.getsize(jar_path) > 0)
    has_pom = os.path.exists(pom_path) and os.path.getsize(pom_path) > 0
    
    if has_artifact and has_pom:
        continue
    
    print(f"Downloading: {dep}")
    found = download_artifact(dep, GOOGLE_MAVEN)
    if not found:
        found = download_artifact(dep, MAVEN_CENTRAL)
    if not found:
        print(f"  FAILED: {dep}")

print("\nDone!")
