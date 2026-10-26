import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("idea")
    id("multiloader-common")
}

val commonJava: Configuration by configurations.creating {
    isCanBeResolved = true
}
val commonKotlin: Configuration by configurations.creating {
    isCanBeResolved = true
}
val commonResources: Configuration by configurations.creating {
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
