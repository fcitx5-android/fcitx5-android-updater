import com.android.build.gradle.internal.dsl.SigningConfig
import com.android.build.gradle.internal.tasks.CompileArtProfileTask
import com.android.build.gradle.internal.tasks.ExpandArtProfileWildcardsTask
import com.android.build.gradle.internal.tasks.MergeArtProfileTask
import com.android.build.gradle.tasks.PackageApplication
import org.gradle.api.internal.provider.AbstractProperty
import org.gradle.api.internal.provider.Providers
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
    kotlin("plugin.parcelize")
}

android {
    namespace = "org.fcitx.fcitx5.android.updater"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "org.fcitx.fcitx5.android.updater"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = exec("git describe --tags --long --always", "1.1.0")
        setProperty("archivesBaseName", "$applicationId-$versionName")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.createSigningConfigFromEnv()
        }
        debug {
            applicationIdSuffix = ".debug"
        }
        all {
            // remove META-INF/version-control-info.textproto
            @Suppress("UnstableApiUsage")
            vcsInfo.include = false
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/*.version",
                "/META-INF/*.kotlin_module",
                "/kotlin/**",
                "/DebugProbesKt.bin",
                "/kotlin-tooling-metadata.json"
            )
        }
    }
    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

// remove META-INF/com/android/build/gradle/app-metadata.properties
tasks.withType<PackageApplication> {
    val valueField =
        AbstractProperty::class.java.declaredFields.find { it.name == "value" } ?: run {
            println("class AbstractProperty field value not found, something could have gone wrong")
            return@withType
        }
    valueField.isAccessible = true
    doFirst {
        valueField.set(appMetadata, Providers.notDefined<RegularFile>())
        allInputFilesWithNameOnlyPathSensitivity.removeAll { true }
    }
}

// remove assets/dexopt/baseline.prof{,m} (baseline profile)
tasks.withType<MergeArtProfileTask> { enabled = false }
tasks.withType<ExpandArtProfileWildcardsTask> { enabled = false }
tasks.withType<CompileArtProfileTask> { enabled = false }

dependencies {
    implementation("net.swiftzer.semver:semver:2.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.11.00"))
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    val lifecycleVersion = "2.8.7"
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
}

configurations {
    all {
        // remove Baseline Profile Installer or whatever it is...
        exclude(group = "androidx.profileinstaller", module = "profileinstaller")
        // remove libandroidx.graphics.path.so
        exclude(group = "androidx.graphics", module = "graphics-path")
    }
}

fun exec(cmd: String, defaultValue: String = ""): String {
    val stdout = ByteArrayOutputStream()
    val result = stdout.use {
        project.exec {
            commandLine = cmd.split(" ")
            standardOutput = stdout
        }
    }
    return if (result.exitValue == 0) stdout.toString().trim() else defaultValue
}

fun env(name: String): String? = System.getenv(name)

private var signKeyTempFile: File? = null

fun NamedDomainObjectContainer<SigningConfig>.createSigningConfigFromEnv(): SigningConfig? {
    var signKeyFile: File? = null
    env("SIGN_KEY_FILE")?.let {
        val file = File(it)
        if (file.exists()) {
            signKeyFile = file
        }
    }
    @OptIn(ExperimentalEncodingApi::class)
    env("SIGN_KEY_BASE64")?.let {
        if (signKeyTempFile?.exists() == true) {
            signKeyFile = signKeyTempFile
        } else {
            val buildDir = layout.buildDirectory.asFile.get()
            buildDir.mkdirs()
            val file = File.createTempFile("sign-", ".ks", buildDir)
            try {
                file.writeBytes(Base64.decode(it))
                file.deleteOnExit()
                signKeyFile = file
                signKeyTempFile = file
            } catch (e: Exception) {
                file.delete()
            }
        }
    }
    signKeyFile ?: return null
    return create("release") {
        storeFile = signKeyFile
        storePassword = env("SIGN_KEY_PWD")
        keyAlias = env("SIGN_KEY_ALIAS")
        keyPassword = env("SIGN_KEY_PWD")
    }
}
