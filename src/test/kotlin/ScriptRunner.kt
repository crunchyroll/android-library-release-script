import java.io.File
import java.util.concurrent.TimeUnit

fun String.output(workingDir: File = File(".")): String {
    println("Running command: $this")
    val process = ProcessBuilder(split(" "))
        .directory(workingDir)
        .start()
    process.waitFor(60, TimeUnit.SECONDS)
    val exitValue = process.exitValue()
    if (exitValue != 0) {
        throw IllegalStateException("Command $this has exited with non-zero value $exitValue.")
    }
    return process!!.inputStream.bufferedReader().use { it.readText() }.trim()
}
