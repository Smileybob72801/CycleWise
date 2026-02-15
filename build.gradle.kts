import java.io.File
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import org.gradle.api.tasks.*
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}

abstract class ExportProjectAsMarkdown : DefaultTask() {

    // Use a simple string instead of a DirectoryProperty to avoid directory snapshotting
    @get:Input
    abstract var repositoryRootPath: String

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    var maxBytesPerFile: Long = 1_000_000L

    @TaskAction
    fun generate() {
        val repoRoot = File(repositoryRootPath)
        val out = outputFile.get().asFile
        out.parentFile.mkdirs()
        out.writeText("# Project Export\n\n_Generated: ${ZonedDateTime.now()}_\n", StandardCharsets.UTF_8)

        val files = sources.files
            .filter { it.isFile }
            .sortedBy { it.relativeTo(repoRoot).invariantSeparatorsPath }

        for (f in files) {
            val rel = f.relativeTo(repoRoot).invariantSeparatorsPath
            val lang = languageFor(f.name)
            val size = f.length()

            out.appendText("\n\n---\n\n## `${rel}`\n\n", StandardCharsets.UTF_8)
            if (size > maxBytesPerFile) {
                out.appendText("> Skipped (> ${maxBytesPerFile} bytes)\n", StandardCharsets.UTF_8)
                continue
            }
            val content = f.readText(StandardCharsets.UTF_8)
            if (lang.isNotEmpty()) {
                out.appendText("```$lang\n$content\n```\n", StandardCharsets.UTF_8)
            } else {
                out.appendText("```\n$content\n```\n", StandardCharsets.UTF_8)
            }
        }
    }

    private fun languageFor(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".kt") || lower.endsWith(".kts") -> "kotlin"
            lower.endsWith(".java") -> "java"
            lower.endsWith(".swift") -> "swift"
            lower.endsWith(".mm") || lower.endsWith(".m") || lower.endsWith(".h") -> "objectivec"
            lower.endsWith("androidmanifest.xml") || lower.endsWith(".xml") -> "xml"
            lower.endsWith(".gradle.kts") -> "kotlin"
            lower.endsWith(".gradle") -> "groovy"
            lower.endsWith(".proto") -> "proto"
            lower.endsWith(".aidl") -> "aidl"
            lower.endsWith(".json") -> "json"
            lower.endsWith(".yaml") || lower.endsWith(".yml") -> "yaml"
            lower.endsWith(".toml") -> "toml"
            lower.endsWith(".md") -> "markdown"
            lower.endsWith(".sh") -> "bash"
            lower.endsWith(".ps1") -> "powershell"
            else -> ""
        }
    }
}

tasks.register<ExportProjectAsMarkdown>("exportProjectAsMarkdown") {
    group = "export"
    description = "Concatenate project sources/resources into one Markdown for LLM upload"

    // Set a plain string for the repo root (no directory snapshot)
    repositoryRootPath = layout.projectDirectory.asFile.absolutePath

    val includes = listOf(
        "**/*.kt","**/*.kts","**/*.java","**/*.swift","**/*.m","**/*.mm","**/*.h",
        "**/*.gradle","**/*.gradle.kts","**/*.properties","**/*.pro","**/*.manifest","**/AndroidManifest.xml",
        "**/*.xml","**/*.json","**/*.yaml","**/*.yml","**/*.toml","**/*.proto","**/*.aidl",
        "**/*.md","**/*.txt","**/*.sh","**/*.bat","**/*.ps1","**/*.gitignore","**/*.gitattributes"
    )
    val excludes = listOf(
        "**/build/**","**/.gradle/**","**/.idea/**","**/.git/**","**/.DS_Store",
        "**/*.iml","**/*.keystore","**/*.jks",
        "**/*.png","**/*.jpg","**/*.jpeg","**/*.webp","**/*.gif","**/*.ico","**/*.pdf",
        "**/*.so","**/*.a","**/*.dylib","**/*.jar","**/*.klib","**/*.arr"
    )

    // Create the file tree at configuration time
    val tree = fileTree(layout.projectDirectory) {
        includes.forEach { include(it) }
        excludes.forEach { exclude(it) }
    }

    sources.from(tree)
    outputFile.set(layout.buildDirectory.file("exports/project-export.md"))

    maxBytesPerFile = 1_000_000L
}