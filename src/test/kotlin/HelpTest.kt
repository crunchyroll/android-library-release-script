import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HelpTest {
    @get:Rule
    val script = TemporaryReleaseScript()

    @Test
    fun `prints help`() {
        val output = script.run("--help")

        assertTrue(output.contains("-h, --help"))
        assertTrue(output.contains("show this help message and exit"))
    }
}
