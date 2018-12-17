import org.junit.Rule
import org.junit.Test

class ReleaseScriptTest {
    @get:Rule
    val script = TemporaryReleaseScript()

    @Test
    fun `script is runnable in --dry-run mode`() {
        val a = script.newFile("gradle.properties").apply {
            writeText(
                """
            VERSION_NAME=1.0.1
            """.trimIndent()
            )
        }
        val b = script.newFile("CHANGELOG.md").apply {
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
        script.newFile("gradlew")
        script.run("--dry-run")

        println(a.readText())
        println(b.readText())
    }
}
