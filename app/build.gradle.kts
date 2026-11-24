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

    // AGP 9 removed the old Variant API (applicationVariants / outputFileName), so a release
    // APK can no longer be renamed per-variant. friendlyReleaseApk (below) writes a
    // friendly-named copy beside the default output instead.

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

    // Google's dependency-info BLOB (signing block 0x504b4453) is encrypted with Google's
    // public key and is randomised on every build, so it breaks byte-for-byte reproducibility.
    // It is only meaningful to the Play Store, so leave it out of the APK.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
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

// AGP 9 removed the old Variant API (applicationVariants / outputFileName), so a release APK can
// no longer be renamed per-variant. Write a friendly-named copy beside the default output after
// every assembleRelease instead:
//   app/build/outputs/apk/release/exif-eraser-<versionName>.apk
val friendlyReleaseApk by tasks.registering {
    group = "build"
    description = "Copies the release APK to exif-eraser-<versionName>.apk"
    doLast {
        val dir = project.layout.buildDirectory.dir("outputs/apk/release").get().asFile
        // fdroidserver strips signingConfigs, so the output may be app-release-unsigned.apk
        // rather than app-release.apk. This copy is cosmetic, so never fail the build over it.
        val src = dir.listFiles()?.firstOrNull {
            it.isFile && it.extension == "apk" && !it.name.startsWith("exif-eraser-")
        }
        if (src == null) {
            logger.lifecycle("friendlyReleaseApk: no release APK in ${dir.path}, skipping")
            return@doLast
        }
        val metadata = File(dir, "output-metadata.json").takeIf { it.exists() }?.readText().orEmpty()
        val version = Regex("\"versionName\"\\s*:\\s*\"([^\"]+)\"").find(metadata)?.groupValues?.get(1)
            ?: "unversioned"
        val dst = File(dir, "exif-eraser-$version.apk")
        src.copyTo(dst, overwrite = true)
        logger.lifecycle("Wrote ${dst.path}")
    }
}

afterEvaluate {
    tasks.named("assembleRelease") { finalizedBy(friendlyReleaseApk) }
}
