@file:OptIn(ExperimentalWasmDsl::class, ExperimentalKotlinGradlePluginApi::class)

import com.vanniktech.maven.publish.SonatypeHost
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.compose")
    kotlin("multiplatform")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
}

allprojects {
    group = "dev.toastbits.composekit"
    version = "0.0.3-SNAPSHOT"
}

kotlin {
    androidTarget()

    jvm("desktop")

    wasmJs {
        browser()
    }

    applyDefaultHierarchyTemplate {
        common {
            withAndroidTarget()
            withJvm()
            withWasmJs()

            group("jvm") {
                withAndroidTarget()
                withJvm()
            }
            group("cmpJbr") {
                withJvm()
                withWasmJs()
            }
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlinx.serialization.ExperimentalSerializationApi")

                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
                optIn("androidx.compose.foundation.ExperimentalFoundationApi")
                optIn("androidx.compose.foundation.layout.ExperimentalLayoutApi")
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                optIn("androidx.compose.material.ExperimentalMaterialApi")
                optIn("androidx.compose.ui.ExperimentalComposeUiApi")

                enableLanguageFeature("ExpectActualClasses")
            }
        }

        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.material)
                implementation(compose.material3)
                implementation(compose.foundation)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)

                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)

                implementation("com.github.toasterofbread.catppuccin-kotlin:catppuccin-kotlin:cde14d00dd")
                implementation("com.github.toasterofbread.compose-color-picker:compose-color-picker:19c9fb4736")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                api("com.squareup.okio:okio:3.9.1")
            }
        }

        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.9.3")

                implementation("com.anggrayudi:storage:1.5.5")
                implementation("com.google.accompanist:accompanist-swiperefresh:0.21.2-beta")
                implementation("com.github.andob:android-awt:1.0.0")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)
                implementation("com.sshtools:two-slices:0.9.1")
                implementation("com.github.toasterofbread:gdx-nativefilechooser:325fa2a")
            }
        }

        val wasmJsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-browser:0.2")
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":testing-library"))
            }
        }
    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "dev.toastbits.composekit"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.2"
    }
    buildFeatures {
        compose = true
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    // signAllPublications()

    configure(KotlinMultiplatform(
        sourcesJar = true,
        androidVariantsToPublish = listOf("release")
    ))

    pom {
        name.set("ComposeKit")
        description.set("A collection of common code for use in my Compose Multiplatform projects")
        url.set("https://github.com/toasterofbread/composekit")
        inceptionYear.set("2023")

        licenses {
            license {
                name.set("GPL-3.0")
                url.set("https://www.gnu.org/licenses/gpl-3.0.en.html")
            }
        }
        developers {
            developer {
                id.set("toasterofbread")
                name.set("Talo Halton")
                email.set("talohalton@gmail.com")
                url.set("https://github.com/toasterofbread")
            }
        }
        scm {
            connection.set("https://github.com/toasterofbread/composekit.git")
            url.set("https://github.com/toasterofbread/composekit")
        }
        issueManagement {
            system.set("Github")
            url.set("https://github.com/toasterofbread/composekit/issues")
        }
    }
}
