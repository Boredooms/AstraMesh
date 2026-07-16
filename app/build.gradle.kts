plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.astramesh.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.astramesh.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // A fixed, project-committed debug keystore (app/debug.keystore) instead of relying on
    // the auto-generated one at ~/.android/debug.keystore. That auto-generated keystore is
    // per-machine, and every GitHub Actions run is a fresh, ephemeral machine -- so each CI
    // build was silently signed with a DIFFERENT certificate every time. Android refuses to
    // install an APK over an existing app unless the signing certificate matches exactly,
    // which is exactly why installing a newer release failed with "package conflicts with an
    // existing package": v0.1.0 and v0.1.1 had the same applicationId but different, randomly
    // generated signatures. Using one fixed keystore for every build (local and CI) makes all
    // debug-signed builds mutually upgradable.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core-domain"))
    implementation(project(":core-protocol"))
    implementation(project(":core-routing"))
    implementation(project(":core-security"))
    implementation(project(":core-transport"))
    implementation(project(":core-persistence"))
    implementation(project(":core-mesh"))
    implementation(project(":core-ui"))
    implementation(project(":feature-discovery"))
    implementation(project(":feature-chat"))
    implementation(project(":feature-files"))
    implementation(project(":feature-broadcast"))
    implementation(project(":feature-settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
