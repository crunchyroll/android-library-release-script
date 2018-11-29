import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ReleaseScriptTest {
    @get:Rule
    val fakeProject = TemporaryFolder()

    @Test
    fun `script is runnable and can print help`() {
        runScript("--help")
    }

    @Test
    fun `script is runnable in --dry-run mode`() {
        val a = fakeProject.newFile("gradle.properties").apply {
            writeText(
                """
            VERSION_NAME=1.0.1
            """.trimIndent()
            )
        }
        val b = fakeProject.newFile("CHANGELOG.md").apply {
            writeText(
                """
            # Changelog

            ## Version 1.0.1 *(In development)*

            - Another fake changelog entry

            ## Version 1.0.0 *(2018-11-28)*

            - Fake changelog entry
            """.trimIndent()
            )
        }
        fakeProject.newFile("gradlew")
        runScript("--dry-run")

        println(a.readText())
        println(b.readText())
    }

    fun runScript(vararg args: String) {
        fakeProject.newFile("Release.kt")
            .writeBytes(File("src/main/kotlin/Release.kt").readBytes())
        "chmod +x Release.kt".execute(fakeProject.root)

        println()
        println("Script output:")
        println()

        ("./Release.kt " + args.joinToString(separator = " ")).execute(fakeProject.root)
    }
}
