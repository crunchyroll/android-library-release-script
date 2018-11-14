import org.junit.Test

class ReleaseScriptTest {
    @Test
    fun `script is runnable and can print help`() {
        runScript(arrayOf("--help"))
    }

    fun runScript(args: Array<String>) = main(args)
}
