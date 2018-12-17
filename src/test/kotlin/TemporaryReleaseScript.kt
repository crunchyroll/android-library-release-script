import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Creates a temporary executable copy of the Release.kt file.
 */
class TemporaryReleaseScript : TemporaryFolder() {
    fun run(vararg args: String): String {
        return ("./Release.kt " + args.joinToString(separator = " ")).output(root)
    }

    override fun create() {
        super.create()
        newFile("Release.kt").writeBytes(File("src/main/kotlin/Release.kt").readBytes())
        "chmod +x Release.kt".execute(root)
    }
}
