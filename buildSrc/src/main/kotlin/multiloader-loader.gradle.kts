import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("idea")
    id("multiloader-common")
}

val commonJava = configurations.register("commonJava") {
    isCanBeResolved = true
}

val commonKotlin = configurations.register("commonKotlin") {
    isCanBeResolved = true
}

val commonResources = configurations.register("commonResources") {
    isCanBeResolved = true
}

dependencies {
    val commonPath = common.hierarchy.toString()
    compileOnly(project(path = commonPath))
    commonJava(project(path = commonPath, configuration = "commonJava"))
    commonKotlin(project(path = commonPath, configuration = "commonKotlin"))
    commonResources(project(path = commonPath, configuration = "commonResources"))
}

tasks {
    compileJava {
        dependsOn(commonJava)
        source(commonJava)
    }

    tasks.named<KotlinCompile>("compileKotlin") {
        dependsOn(commonKotlin)
        source(commonKotlin)
    }

    processResources {
        dependsOn(commonResources)
        from(commonResources)
    }
}
