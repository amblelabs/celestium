plugins {
    kotlin("jvm")
    `multiloader-loader`
    id("net.neoforged.moddev") version "2.0.148"
}

neoForge {
    enable {
        version = commonMod.dep("neoforge")
    }
}

dependencies {
}

neoForge {
    runs {
        register("client") {
            client()
            ideName = "NeoForge Client (${project.path})"
        }

        register("server") {
            server()
            ideName = "NeoForge Server (${project.path})"
        }
    }

    parchment {
        commonMod.depOrNull("parchment")?.let {
            mappingsVersion = it
            minecraftVersion = commonMod.mc
        }
    }

    mods {
        register(commonMod.id) {
            sourceSet(sourceSets.main.get())
        }
    }
}

sourceSets.main {
    resources.srcDir("src/generated/resources")
}