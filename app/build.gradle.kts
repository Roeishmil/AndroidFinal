plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("kotlin-kapt")

}

android {
    namespace = "com.rs.photoshare"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rs.photoshare"
        minSdk = 35
        targetSdk = 35
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
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Firebase BoM (manages Firebase versions)
    implementation(platform(libs.firebase.bom))
    // Firebase Analytics
    implementation(libs.firebase.analytics.ktx)
    // Firebase BoM (manages Firebase versions)
    implementation(platform(libs.firebase.bom.v3390))

    // Firebase Authentication
    implementation(libs.firebase.auth.ktx)

    // Firestore (since we’ll store user details in Firestore after sign up)
    implementation(libs.firebase.firestore.ktx)

    implementation(libs.androidx.constraintlayout)

    implementation(libs.material)

    implementation (libs.androidx.navigation.compose)

    implementation (libs.picasso)

    implementation (libs.gson)

    // Cloudinary
    implementation(libs.cloudinary.android)
    implementation(libs.cloudinary.core)
// Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    implementation (libs.material.v190)


    implementation (libs.androidx.room.runtime)
    kapt (libs.androidx.room.compiler)
    implementation (libs.androidx.room.ktx)

    implementation (libs.material.v160)
    implementation (libs.material.v170)

}