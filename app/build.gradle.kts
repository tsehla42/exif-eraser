import java.io.FileInputStream
import java.util.Properties

data class KeyStoreConfig(
    val file: String,
    val password: String,
    val keyAlias: String,
    val keyPassword: String
)

fun loadKeyStoreConfig(): KeyStoreConfig {
    val properties = Properties().apply {
        val file = File(projectDir.parent, "keystore.properties")
        if (file.exists()) {
            load(FileInputStream(file))
        }
    }
    return KeyStoreConfig(
        file = properties.getProperty("ks.file") ?: "${System.getProperty("user.home")}/projects/android/certificates/exif-eraser-release.jks",
        password = properties.getProperty("ks.password") ?: "",
        keyAlias = properties.getProperty("ks.key.alias") ?: "exif-eraser",
        keyPassword = properties.getProperty("ks.key.password") ?: ""
    )
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.wire)
}

android {
    namespace = "com.tsehla.exiferaser"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.tsehla.exiferaser"
        minSdk = 28
        targetSdk = 37
        versionCode = 45
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            val ks = loadKeyStoreConfig()
            storeFile = file(ks.file)
            storePassword = ks.password
            keyAlias = ks.keyAlias
            keyPassword = ks.keyPassword
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "ExifEraser dev")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.implementation)
    ksp(libs.bundles.kapt)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
}

wire {
    kotlin {
        android = true
    }
}
