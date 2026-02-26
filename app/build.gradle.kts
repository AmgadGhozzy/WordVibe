import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.serialization)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("dagger.hilt.android.plugin")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

val versionFile = rootProject.file("version.properties")

val localProperties = Properties()
val versionProperties = Properties()
localProperties.load(FileInputStream(rootProject.file("local.properties")))
versionProperties.load(FileInputStream(versionFile))

val localVersionCode = versionProperties.getProperty("APP_VERSION_CODE").toInt()
val localVersionName = versionProperties.getProperty("APP_VERSION_NAME")

android {
    namespace = "com.venom.wordvibe"
    compileSdk = 36
    lint {
        disable.add("NullSafeMutableLiveData")
        checkReleaseBuilds = true
    }
    defaultConfig {
        applicationId = "com.venom.wordvibe"
        minSdk = 24
        targetSdk = 36
        versionCode = localVersionCode
        versionName = "${localVersionName}${localVersionCode}"
        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("arm-v7a")
        }
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resourceConfigurations.addAll(listOf("en", "ar"))
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.version",
                "/META-INF/kotlin-project-structure-metadata.json",
                "**/dump_syms*"
            )
        }
    }
    android {
        lint {
            disable += "NullSafeMutableLiveData"
        }
    }
    // Auto increment app version
    gradle.startParameter.taskNames.forEach {
        if (it.contains(":app:assembleRelease")) {
            versionFile.bufferedWriter().use { file ->
                file.write("APP_VERSION_CODE=${(localVersionCode + 1)}")
            }
        }
    }

    tasks.whenTaskAdded {
        if (name == "assembleDebug") {
            doLast {
                project.exec {
                    commandLine("cmd", "/c", "D:\\Amgad\\unlock_device.bat")
                    isIgnoreExitValue = true
                }
            }
        }
    }
}


dependencies {

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // Ads
    api(libs.play.services.ads)

    // Core
    implementation("com.venom:core:data")
    implementation("com.venom:core:ui")
    implementation("com.venom:core:di")
    implementation("com.venom:core:resources")
    implementation("com.venom:core:utils")
    implementation("com.venom:core:domain")
    implementation("com.venom:core:analytics")

    // Features
    implementation("com.venom:features:translation")
    implementation("com.venom:features:stackcard")
    implementation("com.venom:features:quiz")
    implementation("com.venom:features:lingospell")
    implementation("com.venom:features:settings")
    
    implementation("com.venom:features:phrase")
    implementation("com.venom:features:dialog")
    implementation("com.venom:features:ocr")
    implementation("com.venom:features:quote")
    implementation("com.venom:features:wordcraftai")


    // Android Jetpack
    api(libs.androidx.core.ktx)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(platform(libs.compose.bom))
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.lifecycle.viewmodel.ktx)
    api(libs.androidx.fragment.ktx)

    // Hilt
    api(libs.hilt.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.splashscreen)
    ksp(libs.hilt.android.compiler)
    api(libs.hilt.navigation.compose)

    // Networking
    api(libs.okhttp)
    api(libs.retrofit)
    api(libs.converter.moshi)
    api(libs.moshi)

    // Room
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.serialization.json)


    // Compose
    api(libs.compose.ui)
    api(libs.compose.ui.graphics)
    api(libs.compose.ui.tooling.preview)
    api(libs.compose.material3)
    api(libs.compose.animation)
    api(libs.coil.compose)
    api(libs.compose.material.icons.extended) {
        exclude(group = "androidx.compose.material.icons", module = "filled")
        exclude(group = "androidx.compose.material.icons", module = "outlined")
        exclude(group = "androidx.compose.material.icons", module = "round")
        exclude(group = "androidx.compose.material.icons", module = "sharp")
        exclude(group = "androidx.compose.material.icons", module = "twotone")
    }


    // Kotlin
    api(libs.kotlin.stdlib)
    api(libs.androidx.exifinterface)


    // Testing
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

}


