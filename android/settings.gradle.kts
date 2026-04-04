// Plugin management: defines where Gradle should look for plugin artifacts
pluginManagement {
    repositories {
        google()          // Google's Maven repo (Android Gradle Plugin, Google Services, etc.)
        mavenCentral()    // Maven Central (Kotlin plugins, etc.)
        gradlePluginPortal() // Gradle Plugin Portal (community plugins)
    }
}

// Dependency resolution management: defines where to resolve library dependencies
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TriggerApp"
include(":app")
