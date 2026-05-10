pluginManagement {
    repositories {
        maven {
            url = uri("${rootProject.projectDir}/local-maven-repo")
        }
        maven {
            url = uri("/root/.gradle/caches/modules-2/files-2.1")
        }
        flatDir {
            dirs("${rootProject.projectDir}/local-maven-repo")
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri("${rootProject.projectDir}/local-maven-repo")
        }
    }
}
rootProject.name = "MiniMind"
include(":app")
