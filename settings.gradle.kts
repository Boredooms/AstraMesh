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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AstraMesh"

include(":app")
include(":core-domain")
include(":core-protocol")
include(":core-routing")
include(":core-security")
include(":core-transport")
include(":core-persistence")
include(":core-mesh")
include(":core-ui")
include(":feature-discovery")
include(":feature-chat")
include(":feature-files")
include(":feature-broadcast")
include(":feature-settings")
include(":desktop")

// Note: `web/` is a standalone Next.js app and is intentionally NOT a Gradle module.
