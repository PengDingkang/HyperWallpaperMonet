import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.agp.app)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties")
        .takeIf { it.isFile }
        ?.inputStream()
        ?.use(::load)
}

fun signingProperty(name: String, envName: String): String? =
    (findProperty(name) as? String)
        ?: localProperties.getProperty(name)
        ?: System.getenv(envName)

fun signingFile(path: String): File =
    File(path).takeIf { it.isAbsolute } ?: rootProject.file(path)

val releaseStoreFile = signingProperty("releaseStoreFile", "RELEASE_STORE_FILE")
val releaseStorePassword = signingProperty("releaseStorePassword", "RELEASE_STORE_PASSWORD")
val releaseKeyAlias = signingProperty("releaseKeyAlias", "RELEASE_KEY_ALIAS")
val releaseKeyPassword = signingProperty("releaseKeyPassword", "RELEASE_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }
val appVersionName = providers.gradleProperty("appVersionName").get()
val appVersionCode = providers.gradleProperty("appVersionCode").get().toInt()

android {
    namespace = "org.hdhmc.hyperwallpapermonet"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "org.hdhmc.hyperwallpapermonet"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = signingFile(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
        }
    }
}

dependencies {
    compileOnly(libs.libxposed.api)
}
