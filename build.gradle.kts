plugins {
    `maven-publish`
    kotlin("jvm") version "2.3.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("com.gradleup.shadow") version "9.4.1"
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
    api("com.hierynomus:sshj:0.40.0")
    api("com.github.mwiede:jsch:2.28.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.slf4j:slf4j-api:2.0.17")
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.17")
}

sourceSets {
    create("examples") {
        kotlin.srcDir("src/examples/kotlin")
        resources.srcDir("src/examples/resources")
        compileClasspath += sourceSets["main"].output + configurations["runtimeClasspath"]
        runtimeClasspath += output + compileClasspath
    }
}

dependencies {
    add("examplesImplementation", "org.jline:jline:3.26.3")
    add("examplesImplementation", "org.slf4j:slf4j-simple:2.0.17")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()

    val verboseTestOutput = (System.getProperty("sshClientCore.test.verbose") ?: "false").toBoolean()
    val logLevel = System.getProperty("sshClientCore.test.logLevel") ?: "off"

    testLogging {
        events("failed", "skipped")
        if (verboseTestOutput) {
            events("passed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = true
        } else {
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
            showStandardStreams = false
        }
    }

    systemProperty("org.slf4j.simpleLogger.defaultLogLevel", logLevel)
    systemProperty("org.slf4j.simpleLogger.showDateTime", "true")
    systemProperty("org.slf4j.simpleLogger.showThreadName", "true")
    systemProperty("org.slf4j.simpleLogger.showLogName", "true")
}

tasks.shadowJar {
    archiveClassifier.set("uber")
    // Drop signature metadata that becomes invalid once dependencies are repackaged.
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

tasks.register<Test>("testVerbose") {
    description = "Run tests with verbose output and debug logs"
    useJUnitPlatform()

    testLogging {
        events("passed", "failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }

    systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
    systemProperty("org.slf4j.simpleLogger.showDateTime", "true")
    systemProperty("org.slf4j.simpleLogger.showThreadName", "true")
    systemProperty("org.slf4j.simpleLogger.showLogName", "true")
}

tasks.register<JavaExec>("runTerminal") {
    group = "application"
    description = "Run the interactive sshClientCore terminal"
    classpath = sourceSets["examples"].runtimeClasspath
    mainClass.set("pt.paulinoo.sshClientCore.TerminalExampleKt")
    standardInput = System.`in`
}

tasks.register("writeRuntimeClasspath") {
    group = "application"
    description = "Writes examples runtime classpath to build/runtime-classpath.txt"
    dependsOn("examplesClasses")
    doLast {
        val outFile =
            layout.buildDirectory
                .file("runtime-classpath.txt")
                .get()
                .asFile
        outFile.parentFile.mkdirs()
        outFile.writeText(sourceSets["examples"].runtimeClasspath.asPath)
    }
}
