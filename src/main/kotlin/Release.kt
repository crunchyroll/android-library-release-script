#!/usr/bin/env kscript

@file:DependsOn("com.xenomachina:kotlin-argparser:2.0.7")

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

var DRY_RUN = false

fun main(args: Array<String>) = steps {
    val config = CliConfig.parse(args)
    println("ticket = ${config.ticket}")

    DRY_RUN = config.dryRun

    step("Upload artifact") {
        val gradle = GradleW()
        gradle.uploadArchives()
    }

    val tagPrefix = if (config.noTagPrefix) "" else config.tagPrefix
    val git = Git(tagPrefix = tagPrefix)

    val gradleProperties = GradleProperties(path = ".", versionNameKey = "VERSION_NAME")
    step("Create and push release tag") {
        git.createReleaseTag(gradleProperties.versionToRelease)
    }

    step("Increment version in gradle.properties") {
        gradleProperties.incrementVersion()
    }

    val changelog = Changelog(path = ".", filename = config.changelog)

    step("Update release date in changelog") {
        changelog.setReleaseDate(gradleProperties.versionToRelease, today())
    }
    step("Add new version to changelog") {
        changelog.addNewVersion(gradleProperties.newVersion)
    }

    step("Create commit and pull request") {
        git.createPullRequestForNewVersion(
            gradleProperties.newVersion,
            config.ticket,
            listOf(gradleProperties.file, changelog.file)
        )
    }
}

fun steps(action: Steps.() -> Unit) {
    val steps = Steps()
    steps.action()
    steps.run()
}

class Steps {
    private val steps = mutableListOf<Step>()

    fun step(description: String, action: () -> Unit) {
        steps.add(Step(description, action))
    }

    fun run() {
        var theFailure: Throwable? = null
        for ((index, step) in steps.withIndex()) {
            try {
                println()
                println(">> Running step #$index '${step.description}'")
                step.action()
                step.success = true
            } catch (t: Throwable) {
                if (t is NonFatalException) {
                    println("Non-fatal exception: ${t.message}")
                } else {
                    theFailure = t
                    break
                }
            }
        }

        println()
        steps.forEachIndexed { index, step ->
            println("#$index ${if (step.success) " OK" else "NOK"} ${step.description}")
        }
        theFailure?.let { throw it }
    }

    class Step(val description: String, val action: () -> Unit) {
        var success: Boolean = false
    }
}

class CliConfig(parser: ArgParser) {
    companion object {
        fun parse(args: Array<String>) = mainBody {
            ArgParser(args).parseInto(::CliConfig)
        }
    }

    val dryRun by parser.flagging(
        "--dry-run",
        help = "Run the script with all actions disabled. " +
                "Use this to understand which actions would have been executed."
    )

    val ticket by parser.storing(
        "--ticket",
        help = "Ticket number to use when creating the version bump PR. Default is GUNDROID-88."
    ).default("GUNDROID-88")

    val changelog by parser.storing(
        "--changelog",
        help = "Changelog file to modify"
    ).default("changelog.md")

    val tagPrefix by parser.storing(
        "--tag-prefix",
        help = "Version prefix to use for tag creation"
    ).default("v").addValidator {
        if (noTagPrefix) {
            throw IllegalArgumentException(
                "The --tag-prefix and --no-tag-prefix can't be used together"
            )
        }
    }

    val noTagPrefix by parser.flagging(
        "--no-tag-prefix",
        help = "Do not add any prefix to the version tag"
    )
}

fun today() = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

fun File.replaceLines(modifiedLines: (List<String>) -> List<String>) {
    val newChangelog = modifiedLines(readLines())
    printWriter().use { writer ->
        newChangelog.forEach(writer::println)
    }
}

class Preconditions(private val workingDir: String) {
    private val preconditions: List<Pair<String, () -> Boolean>> = listOf(
        "Cannot locate gradlew executable"
                to { File(workingDir, "gradlew").exists() },
        "hub is not installed, run 'brew install hub'"
                to { "hub --version".execute()?.exitValue() == 0 },
        "mvn is not installed, run 'brew install maven'"
                to { "mvn --version".execute()?.exitValue() == 0 }
    )

    fun verify() {
        preconditions.forEach { precondition ->
            assert(precondition.second()) { precondition.first }
        }
    }
}

class Changelog(
    private val path: String,
    private val filename: String
) {
    val file = findChangelogFile()

    fun setReleaseDate(version: String, date: String) {
        file ?: throw NonFatalException("Could not find '$filename' changelog file")

        file.replaceLines { lines ->
            lines.map { line ->
                when {
                    line.contains("$version *(In development)*") ->
                        line.replace("In development", date).also {
                            println("Set date $date for version $version in $filename changelog file")
                        }
                    else -> line
                }
            }
        }
    }

    fun addNewVersion(version: String) {
        file ?: throw NonFatalException("Could not find $filename")
        file.replaceLines { lines ->
            lines.toMutableList().apply {
                add(2, "")
                add(2, "## Version $version *(In development)*")
                add(2, "")
            }
        }
        println("Added version $version to $filename changelog file")
    }

    private fun findChangelogFile(): File? = findFile(workingDir = path, filename = filename)
}

class GradleProperties(
    private val path: String,
    private val versionNameKey: String
) {
    val file = findGradlePropertiesFile() ?: throw IllegalArgumentException(
        "Couldn't find a gradle.properties file that contains $versionNameKey"
    )

    private val lines = file.readLines()
    private val versionLine: String = lines.find { line -> line.contains(versionNameKey) }
        ?: throw IllegalArgumentException("Couldn't find $versionNameKey in $file")

    val versionToRelease: String = versionLine.split("=").last().trim()
    val newVersion: String = versionToRelease.increment()

    fun incrementVersion() {
        println("Set version to $newVersion")
        writeVersion(newVersion)
    }

    private fun writeVersion(version: String) {
        file.replaceLines { lines ->
            lines.map { line ->
                when {
                    line.contains(versionNameKey) -> "$versionNameKey=$version"
                    else -> line
                }
            }
        }
    }

    private fun findGradlePropertiesFile(): File? {
        return findFile(workingDir = path, filename = "gradle.properties") {
            it.readText().contains(versionNameKey)
        }
    }

    private fun String.increment(): String {
        val delimiter = "."
        val split = this.split(delimiter)
        val oldPatch = split.last().toInt()
        val newPatch = oldPatch + 1
        return (split.dropLast(1) + newPatch).joinToString(separator = delimiter)
    }
}

class GradleW {
    fun uploadArchives() {
        "./gradlew uploadArchives".execute()
    }
}

class Git(
    private val path: String = ".",
    private val remote: String = "origin",
    private val tagPrefix: String
) {
    fun createReleaseTag(version: String) {
        val repo = File(path)

        val tag = "$tagPrefix$version"
        "git tag $tag".execute(repo)
        "git push $remote $tag".execute(repo)
    }

    fun createPullRequestForNewVersion(version: String, ticket: String, files: List<File?>) {
        val name = "$ticket-increment-version-to-$version"
        "git checkout -b $name".execute()
        addFilesToIndex(files)
        createCommit(version, ticket)
        "git push origin $name".execute()
        createPullRequest(version, ticket)
        "git checkout master".execute()
    }

    private fun createCommit(version: String, ticket: String) {
        listOf(
            "git", "commit",
            "-m", "Prepare next version $version",
            "-m", "Jira: $ticket"
        ).execute()
    }

    private fun addFilesToIndex(files: List<File?>) {
        "git add ${files.filterNotNull().joinToString(separator = " ")}".execute()
    }

    private fun createPullRequest(version: String, ticket: String) {
        listOf(
            "hub", "pull-request",
            "-m", """Prepare next version $version
                    |
                    |Jira: [$ticket](https://ellation.atlassian.net/browse/$ticket)
                    |
                    |This PR was created [automatically](https://github.com/crunchyroll/android-library-release-script).""".trimMargin()
        ).execute()
    }
}

class NonFatalException(message: String? = "", cause: Throwable? = null) :
    RuntimeException(message, cause)

fun findFile(
    workingDir: String = ".",
    filename: String,
    predicate: (File) -> Boolean = { true }
): File? {
    val realFile = File(workingDir)
        .walkTopDown()
        .filter { file -> file.name.equals(filename, ignoreCase = true) }
        .find(predicate)
    return if (DRY_RUN) realFile?.createTempCopy() else realFile
}

private fun File.createTempCopy(): File = File.createTempFile("ALRS", null).also { temp ->
    temp.writeBytes(this.readBytes())
    temp.deleteOnExit()
}

fun String.execute(workingDir: File = File(".")): Process? {
    return split(" ").execute(workingDir)
}

fun List<String>.execute(workingDir: File = File(".")): Process? {
    println("$ $this")
    if (DRY_RUN) {
        return null
    }
    val process = ProcessBuilder(this)
        .directory(workingDir)
        .redirectInput(ProcessBuilder.Redirect.INHERIT)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
    process.waitFor()
    val exitValue = process.exitValue()
    if (exitValue != 0) {
        throw IllegalStateException("Command $this has exited with non-zero value $exitValue.")
    }
    return process
}
