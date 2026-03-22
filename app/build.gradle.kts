import org.gradle.api.GradleException
import org.gradle.internal.os.OperatingSystem
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

// Read API keys from local.properties (gitignored)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val releaseStoreFilePath = localProperties.getProperty("RELEASE_STORE_FILE", "").trim()
val releaseStorePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD", "").trim()
val releaseKeyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS", "").trim()
val releaseKeyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD", "").trim()
val hasConfiguredReleaseSigning =
    releaseStoreFilePath.isNotBlank() &&
        releaseStorePassword.isNotBlank() &&
        releaseKeyAlias.isNotBlank() &&
        releaseKeyPassword.isNotBlank()
val hasConfiguredRuntimeEnv =
    localProperties.getProperty("SUPABASE_URL", "").isNotBlank() &&
        localProperties.getProperty("SUPABASE_ANON_KEY", "").isNotBlank() &&
        localProperties.getProperty("ASSISTANT_PROXY_URL", "").isNotBlank() &&
        localProperties.getProperty("OTA_MANIFEST_URL", "").isNotBlank()
val requireReleaseSigning =
    providers.gradleProperty("requireReleaseSigning").orNull?.toBooleanStrictOrNull() ?: false
val requireReleaseRuntimeConfig =
    providers.gradleProperty("requireReleaseRuntimeConfig").orNull?.toBooleanStrictOrNull() ?: false

android {
    namespace = "com.personal.lifeOS"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.personal.lifeOS"
        minSdk = 26
        targetSdk = 35
        versionCode = 14
        versionName = "1.2.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        // Inject API keys into BuildConfig
        buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProperties.getProperty("SUPABASE_ANON_KEY", "")}\"")
        buildConfigField(
            "String",
            "ASSISTANT_PROXY_URL",
            "\"${localProperties.getProperty("ASSISTANT_PROXY_URL", "")}\"",
        )
        buildConfigField("String", "OTA_MANIFEST_URL", "\"${localProperties.getProperty("OTA_MANIFEST_URL", "")}\"")
    }

    signingConfigs {
        create("release") {
            if (hasConfiguredReleaseSigning) {
                storeFile = file(releaseStoreFilePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasConfiguredReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    ignoreFailures = false
    config.setFrom(rootProject.file("detekt.yml"))
    baseline = file("$projectDir/detekt-baseline.xml")
}

ktlint {
    ignoreFailures.set(false)
    android.set(true)
    baseline.set(file("$projectDir/config/ktlint/baseline.xml"))
}

tasks.named("check") {
    dependsOn("detekt", "ktlintCheck", "architectureBoundaryCheck", "secretScan")
}

tasks.register("verifyReleaseSigningConfig") {
    group = "verification"
    description = "Fails when release signing credentials are missing or invalid."
    doLast {
        if (!hasConfiguredReleaseSigning) {
            throw GradleException(
                "Release signing is not configured. " +
                    "Add RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, " +
                    "RELEASE_KEY_PASSWORD to local.properties.",
            )
        }
        if (!file(releaseStoreFilePath).exists()) {
            throw GradleException("Release keystore file not found at: $releaseStoreFilePath")
        }
    }
}

tasks.register("verifyReleaseRuntimeConfig") {
    group = "verification"
    description = "Fails when runtime backend/OTA config is missing for release."
    doLast {
        if (!hasConfiguredRuntimeEnv) {
            throw GradleException(
                "Release runtime config is missing. " +
                    "Add SUPABASE_URL, SUPABASE_ANON_KEY, ASSISTANT_PROXY_URL, " +
                    "OTA_MANIFEST_URL to local.properties.",
            )
        }
    }
}

if (requireReleaseSigning) {
    tasks
        .matching { it.name in listOf("assembleRelease", "bundleRelease", "buildReleasePreBundle") }
        .configureEach {
            dependsOn("verifyReleaseSigningConfig")
        }
}

if (requireReleaseRuntimeConfig) {
    tasks
        .matching { it.name in listOf("assembleRelease", "bundleRelease", "buildReleasePreBundle") }
        .configureEach {
            dependsOn("verifyReleaseRuntimeConfig")
        }
}

tasks.register<Exec>("architectureBoundaryCheck") {
    group = "verification"
    description = "Fails build when UI/presentation touches forbidden data/network internals."
    workingDir = rootProject.rootDir
    if (OperatingSystem.current().isWindows) {
        commandLine("powershell", "-ExecutionPolicy", "Bypass", "-File", "scripts/architecture_boundary_check.ps1")
    } else {
        commandLine("pwsh", "-File", "scripts/architecture_boundary_check.ps1")
    }
}

tasks.register<Exec>("secretScan") {
    group = "verification"
    description = "Scans source files for potential committed secrets."
    workingDir = rootProject.rootDir
    if (OperatingSystem.current().isWindows) {
        commandLine("powershell", "-ExecutionPolicy", "Bypass", "-File", "scripts/secret_scan.ps1")
    } else {
        commandLine("pwsh", "-File", "scripts/secret_scan.ps1")
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.ui.tooling.preview)

    // Navigation
    implementation(libs.androidx.navigation)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.room.paging)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines)

    // Vico Charts (stable 1.13.1)
    implementation(libs.vico.compose)

    // Security
    implementation(libs.biometric)
    implementation(libs.androidx.security.crypto)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Image Loading
    implementation(libs.coil.compose)

    // Networking (Supabase + assistant proxy)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.androidx.work.runtime)

    // Unit tests
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.room.testing)
}
