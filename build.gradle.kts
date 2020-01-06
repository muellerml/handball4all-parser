import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


buildscript {
    repositories {
        jcenter()
    }
}
repositories {
    // artifacts are published to JCenter
    jcenter()
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.60"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.3.60"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.60")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.14.0")
    implementation("io.ktor:ktor-client-okhttp:1.2.6")
    implementation("io.ktor:ktor-client-jackson:1.2.6")

    implementation("org.apache.pdfbox:pdfbox:2.0.18")

    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.3")
    testImplementation("io.mockk:mockk:1.9.3")
}
tasks {
    test {
        useJUnitPlatform()
    }

    withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
    }
}

