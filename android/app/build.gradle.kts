plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.minimind.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.minimind.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            pickFirsts += "commonMain/**"
            pickFirsts += "nativeMain/**"
            pickFirsts += "META-INF/kotlin-project-structure-metadata.json"
        }
    }
}

configurations.all {
    resolutionStrategy {
        force("androidx.collection:collection:1.4.0")
        force("androidx.collection:collection-jvm:1.4.0")
        force("androidx.collection:collection-ktx:1.4.0")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation("androidx.compose.ui:ui-android:1.5.4")
    implementation("androidx.compose.ui:ui-graphics-android:1.5.4")
    implementation("androidx.compose.ui:ui-tooling-preview-android:1.5.4")
    implementation("androidx.compose.ui:ui-unit-android:1.5.4")
    implementation("androidx.compose.ui:ui-geometry-android:1.5.4")
    implementation("androidx.compose.ui:ui-text-android:1.5.4")
    implementation("androidx.compose.material3:material3-android:1.2.0")
    implementation("androidx.compose.material:material-icons-extended-android:1.5.4")
    implementation("androidx.compose.runtime:runtime-android:1.5.4")
    implementation("androidx.compose.foundation:foundation-android:1.5.4")
    implementation("androidx.compose.material:material-android:1.5.4")
    implementation("androidx.compose.animation:animation-android:1.5.4")
    implementation("androidx.compose.animation:animation-core-android:1.5.4")
    implementation("androidx.compose.runtime:runtime-saveable-android:1.5.4")
    implementation("androidx.compose.material:material-ripple-android:1.5.4")
    implementation("androidx.compose.material:material-icons-core-android:1.5.4")
    implementation("androidx.compose.foundation:foundation-layout-android:1.5.4")
    implementation("androidx.compose.ui:ui-util-android:1.5.4")
    implementation("androidx.compose.animation:animation-graphics-android:1.5.4")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("androidx.datastore:datastore-preferences:1.0.0")
}
