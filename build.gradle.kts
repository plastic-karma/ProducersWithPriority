plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.3.10"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jlleitschuh.gradle.ktlint") version "14.1.0"
    id("org.jetbrains.kotlinx.kover") version "0.9.7"
    `maven-publish`
}

repositories {
    mavenCentral()
}

group = "com.plastickarma"
version = "0.0.1"

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }
    js {
        nodejs {
            testTask {
                useMocha {
                    timeout = "15000"
                }
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
            }
        }
        jvmTest {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}

kover {
    reports {
        verify {
            rule {
                minBound(90)
            }
        }
    }
}

detekt {
    config.setFrom(files("config/detekt.yml"))
    source.setFrom(files("src/commonMain/kotlin"))
}

tasks.assemble {
    dependsOn(tasks.check)
}

tasks.named("kotlinNpmInstall") {
    doFirst {
        val npmrc =
            layout.buildDirectory
                .file("js/.npmrc")
                .get()
                .asFile
        npmrc.parentFile.mkdirs()
        npmrc.writeText("registry=https://registry.npmjs.org/\n")
    }
}
