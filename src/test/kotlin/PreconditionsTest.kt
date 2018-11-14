import org.junit.Test

class PreconditionsTest {
    @Test
    fun `preconditions don't fail`() {
        Preconditions(".").verify()
    }
}
