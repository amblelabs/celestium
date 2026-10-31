@file:Suppress("UnstableApiUsage")

plugins {
    kotlin("jvm")
    `multiloader-common`
    id("dev.kikugie.loom-back-compat")
}

stonecutter {

}

loom {
    mixin {
        useLegacyMixinAp = false
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${commonMod.mc}")

    commonMod.depOrNull("parchment")?.let { parchmentVersion ->
        mappings(loom.layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-${commonMod.mc}:$parchmentVersion@zip")
        })
    }

    compileOnly("org.spongepowered:mixin:0.8.5")
    modCompileOnly("net.fabricmc:fabric-loader:${commonMod.dep("fabric_loader")}")
}

// TODO(template): same as buildSrc/.../multiloader-loader.gradle.kts
val commonJava = configurations.register("commonJava") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val commonKotlin = configurations.register("commonKotlin") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val commonResources = configurations.register("commonResources") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

artifacts {
    afterEvaluate {
        val mainSourceSet = sourceSets.main.get()
        mainSourceSet.kotlin.sourceDirectories.files.forEach {
            add(commonKotlin.name, it)
        }
        mainSourceSet.java.sourceDirectories.files.forEach {
            add(commonJava.name, it)
        }
        mainSourceSet.resources.sourceDirectories.files.forEach {
            add(commonResources.name, it)
        }
    }
}
