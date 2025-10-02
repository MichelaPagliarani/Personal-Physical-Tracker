plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
}

android {
    namespace = "com.example.personalphysicaltracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.personalphysicaltracker"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation (libs.android.lottie)
    implementation(libs.androidx.navigation.common.ktx)

    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.core.android)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.compose.material.core)

    kapt(libs.androidx.room.compiler)

    implementation(libs.play.services.location)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.notify)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore)
    implementation (libs.androidx.lifecycle.livedata.ktx)

    //implementation ("androidx.core:core-ktx:1.6.0")
    //implementation ("com.google.android.gms:play-services-location:21.3.0")
    implementation (libs.github.mpandroidchart)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}