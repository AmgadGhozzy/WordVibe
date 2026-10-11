import org.jetbrains.kotlin.konan.properties.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.serialization)

    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("androidx.baselineprofile")
}

val localProperties = Properties()
// local.properties is optional: clean checkouts without local keys must still configure.
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
val versionProperties = Properties()

android {
    namespace = "com.venom.wordvibe"
    compileSdk = 37
    lint {
        checkReleaseBuilds = true
    }
    defaultConfig {
        applicationId = "com.venom.wordvibe"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

//        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", localProperties.getProperty("GOOGLE_WEB_CLIENT_ID"))
//        buildConfigField("String", "SUPABASE_URL", localProperties.getProperty("SUPABASE_URL"))
//        buildConfigField("String", "SUPABASE_ANON_KEY", localProperties.getProperty("SUPABASE_ANON_KEY"))

        androidResources {
            localeFilters += listOf("en", "ar")
        }
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

    baselineProfile {
        dexLayoutOptimization = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.version",
                "**/dump_syms*"
            )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        getByName("androidTest") {
            assets.srcDir("$projectDir/schemas")
        }
    }
}

dependencies {

    // Baseline Profile
    //baselineProfile(project(":baselineprofile"))

    // Firebase (messaging wired; analytics/crashlytics via plugins, no direct calls yet)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.animation)

    // Hilt
    api(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    api(libs.hilt.navigation.compose)

    // Room
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Datastore
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.serialization.json)

    // Android Jetpack
    api(libs.androidx.core.ktx)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(platform(libs.compose.bom))
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.lifecycle.viewmodel.ktx)

    // Compose
    api(libs.compose.ui)
    api(libs.compose.ui.graphics)
    api(libs.compose.ui.tooling.preview)
    api(libs.compose.material3)
    api(libs.compose.animation)
    implementation(libs.compose.material.icons.extended)

    // Kotlin
    api(libs.kotlin.stdlib)

    // Shared Synapse libraries (vendored in libs/m2repo; no mavenLocal needed)
    implementation("com.venom:design-system:1.0.0")


    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:2.3.10")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
