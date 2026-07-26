import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.com.google.dagger.hilt)
    alias(libs.plugins.com.mikepenz.aboutlibraries)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.com.google.devtools.ksp)
}

android {
    namespace = "vegabobo.languageselector"
    compileSdk = 37

    defaultConfig {
        applicationId = "vegabobo.languageselector"
        minSdk = 33
        // Compiled against the newest APIs, but runtime behaviour changes are opted
        // into separately once they can be verified on a device.
        targetSdk = 36
        versionCode = 5
        versionName = "1.04"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Set by CI from repository secrets. Without them the release build keeps falling back to
    // the debug key, so local builds behave exactly as before.
    val releaseKeystore = System.getenv("KEYSTORE_FILE")?.takeIf { it.isNotBlank() }?.let(::file)
    if (releaseKeystore != null) {
        signingConfigs.create("release") {
            storeFile = releaseKeystore
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        buildConfig = true
        compose = true
        aidl = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

aboutLibraries {
    export {
        // "generated" only records the timestamp of the last export and would make
        // every build produce a different resource, breaking build reproducibility.
        excludeFields.add("generated")
    }
}

dependencies {
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    implementation(libs.libsu.core)
    implementation(libs.libsu.service)

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material)
    implementation(libs.material3)

    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.aboutlibraries.core)

    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    compileOnly(project(":hidden_api"))
}