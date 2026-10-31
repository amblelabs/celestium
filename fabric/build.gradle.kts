plugins {
    kotlin("jvm")
    `multiloader-loader`
    id("dev.kikugie.loom-back-compat")
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

            displayName = "Fabric Client"
            generateRunConfig = true
        }

        getByName("server") {
            server()

            displayName = "Fabric Server"
            generateRunConfig = true
        }
    }
}