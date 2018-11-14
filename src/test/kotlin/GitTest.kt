import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.TimeUnit

class GitTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    lateinit var localRepository: File
    lateinit var remoteRepository: File

    lateinit var commitHash: String

    @Before
    fun setUp() {
        localRepository = tempFolder.newFolder()
        "git init".output(localRepository)
        File(localRepository, "testfile.kt").createNewFile()
        "git add testfile.kt".output(localRepository)
        "git commit -m \"testcommit\"".output(localRepository)
        commitHash = "git rev-parse HEAD".output(localRepository)
        remoteRepository = tempFolder.newFolder("remote-repo")
        "git init --bare --shared=group ${remoteRepository.path}".output(localRepository)

        "git remote add origin ${remoteRepository.path}".output(localRepository)

        "git push origin master".output(localRepository)
    }

    @Test
    fun `creates a tag with a prefix`() {
        val git = Git(path = localRepository.path, remote = "origin", tagPrefix = "v")
        git.createReleaseTag("2.0.0")

        assertEquals(
            "v2.0.0",
            "git tag".output(remoteRepository)
        )
    }

    @Test
    fun `creates a tag with no prefix`() {
        val git = Git(path = localRepository.path, remote = "origin", tagPrefix = "")
        git.createReleaseTag("2.0.0")

        assertEquals(
            "2.0.0",
            "git tag".output(remoteRepository)
        )
    }

    fun String.output(repo: File): String = execuute(repo).inputStream.bufferedReader().use { it.readText() }.trim()

    fun String.execuute(workingDir: File = File(".")): Process {
        return split(" ").execute(workingDir)
    }

    fun List<String>.execute(workingDir: File = File(".")): Process {
        println("Running command: $this")
        val process = ProcessBuilder(this)
            .directory(workingDir)
            .start()
        process.waitFor(60, TimeUnit.SECONDS)
        val exitValue = process.exitValue()
        if (exitValue != 0) {
            throw IllegalStateException("Command $this has exited with non-zero value $exitValue.")
        }
        return process
    }
}
