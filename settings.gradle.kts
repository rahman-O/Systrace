pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.google\\.devtools.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SysTrace"
include(":app")
include(":core:common")
include(":core:domain")
include(":core:database")
include(":core:network")
include(":core:permissions")
include(":core:utils")
include(":service")
include(":presentation")
include(":data")
