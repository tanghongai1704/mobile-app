import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

// Plugin versions and architectural dependency checks are shared at the root.
plugins {
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

abstract class VerifyArchitectureTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val architectureSources: ConfigurableFileCollection

    @get:Input
    abstract val workspacePath: Property<String>

    @TaskAction
    fun verify() {
        val root = File(workspacePath.get())
        val violations = mutableListOf<String>()
        val imports = Regex("(?m)^import\\s+([\\w.]+)")
        val dependencies = Regex("""project\("(:[^"]+)"\)""")
        var checked = 0

        architectureSources.files.sortedBy { it.path }.forEach { file ->
            val relative = file.relativeTo(root).invariantSeparatorsPath
            val parts = relative.split("/")
            val area = parts.first()
            val module = if (area in setOf("feature", "data", "core")) parts[1] else area
            val text = file.readText()
            checked++
            fun reject(reason: String) { violations += "$relative: $reason" }
            fun forbiddenTarget(target: String): Boolean = when (area) {
                "domain" -> true
                "feature", "data", "core" ->
                    target == ":app" || target.startsWith(":feature:") || target.startsWith(":data:")
                else -> false
            }
            if (file.name == "build.gradle.kts") {
                dependencies.findAll(text).forEach {
                    val target = it.groupValues[1]
                    if (forbiddenTarget(target)) reject("forbidden module dependency $target")
                }
                if (area == "domain" && Regex("plugins.android|com\\.android").containsMatchIn(text)) {
                    reject("domain must remain a Kotlin/JVM module")
                }
            } else if (file.extension == "kt") {
                imports.findAll(text).map { it.groupValues[1] }.forEach { imported ->
                    when {
                        area == "domain" && Regex("^(android\\.|androidx\\.|dagger\\.|com\\.example\\.onespace\\.(app|core|data|feature)\\.)").containsMatchIn(imported) ->
                            reject("domain imports framework/implementation $imported")
                        area in setOf("feature", "data", "core") && imported.startsWith("com.example.onespace.app.") ->
                            reject("library imports app shell $imported")
                        area in setOf("feature", "core") && imported.startsWith("com.example.onespace.data.") ->
                            reject("presentation/core imports a data implementation $imported")
                        area in setOf("data", "core") && imported.startsWith("com.example.onespace.feature.") ->
                            reject("data/core imports a feature $imported")
                        area == "feature" && imported.startsWith("com.example.onespace.feature.") &&
                            imported.removePrefix("com.example.onespace.feature.").substringBefore(".") != module ->
                            reject("feature imports another feature $imported")
                    }
                }
                if (area == "feature") {
                    val code = text.replace(Regex("(?s)/\\*.*?\\*/"), "")
                        .replace(Regex("(?m)//.*$"), "")
                    if (Regex("\\b(NavController|NavHostController)\\b").containsMatchIn(code)) {
                        reject("feature must expose navigation callbacks, not a controller")
                    }
                }
                if (relative.startsWith("app/src/main/") &&
                    Regex("/com/example/onespace/(domain|data|feature|core)/").containsMatchIn(relative)) {
                    reject("business/feature/core sources must live outside app")
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException("Architecture violations:\n" + violations.joinToString("\n"))
        }
        logger.lifecycle("Architecture verified: $checked source/build files; no forbidden dependencies or feature controllers.")
    }
}

tasks.register<VerifyArchitectureTask>("verifyArchitecture") {
    group = "verification"
    description = "Checks the module/layer and navigation boundaries agreed in Section 4."
    workspacePath.set(layout.projectDirectory.asFile.absolutePath)
    architectureSources.from(fileTree(rootDir) {
        include("domain/build.gradle.kts", "domain/src/main/**/*.kt")
        include("app/src/main/**/*.kt")
        include("feature/*/build.gradle.kts", "feature/*/src/main/**/*.kt")
        include("data/*/build.gradle.kts", "data/*/src/main/**/*.kt")
        include("core/*/build.gradle.kts", "core/*/src/main/**/*.kt")
        exclude("**/build/**")
    })
}
