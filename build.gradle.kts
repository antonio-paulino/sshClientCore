import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `maven-publish`
    kotlin("jvm") version "2.3.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("com.gradleup.shadow") version "9.4.0"
}

group = "pt.paulinoo"
version = "0.0.1"

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    relocate("com.hierynomus", "pt.paulinoo.internal.sshj")
    relocate("kotlinx.coroutines", "pt.paulinoo.internal.coroutines")
    relocate("org.slf4j", "pt.paulinoo.internal.slf4j")

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.shadowJar)

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            pom.withXml {
                val dependenciesNode = asNode().get("dependencies") as? groovy.util.Node
                dependenciesNode?.parent()?.remove(dependenciesNode)
            }
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
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}