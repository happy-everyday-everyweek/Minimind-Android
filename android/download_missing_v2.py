import urllib.request
import os
import sys

LOCAL_REPO = "/workspace/android/local-maven-repo"
GOOGLE_MAVEN = "https://dl.google.com/dl/android/maven2"
MAVEN_CENTRAL = "https://repo1.maven.org/maven2"

artifacts = [
    # Missing from build error
    "androidx.compose.material3:material3-android:1.2.0",
    "androidx.compose.foundation:foundation-android:1.5.4",
    "androidx.compose.material:material-android:1.5.4",
    # Also download their likely transitive deps
    "androidx.compose.foundation:foundation-layout-android:1.5.4",
    "androidx.compose.material3:material3-android:1.1.2",
    "androidx.compose.material3:material3-common:1.2.0",
    # Try different versions that might exist
    "androidx.compose.material3:material3:1.2.0",
    "androidx.compose.foundation:foundation:1.5.4",
    "androidx.compose.material:material:1.5.4",
]

def download_artifact(artifact, base_url):
    parts = artifact.split(":")
    group_id, artifact_id, version = parts[0], parts[1], parts[2]
    group_path = group_id.replace(".", "/")
    artifact_dir = os.path.join(LOCAL_REPO, group_path, artifact_id, version)
    os.makedirs(artifact_dir, exist_ok=True)
    
    base_artifact_url = f"{base_url}/{group_path}/{artifact_id}/{version}/{artifact_id}-{version}"
    
    downloaded = False
    for ext in [".pom", ".aar", ".jar", "-sources.jar"]:
        url = base_artifact_url + ext
        filename = os.path.join(artifact_dir, artifact_id + "-" + version + ext)
        if os.path.exists(filename) and os.path.getsize(filename) > 0:
            print(f"  SKIP (exists): {artifact_id}-{version}{ext}")
            downloaded = True
            continue
        try:
            print(f"  Downloading: {url}")
            urllib.request.urlretrieve(url, filename)
            size = os.path.getsize(filename)
            if size > 0:
                print(f"  OK: {size} bytes")
                downloaded = True
            else:
                os.remove(filename)
                print(f"  EMPTY, removed")
        except Exception as e:
            if os.path.exists(filename):
                os.remove(filename)
            print(f"  NOT FOUND: {ext}")
    
    return downloaded

for artifact in artifacts:
    print(f"\n=== {artifact} ===")
    found = download_artifact(artifact, GOOGLE_MAVEN)
    if not found:
        print("  Trying Maven Central...")
        download_artifact(artifact, MAVEN_CENTRAL)

print("\nDone!")
