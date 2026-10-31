plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidx.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm("android") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            // Ktor HTTP multiplatform
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Coil 3 imagini multiplatform
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // Multiplatform settings (favorite)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
        }

        getByName("androidMain").dependencies {
            implementation(compose.preview)
            implementation(libs.kotlinx.coroutines.android)

            // Ktor engine Android (OkHttp)
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            // Ktor engine iOS (Darwin / NSURLSession)
            implementation(libs.ktor.client.darwin)
        }
    }
}
