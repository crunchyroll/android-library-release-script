import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class StepsTest {
    @Test
    fun `doesn't throw if actions don't throw`() {
        steps {
            step("step #1") {}
            step("step #2") {}
        }
    }

    @Test(expected = IOException::class)
    fun `throws in 2nd step and doesn't execute 3rd step`() {
        steps {
            step("step #1") {}
            step("step #2") { throw IOException() }
            step("step #3") { fail("This step shouldn't be executed") }
        }
    }

    @Test
    fun `doesn't rethrow NonFatalException`() {
        try {
            steps {
                step("step #1") {}
                step("step #2") { throw NonFatalException() }
                step("step #3") {}
            }
        } catch (t: NonFatalException) {
            fail("NonFatalException should be rethrown")
        }
    }
}
