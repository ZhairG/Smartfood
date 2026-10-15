plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.smartfood.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.smartfood.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.smartfood.app.CustomTestRunner"
    }

    buildTypes {
        // Opciones de desarrollo (spec 003, plan §6): solo el build debug las lee de Gradle.
        debug {
            buildConfigField("boolean", "USE_DEMO_RECOVERY", "${booleanProperty("recoveryDemo")}")
            buildConfigField("boolean", "USE_FIREBASE_EMULATOR", "${booleanProperty("useFirebaseEmulator")}")
            buildConfigField(
                "String",
                "FIREBASE_EMULATOR_HOST",
                "\"${providers.gradleProperty("firebaseEmulatorHost").getOrElse("10.0.2.2")}\""
            )
        }
        release {
            buildConfigField("boolean", "USE_DEMO_RECOVERY", "false")
            buildConfigField("boolean", "USE_FIREBASE_EMULATOR", "false")
            buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time (LocalDate, Clock) en el dominio con minSdk 24 (spec 006, plan §9).
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        // Las excepciones de Firebase usan android.text.TextUtils en su constructor; en la JVM devuelve valores por defecto.
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Navigation Compose y Serialization (Type-Safe)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Dagger Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Corrutinas Kotlin
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Firebase (BOM, Auth, Firestore, Functions y Storage)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.storage)

    // Inventario (spec 006): cámara, imágenes y avisos en segundo plano
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // java.time con minSdk 24
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Preferencias locales ("Recuérdame")
    implementation(libs.androidx.datastore.preferences)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

/** Lee una propiedad booleana de Gradle (`-Pnombre=true`); por defecto `false`. */
fun booleanProperty(name: String): Boolean =
    providers.gradleProperty(name).map { it.toBoolean() }.getOrElse(false)
