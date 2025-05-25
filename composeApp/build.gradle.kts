import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream


plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.hotreload)
}

composeCompiler {
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
}

val localPropertiesFile = file("../local.properties")

val localProperties = Properties();
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProperties.apply { load(localPropertiesFile.inputStream()) }
}

// Helper to get from env or local.properties or use default
fun getEnvOrLocal(key: String): String {
    return System.getenv(key) ?: localProperties.getProperty(key) ?: error("Missing property: $key")
}


buildkonfig {
    packageName = "com.andreasgift.kmpweatherapp"

    defaultConfigs {
        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "FIREBASE_PROJECT_ID",
            getEnvOrLocal("FIREBASE_PROJECT_ID")
        )

        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "FIREBASE_APPLICATION_ID",
            getEnvOrLocal("FIREBASE_APPLICATION_ID")
        )

        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "FIREBASE_API_KEY",
            getEnvOrLocal("FIREBASE_API_KEY")
        )
    }
}

val versionPropertiesInputStream = FileInputStream("$rootDir/versions.properties")
val versionProperties = Properties().apply {
    load(versionPropertiesInputStream)
}
val versionCodeProperty = versionProperties.getProperty("versionCode").toInt()
val versionMajorProperty = versionProperties.getProperty("versionMajor").toInt()
val versionMinorProperty = versionProperties.getProperty("versionMinor").toInt()
val versionPatchProperty = versionProperties.getProperty("versionPatch").toInt()

val versionNameProperty = "$versionMajorProperty.$versionMinorProperty.$versionPatchProperty"

kotlin {

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(project.dependencies.platform(libs.android.firebase.bom))
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.couroutines.core)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(compose.materialIconsExtended)
            implementation(libs.androidx.navigation)
            implementation(libs.kotlinx.serialization.json)
            // firebase
            implementation(libs.gitlive.firebase.firestore)
            implementation(libs.gitlive.firebase.auth)
            //implementation(libs.gitlive.firebase.crashlytics)

            implementation(libs.kermit)

        }
        desktopMain.dependencies {
            // TODO delete when this pr is merged: https://github.com/GitLiveApp/firebase-java-sdk/pull/33
            implementation("dev.gitlive:firebase-java-sdk:1.2.3")
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.pdfbox)
        }
    }
}

android {
    namespace = "org.futterbock.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "org.futterbock.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = versionCodeProperty
        versionName = versionNameProperty
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    dependencies {
        debugImplementation(compose.uiTooling)
    }
}

compose.desktop {
    application {
        buildTypes.release.proguard {

            obfuscate.set(false)
            version.set("7.7.0")
            configurationFiles.from(project.file("compose-desktop.pro"))
        }
        mainClass = "MainKt"

        nativeDistributions {
            modules(
                "java.compiler",
                "java.instrument",
                "java.management",
                "java.naming",
                "java.sql",
                "jdk.unsupported"
            )
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Futterbock"
            packageVersion = versionNameProperty

            //includeAllModules = true

            windows {
                menuGroup = "Futterbock"
                iconFile.set(project.file("src/resources/futterbock_icon.ico"))
                //dirChooser = true
                //installationPath = "\\FutterbockApp"
            }
            macOS {
                iconFile.set(project.file("src/resources/futterbock_icon.icns"))
            }
        }
    }
}
