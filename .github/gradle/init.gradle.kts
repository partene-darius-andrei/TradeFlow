// Replace Maven Central with mirrors for CI builds
settingsEvaluated {
    pluginManagement {
        repositories {
            removeIf { it is MavenArtifactRepository && it.url.toString().contains("repo.maven.apache.org") }
            google()
            maven { url = uri("https://repo1.maven.org/maven2/") }
            maven { url = uri("https://maven-central.storage.googleapis.com") }
            gradlePluginPortal()
        }
    }
    dependencyResolutionManagement {
        repositories {
            removeIf { it is MavenArtifactRepository && it.url.toString().contains("repo.maven.apache.org") }
            google()
            maven { url = uri("https://repo1.maven.org/maven2/") }
            maven { url = uri("https://maven-central.storage.googleapis.com") }
        }
    }
}
