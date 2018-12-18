import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ChangelogTest {
    @get:Rule
    var tempFolder = TemporaryFolder()

    @Test
    fun `updates changelog with release date and new versions`() {
        val file = tempFolder.newFile("CHANGELOG.md").apply {
            writeText(
                """
                |# Change Log
                |
                |## Version 1.1.16 *(In development)*
                |- Add player settings related events
                |
                |## Version 1.1.15
                |- Add `issueId` field to `VideoProblemReportedEvent`
                """.trimMargin()
            )
        }

        val changelog = Changelog(path = file.path, filename = "changelog.md")

        changelog.setReleaseDate(version = "1.1.16", date = "2020-04-20")

        assertEquals(
            """
            |# Change Log
            |
            |## Version 1.1.16 *(2020-04-20)*
            |- Add player settings related events
            |
            |## Version 1.1.15
            |- Add `issueId` field to `VideoProblemReportedEvent`
            """.trimMargin(),
            changelog.file?.readText()?.trim()
        )

        changelog.addNewVersion("1.1.17")

        assertEquals(
            """
            |# Change Log
            |
            |
            |## Version 1.1.17 *(In development)*
            |
            |## Version 1.1.16 *(2020-04-20)*
            |- Add player settings related events
            |
            |## Version 1.1.15
            |- Add `issueId` field to `VideoProblemReportedEvent`
            """.trimMargin(),
            changelog.file?.readText()?.trim()
        )
    }

    @Test(expected = NonFatalException::class)
    fun `throws NonFatalException when setting release date if changelog file cannot be found`() {
        val changelog = Changelog(path = tempFolder.toString(), filename = "changelog.md")

        changelog.setReleaseDate(version = "1.1.16", date = "2020-04-20")
    }

    @Test(expected = NonFatalException::class)
    fun `throws NonFatalException when adding new version if changelog file cannot be found`() {
        val changelog = Changelog(path = tempFolder.toString(), filename = "changelog.md")

        changelog.addNewVersion(version = "1.1.17")
    }
}
