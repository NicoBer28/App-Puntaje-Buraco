import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Capa de datos: implementa los repositorios del dominio con Firestore, DataStore y LiteRT.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Host de los emuladores de Firebase para el build de debug (ver docs/ARQUITECTURA.md). Se indica
// con -PemuladoresFirebase=10.0.2.2 o con esa misma clave en local.properties. Sin él, la app usa
// el proyecto real.
val hostEmuladores: String? = providers.gradleProperty("emuladoresFirebase").orNull
    ?: rootProject.file("local.properties").takeIf { it.exists() }?.let { archivo ->
        Properties().apply { archivo.inputStream().use(::load) }.getProperty("emuladoresFirebase")
    }

android {
    namespace = "com.example.puntajeburaco20.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "HOST_EMULADORES", "null")
    }

    buildTypes {
        debug {
            buildConfigField("String", "HOST_EMULADORES", hostEmuladores?.let { "\"$it\"" } ?: "null")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    api(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)

    implementation(libs.litert)

    testImplementation(libs.junit)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}
