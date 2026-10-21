import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// Firma del release: variables de entorno (CI) o local.properties. Sin ellas el release queda sin firmar.
val localProps = Properties().apply {
    runCatching { load(rootProject.file("local.properties").inputStream()) }
}

fun secret(name: String): String? = localProps.getProperty(name) ?: System.getenv(name)

@Suppress("UnstableApiUsage")
android {
    namespace = "com.jonnyuun.decocenter"
    compileSdk {
        version = release(37) { minorApiLevel = 0 }
    }

    defaultConfig {
        applicationId = "com.jonnyuun.decocenter"
        minSdk = 24
        // Google Play exige API 36 o superior para apps nuevas y actualizaciones desde el 31/08/2026.
        targetSdk = 36
        versionCode = 3
        versionName = "3.0"
    }

    val keystorePath = secret("KEYSTORE_PATH")
    if (keystorePath != null) {
        signingConfigs {
            register("release") {
                storeFile = file(keystorePath)
                storePassword = secret("KEYSTORE_PASS")
                keyAlias = secret("KEY_ALIAS")
                keyPassword = secret("KEY_PASSWORD")
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            optimization.enable = true
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    // Miuix se compila con JVM 21: el módulo debe usar el mismo destino para poder inlinar sus funciones.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.preference)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
