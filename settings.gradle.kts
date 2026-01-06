pluginManagement {
    repositories {
        google()
        maven { url = uri("https://maven-central.storage.googleapis.com") }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven { url = uri("https://maven-central.storage.googleapis.com") }
        mavenCentral()
    }
}

rootProject.name = "TradeFlow"
include(":app")
 