pluginManagement {
    repositories {
        google()
        maven {
            url = uri("https://maven-central.storage.googleapis.com")
            content { includeGroupByRegex(".*") }
        }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven {
            url = uri("https://maven-central.storage.googleapis.com")
            content { includeGroupByRegex(".*") }
        }
    }
}

rootProject.name = "TradeFlow"
include(":app")
 