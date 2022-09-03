plugins {
    kotlin("multiplatform") version "1.7.20-Beta"
    kotlin("plugin.serialization") version "1.6.21"
}

group = "me.nick"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        hostOs.startsWith("Windows") -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
            }
        }

        compilations["main"].cinterops {
            val hidInterop by creating {
                defFile("src/nativeInterop/hidapi/hidapi.def")
            }
            val usbInterop by creating {
                defFile("src/nativeInterop/libusb/libusb.def")
            }
        }
    }
    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
            }
        }
        val nativeTest by getting
    }
}
