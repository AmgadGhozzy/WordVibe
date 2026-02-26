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
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

includeBuild("C:\\Users\\HP\\AndroidStudioProjects\\LingoLens") {
    dependencySubstitution {
        substitute(module("com.venom:features:translation")).using(project(":features:translation"))
        substitute(module("com.venom:features:phrase")).using(project(":features:phrase"))
        substitute(module("com.venom:features:dialog")).using(project(":features:dialog"))
        substitute(module("com.venom:features:stackcard")).using(project(":features:stackcard"))
        substitute(module("com.venom:features:quiz")).using(project(":features:quiz"))
        substitute(module("com.venom:features:lingospell")).using(project(":features:lingospell"))
        substitute(module("com.venom:features:quote")).using(project(":features:quote"))
        substitute(module("com.venom:features:wordcraftai")).using(project(":features:wordcraftai"))
        substitute(module("com.venom:features:ocr")).using(project(":features:ocr"))
        substitute(module("com.venom:features:settings")).using(project(":features:settings"))
        substitute(module("com.venom:core:ui")).using(project(":core:ui"))
        substitute(module("com.venom:core:data")).using(project(":core:data"))
        substitute(module("com.venom:core:di")).using(project(":core:di"))
        substitute(module("com.venom:core:resources")).using(project(":core:resources"))
        substitute(module("com.venom:core:utils")).using(project(":core:utils"))
        substitute(module("com.venom:core:domain")).using(project(":core:domain"))
        substitute(module("com.venom:core:analytics")).using(project(":core:analytics"))
    }
}

rootProject.name = "WordVibe"
include(":app")