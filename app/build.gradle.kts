plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Cada compilación en GitHub sube el número de versión, así el celular
// acepta la nueva versión como actualización sin desinstalar.
val numeroCompilacion = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "ar.escanerflotante"
    compileSdk = 35

    defaultConfig {
        applicationId = "ar.escanerflotante"
        minSdk = 26
        targetSdk = 34
        versionCode = numeroCompilacion
        versionName = "1.$numeroCompilacion"
    }

    signingConfigs {
        create("release") {
            storeFile = file("escaner.keystore")
            storePassword = "escaner123"
            keyAlias = "escaner"
            keyPassword = "escaner123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    val camerax = "1.4.1"
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")

    // Lector de códigos de Google, incluido en la app (funciona sin internet)
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
}
