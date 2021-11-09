import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `prints help`() {
        val output = script.run("--help")

        assertTrue(output.contains("-h, --help"))
        assertTrue(output.contains("Show this message and exit"))
    }

    @Test
    fun `doesn't change file contents in --dry-run mode`() {
        val gradleProperties = script.newFile("gradle.properties")
        gradleProperties.writeText(gradlePropertiesContent)
        val changelog = script.newFile("CHANGELOG.md")
        changelog.writeText(changelogContent)

        val output = script.run("--dry-run")

        assertEquals(gradlePropertiesContent, gradleProperties.readText())
        assertEquals(changelogContent, changelog.readText())
        assertTrue(
            output.endsWith(
                """
                    #0  OK Upload artifact
                    #1  OK Create and push release tag
                    #2  OK Increment version in gradle.properties
                    #3  OK Update release date in changelog
                    #4  OK Add new version to changelog
                    #5  OK Create commit and pull request
                """.trimIndent()
            )
        )
    }
}
