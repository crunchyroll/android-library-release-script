import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GradlePropertiesTest {
    @get:Rule
    var tempFolder = TemporaryFolder()

    @Test
    fun `correctly parses version if = doesn't have whitespaces around it`() {
        val gradleProperties = createGradleProperties(initialVersion = "1.0.0")

        assertEquals("1.0.0", gradleProperties.versionToRelease)
    }

    @Test
    fun `correctly parses version if = has whitespaces around it`() {
        val gradleProperties = createGradleProperties(initialVersion = " 1.0.0")

        assertEquals("1.0.0", gradleProperties.versionToRelease)
    }

    @Test
    fun `patch version is incremented`() {
        val gradleProperties = createGradleProperties(initialVersion = "1.0.0")

        assertEquals("1.0.1", gradleProperties.newVersion)
    }

    @Test
    fun `writes incremented version to file`() {
        val file = tempFolder.newFile("gradle.properties").apply {
            writeText(
                """
                |VERSION_NAME=1.0.0
                |GROUP=com.ellation
                |ARTIFACT_ID=some-library
                """.trimMargin()
            )
        }
        val gradleProperties = GradleProperties(path = file.path, versionNameKey = "VERSION_NAME")

        gradleProperties.incrementVersion()

        assertEquals(
            """
            |VERSION_NAME=1.0.1
            |GROUP=com.ellation
            |ARTIFACT_ID=some-library
            |
            """.trimMargin(),
            file.readText()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws if there's no VERSION_NAME property`() {
        val file = tempFolder.newFile("gradle.properties").apply {
            writeText(
                """
                |GROUP=com.ellation
                |ARTIFACT_ID=some-library
                """.trimMargin()
            )
        }

        GradleProperties(path = file.path, versionNameKey = "VERSION_NAME")
    }

    private fun createGradleProperties(initialVersion: String): GradleProperties {
        val file = tempFolder.newFile("gradle.properties").apply {
            writeText(
                """
                |VERSION_NAME=$initialVersion
                |GROUP=com.ellation
                |ARTIFACT_ID=some-library
                """.trimMargin()
            )
        }
        return GradleProperties(path = file.path, versionNameKey = "VERSION_NAME")
    }
}
