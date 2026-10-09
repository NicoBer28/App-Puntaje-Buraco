import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Capa de dominio: Kotlin puro. No puede depender de Android, Firebase ni de las otras capas.
plugins {
    alias(libs.plugins.kotlin.jvm)
    // Expone los repositorios en memoria (fakes) para los tests de las demás capas.
    `java-test-fixtures`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // Solo las anotaciones estándar (JSR-330): el dominio no conoce a Hilt.
    implementation(libs.javax.inject)
    api(libs.kotlinx.coroutines.core)

    testFixturesImplementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
