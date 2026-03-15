plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.puntaje_buraco_3"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.puntajeburaco20"
        minSdk = 24
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Firestore para Java
    implementation(libs.firebase.firestore)

    // Navigation Component para Java
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth:23.0.0")

    // Herramientas de Google para el botón de "Sign in with Google"
    implementation("com.google.android.gms:play-services-auth:21.0.0")


}