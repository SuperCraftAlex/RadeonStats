plugins {
    kotlin("jvm") version "2.0.0"
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "me.alex_s168"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("com.googlecode.lanterna:lanterna:3.2.0-alpha1")
    implementation("com.google.code.gson:gson:2.10.1")

    val jnaVersion = "5.14.0"
    // https://mvnrepository.com/artifact/net.java.dev.jna/jna
    implementation("net.java.dev.jna:jna:$jnaVersion")
    // https://mvnrepository.com/artifact/net.java.dev.jna/jna-platform
    implementation("net.java.dev.jna:jna-platform:$jnaVersion")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("MainKt")
}
