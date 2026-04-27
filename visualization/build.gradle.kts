plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.3.21"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jlleitschuh.gradle.ktlint") version "14.1.0"
}

repositories {
    mavenCentral()
}

kotlin {
    js {
        browser {
            commonWebpackConfig {
                outputFileName = "visualization.js"
            }
        }
        binaries.executable()
    }
    sourceSets {
        jsMain {
            dependencies {
                implementation(project(":"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            }
        }
    }
}

detekt {
    config.setFrom(rootProject.files("config/detekt.yml"))
    source.setFrom(files("src/jsMain/kotlin"))
}
