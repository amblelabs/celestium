plugins {
    `multiloader-loader`
    id("dev.kikugie.loom-back-compat")
    kotlin("jvm") version "2.4.0"
//    id("com.google.devtools.ksp") version "2.4.12" // TODO(template): review
//    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.14" // TODO(template): review
}

stonecutter {

}

dependencies {
    minecraft("com.mojang:minecraft:${commonMod.mc}")

    commonMod.depOrNull("parchment")?.let { parchmentVersion ->
        mappings(loom.layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-${commonMod.mc}:$parchmentVersion@zip")
        })
    }

    modImplementation("net.fabricmc:fabric-loader:${commonMod.dep("fabric_loader")}")
    modApi("net.fabricmc.fabric-api:fabric-api:${commonMod.dep("fabric_api")}+${commonMod.mc}")

    modImplementation("net.fabricmc:fabric-language-kotlin:${commonMod.dep("fabric_language_kotlin")}")

    // Required dependencies
    modImplementation("com.terraformersmc:modmenu:${commonMod.dep("modmenu")}")
}

loom {
    runs {
        getByName("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
        }
        getByName("server") {
            server()
            configName = "Fabric Server"
            ideConfigGenerated(true)
        }
    }
}