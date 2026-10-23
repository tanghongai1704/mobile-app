pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "OneSpace"
include(":app")
include(":domain")
include(":core:common", ":core:designsystem", ":core:session", ":core:media")
include(":core:network", ":core:database", ":core:datastore", ":core:sync")
include(":data:video", ":data:music", ":data:news", ":data:games")
include(":feature:home", ":feature:video", ":feature:music", ":feature:news", ":feature:games", ":feature:account", ":feature:onboarding")
