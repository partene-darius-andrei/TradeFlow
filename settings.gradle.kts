pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TradeFlow"
include(":app")
include(":core:domain")
include(":core:data")
include(":core:ui")
include(":exchange:coinbase")
include(":feature:dashboard")
include(":feature:trading")
include(":feature:settings")
