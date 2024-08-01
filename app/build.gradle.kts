@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.tasks.CompileArtProfileTask
import com.android.build.gradle.internal.tasks.ExpandArtProfileWildcardsTask
import com.android.build.gradle.internal.tasks.MergeArtProfileTask
import com.android.build.gradle.tasks.PackageApplication
import org.gradle.api.internal.provider.AbstractProperty
import org.gradle.api.internal.provider.Providers
import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
}

fun exec(cmd: String): String = ByteArrayOutputStream().use {
    project.exec {
        commandLine = cmd.split(" ")
        standardOutput = it
    }
    it.toString().trim()
}

android {
    namespace = "org.fcitx.fcitx5.android.updater"
    compileSdk = 34
    buildToolsVersion = "34.0.0"
    defaultConfig {
        applicationId = "org.fcitx.fcitx5.android.updater"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = exec("git describe --tags --long --always")
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
        }
        debug {
            applicationIdSuffix = ".debug"
        }
        forEach {
            it.vcsInfo.include = false
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
                "/kotlin-tooling-metadata.json"
            )
        }
    }
    androidResources {
        generateLocaleConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    val lifecycleVersion = "2.8.4"
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
}

configurations {
    all {
        // remove Baseline Profile Installer or whatever it is...
        exclude(group = "androidx.profileinstaller", module = "profileinstaller")
    }
}
