import java.io.BufferedReader
import java.io.File

fun String.output(workingDir: File = File(".")): String {
    println("Running command: $this")
    with(ProcessBuilder(split(" ")).directory(workingDir).start()) {
        waitFor()
        val output = inputStream.bufferedReader().use(BufferedReader::readText).trim()
        val exitValue = exitValue()
        if (exitValue != 0) {
            println(output)
            throw IllegalStateException("Command $this has exited with non-zero value $exitValue.")
        }
        return output
    }
}
