plugins {
    id("java")
    id("idea")
    id("java-library")
}

object CI {
    const val REFS_PREFIX = "refs/heads/"

    fun branch(): String? {
        val githubRef = System.getenv("GITHUB_REF")
        if (githubRef != null && githubRef.startsWith(REFS_PREFIX)) {
            return githubRef.substring(REFS_PREFIX.length).replace("/", "-")
        }

        val gitBranch = System.getenv("GIT_BRANCH")
        return gitBranch?.replace("/", "-")
    }

    fun buildNum(): String? {
        val githubRunNumber = System.getenv("GITHUB_RUN_NUMBER")
        if (githubRunNumber != null) return githubRunNumber

        val gitCommit = System.getenv("GIT_COMMIT")
        return if (gitCommit != null && gitCommit.length >= 8) gitCommit.substring(0, 8) else gitCommit
    }
}

object Versioning {
    const val BRANCH_RELEASE = "release"
    const val BUILD_RELEASE = "release"
    const val BUILD_LOCAL = "local"
    const val BUILD = "dev"

    fun getBranch(): String? {
        val branch = CI.branch()
        return branch?.takeIf {
            it != "main" && it != "master"
                    && it != "trunk" && it != "dev"
                    && it != BRANCH_RELEASE
        }
    }

    fun getBuild(): String? {
        val branch = CI.branch()
        if (branch == BRANCH_RELEASE) return null
        return CI.buildNum()
    }

    fun getQualifier(): String {
        val branch = CI.branch() ?: return BUILD_LOCAL
        return if (branch == BRANCH_RELEASE) BUILD_RELEASE else BUILD
    }
}

fun createVer(): String {
    var qualifier = ""

    Versioning.getBranch()?.let { branch ->
        qualifier += "$branch-"
    }

    qualifier += Versioning.getQualifier()

    Versioning.getBuild()?.let { build ->
        qualifier += ".$build"
    }

    return "$loader-${commonMod.version}-$qualifier+mc${stonecutterBuild.current.version}"
}

version = createVer()

base {
    archivesName = commonMod.id
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(commonProject.prop("java.version")!!)
    // withSourcesJar()
    // withJavadocJar()
}

// TODO(template): review
repositories {
    mavenCentral()

    exclusiveContent {
        forRepository {
            maven("https://repo.spongepowered.org/repository/maven-public") { name = "Sponge" }
        }
        filter { includeGroupAndSubgroups("org.spongepowered") }
    }

    exclusiveContent {
        forRepositories(
            maven("https://maven.parchmentmc.org") { name = "ParchmentMC" },
            maven("https://maven.neoforged.net/releases") { name = "NeoForge" },
            maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        )
        filter { includeGroup("org.parchmentmc.data") }
    }

    maven("https://maven.terraformersmc.com/releases/") { name = "TerraformersMC" }
    maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
    maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    strictMaven("https://thedarkcolour.github.io/KotlinForForge/", "KotlinForForge", "thedarkcolour")
}

tasks.processResources {
    dependsOn(":common:${commonMod.propOrNull("minecraft_version")}:stonecutterGenerate")

    val expandProps = mapOf(
        "javaVersion" to commonMod.propOrNull("java.version"),
        "modId" to commonMod.id,
        "modName" to commonMod.name,
        "modVersion" to commonMod.version,
        "modGroup" to commonMod.group,
        "modAuthor" to commonMod.author,
        "modDescription" to commonMod.description,
        "modLicense" to commonMod.license,
        "modGitHub" to commonMod.github,
        "minecraftVersion" to commonMod.propOrNull("minecraft_version"),
        "minMinecraftVersion" to commonMod.propOrNull("min_minecraft_version"),
        "fabricLoaderVersion" to commonMod.depOrNull("fabric_loader"),
        "fabricApiVersion" to commonMod.depOrNull("fabric_api"),
        "neoForgeVersion" to commonMod.depOrNull("neoforge"),
        "forgeVersion" to commonMod.depOrNull("forge"),
        "modMenuVersion" to commonMod.depOrNull("modmenu")
    ).filterValues { it?.isNotEmpty() == true }.mapValues { (_, v) -> v!! }

    val jsonExpandProps = expandProps.mapValues { (_, v) -> v.replace("\n", "\\\\n") }

    filesMatching(listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml")) {
        expand(expandProps)
    }

    filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "*.mixins.json")) {
        expand(jsonExpandProps)
    }

    inputs.properties(expandProps)
}