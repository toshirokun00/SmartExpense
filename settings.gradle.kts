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

rootProject.name = "SmartExpense"
include(":app")
include(":core:common")
include(":core:designsystems")
include(":core:database")
include(":core:network")
include(":domain")
include(":data")
include(":feature:expenses")
include(":feature:auth")
include(":feature:dashoard")
include(":feature:budget")
include(":feature:settings")
include(":feature:reports")
include(":feature:categories")
