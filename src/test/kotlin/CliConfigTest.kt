import org.junit.Test

class CliConfigTest {
    @Test(expected = IllegalArgumentException::class)
    fun `--no-tag-prefix flag and --tag-prefix argument cannot be used together`() {
        ReleaseCommand {}.main(arrayOf("--no-tag-prefix", "--tag-prefix", "v"))
    }
}
