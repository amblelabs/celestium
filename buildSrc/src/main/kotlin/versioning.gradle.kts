// buildSrc/src/main/kotlin/versioning.gradle.kts

// --- CI Logic ---
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
    const val BRANCH_MAIN = "main"
    const val BRANCH_RELEASE = "release"
    const val BUILD_RELEASE = "release"
    const val BUILD_LOCAL = "local"
    const val BUILD = "dev"

    fun getBranch(): String? {
        val branch = CI.branch()
        return branch?.takeIf { it != BRANCH_MAIN && it != BRANCH_RELEASE }
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

// --- Version Generator ---
fun Project.createVer(includeBuild: Boolean): String {
    var qualifier = ""

    Versioning.getBranch()?.let { branch ->
        qualifier += "$branch-"
    }

    qualifier += Versioning.getQualifier()

    if (includeBuild) {
        Versioning.getBuild()?.let { build ->
            qualifier += ".$build"
        }
    }

    // Safely look up properties from active subproject context
    val modVer = project.findProperty("mod_version") ?: "1.0.0"
    val mcVer = project.findProperty("minecraft_version") ?: "unknown"

    return "$modVer-$qualifier+mc.$mcVer"
}

fun Project.getPublicVersion(): String = createVer(true)
fun Project.getArtifactVersion(): String = createVer(false)
