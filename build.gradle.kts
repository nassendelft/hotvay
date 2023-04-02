plugins {
    kotlin("multiplatform") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.0"
}

group = "me.nick"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    val targets = listOf(macosArm64(), macosX64())
    targets.forEach {
        it.binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
            }
        }
        val commonTest by getting
        val macosMain by creating {
            sourceSets { dependsOn(commonMain) }
            targets.forEach { it.compilations["main"].defaultSourceSet.dependsOn(this) }

            dependencies {
                implementation("nl.ncaj:core-foundation-extensions:0.3.0")
            }
        }
        val macosTest by creating {
            sourceSets { dependsOn(commonTest) }
            targets.forEach { it.compilations["test"].defaultSourceSet.dependsOn(this) }
        }
    }
}
