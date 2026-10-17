plugins {
    `kotlin-dsl`
    kotlin("jvm") version "2.2.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()

    maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
    maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
}

dependencies {
    fun plugin(id: String, version: String) = "$id:$id.gradle.plugin:$version"
    implementation("dev.kikugie:stonecutter:0.9.8")
}
