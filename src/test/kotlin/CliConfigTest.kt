import org.junit.Test

class CliConfigTest {
    @Test(expected = IllegalArgumentException::class)
    fun test() {
        CliConfig.parse(arrayOf("--no-tag-prefix", "--tag-prefix", "v"))
    }
}
