import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private val changelogContent = """
                            # Changelog

                            ## Version 1.0.1 *(In development)*

                            - Another fake changelog entry

                            ## Version 1.0.0 *(2018-11-28)*

                            - Fake changelog entry
                            """.trimIndent()

private val gradlePropertiesContent = """
                            VERSION_NAME=1.0.1
                            """.trimIndent()

class ReleaseScriptTest {
    @get:Rule
    val script = TemporaryReleaseScript()

    @Test
    fun `doesn't change file contents in --dry-run mode`() {
        val gradleProperties = script.newFile("gradle.properties")
        gradleProperties.writeText(gradlePropertiesContent)
        val changelog = script.newFile("CHANGELOG.md")
        changelog.writeText(changelogContent)

        script.run("--dry-run")

        assertEquals(gradlePropertiesContent, gradleProperties.readText())
        assertEquals(changelogContent, changelog.readText())
    }
}
