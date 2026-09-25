pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Vendored in-repo Maven repo (libs/m2repo): project artifacts that are not on
        // any public repository. Replaces the machine-specific mavenLocal().
        maven("libs/m2repo")
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "WordVibe"
include(":app")
