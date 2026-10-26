val isCi = System.getenv("CI") == "true"
// TODO(template): remove
//gradle.startParameter.isParallelProjectExecutionEnabled = !isCi
//gradle.startParameter.isBuildCacheEnabled = !isCi
//gradle.startParameter.isConfigureOnDemand = !isCi

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.minecraftforge.net")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.kikugie.dev/releases")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("dev.kikugie.loom-back-compat") version "0.4.3"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" // TODO(template): review
}

gradle.beforeProject {
    buildscript.repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
    }
}

val commonVersions = providers.gradleProperty("stonecutter_enabled_common_versions").orNull?.split(",")?.map { it.trim() } ?: emptyList()
val fabricVersions = providers.gradleProperty("stonecutter_enabled_fabric_versions").orNull?.split(",")?.map { it.trim() } ?: emptyList()
//val forgeVersions = providers.gradleProperty("stonecutter_enabled_forge_versions").orNull?.split(",")?.map { it.trim() } ?: emptyList()
val neoforgeVersions = providers.gradleProperty("stonecutter_enabled_neoforge_versions").orNull?.split(",")?.map { it.trim() } ?: emptyList()
val dists = mapOf(
    "common" to commonVersions,
//    "forge" to forgeVersions,
    "fabric" to fabricVersions,
    "neoforge" to neoforgeVersions
)
val uniqueVersions = dists.values.flatten().distinct()

stonecutter {
    centralScript = "build.gradle.kts"
    kotlinController

    create(rootProject) {
        versions(*uniqueVersions.toTypedArray())
        vcsVersion = "26.3"

        dists.forEach { (branchName, branchVersions) ->
            branch(branchName) {
                versions(*branchVersions.toTypedArray())
            }
        }
    }
}

rootProject.name = "celestium"
