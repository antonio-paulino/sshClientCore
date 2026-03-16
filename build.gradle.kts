plugins {
    `maven-publish`
    kotlin("jvm") version "2.3.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

group = "pt.paulinoo"
version = "0.0.1"

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.hierynomus:sshj:0.40.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
