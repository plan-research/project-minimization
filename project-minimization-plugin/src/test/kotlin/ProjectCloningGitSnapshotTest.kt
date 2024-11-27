import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.*
import com.intellij.testFramework.utils.vfs.deleteRecursively
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.treewalk.TreeWalk
import org.junit.jupiter.api.assertDoesNotThrow
import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.getAllNestedElements
import org.plan.research.minimization.plugin.model.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.services.ProjectCloningGitService
import org.plan.research.minimization.plugin.services.ProjectCloningService
import org.plan.research.minimization.plugin.snapshot.ProjectCloningGitSnapshotManager
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.relativeTo
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.isAccessible

abstract class ProjectCloningGitSnapshotTest<C : IJDDContext> : ProjectCloningBaseTest(), TestWithContext<C> {
    fun testOneFileProjectPartialCloning() {
        val file = myFixture.configureByFile("oneFileProject.txt")
        doPartialCloningTest(
            listOf(
                file.virtualFile
            )
        )
        doPartialCloningTest(listOf())
    }

    fun testFlatProjectPartialCloning() {
        val root = myFixture.copyDirectoryToProject("flatProject", "")
        val fileMap = root.children.associateBy { it.name }
        doPartialCloningTest(root.getAllNestedElements())
        doPartialCloningTest(listOf(fileMap[".config"], fileMap["A"], fileMap["B"]).map { it!! })
        doPartialCloningTest(listOf(fileMap[".config"]!!))
    }

    fun testTreeProjectPartialCloning() {
        val root = myFixture.copyDirectoryToProject("treeProject", "")
        val updatedFileMap = {
            buildMap {
                VfsUtilCore.iterateChildrenRecursively(root, null) {
                    this[it.toNioPath().relativeTo(project.guessProjectDir()!!.toNioPath())] = it
                    true
                }
            }
        }
        doPartialCloningTest(listOf(root))
        doPartialCloningTest(listOf(updatedFileMap()[Path("root-file")]!!))
        doPartialCloningTest(listOf(updatedFileMap()[Path("depth-1-a", "depth-2-a", "depth-2-file-a-a")]!!))
        val fileMap = updatedFileMap()
        doPartialCloningTest(
            listOf(
                fileMap[Path("depth-1-a", "depth-2-a", "pretty-good-file")]!!,
                fileMap[Path("depth-1-a", "depth-2-a-prime", "pretty-good-file")]!!,
                fileMap[Path("depth-1-a", "pretty-good-file")]!!,
                fileMap[Path("depth-1-b", "pretty-good-file")]!!,
                fileMap[Path("depth-1-b", "depth-2-b", "pretty-good-file")]!!,
                fileMap[Path("depth-1-b", "depth-2-b-prime", "pretty-good-file")]!!,
            )
        )
    }

    private fun doPartialCloningTest(
        selectedFiles: List<VirtualFile>
    ) {
        val project = myFixture.project
        val projectDir = project.guessProjectDir()!!
        val snapshotManager = ProjectCloningGitSnapshotManager(project)
        val git = gitInitOrOpen()
        runBlocking { ProjectCloningGitService().commitChanges(createContext(project)) }
        val originalCommitList = getCommitList(git)
        val originalFiles =
            selectedFiles.getAllFiles(projectDir) + projectDir.getPathContentPair(projectDir.toNioPath()) +
                    projectDir.findChild(".git")!!.getAllFiles(projectDir.toNioPath())
        val context = createContext(project)

        val clonedProject = runBlocking {
            val clonedContext = context
            snapshotManager.transaction<Unit, _>(clonedContext) { newContext ->
                writeAction {
                    VfsUtil.iterateChildrenRecursively(newContext.projectDir, null) {
                        if (it.getPathContentPair(newContext.projectDir.toNioPath()) !in originalFiles) {
                            if (it.exists()) {
                                it.deleteRecursively()
                            }
                        }
                        true
                    }
                }
                newContext
            }
        }.getOrNull()
        assertNotNull(clonedProject)
        val clonedFiles = clonedProject!!.projectDir.getAllFiles(clonedProject.projectDir.toNioPath())
        val clonedCommitList = getCommitList(git)

        assertEquals(originalFiles.filter { !it.path.startsWith(".git") && !it.path.toString().contains("/.git/")}.toSet(),
            clonedFiles.filter { !it.path.startsWith(".git") && !it.path.toString().contains("/.git/")}.toSet())
        assertEquals(originalCommitList.size + 1, clonedCommitList.size)

        git.reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD~1").call();
        File("${projectDir.path}/.git").deleteRecursively()
        project.guessProjectDir()!!.refresh(false, true)
    }

    fun testAbortedTransaction() {
        myFixture.copyDirectoryToProject("flatProject", "")
        val project = myFixture.project
        val git = gitInitOrOpen()
        val snapshotManager = ProjectCloningGitSnapshotManager(project)
        val context = createContext(project)
        runBlocking { ProjectCloningGitService().commitChanges(createContext(project)) }
        val originalCommitList = getCommitList(git)
        val result = runBlocking {
            snapshotManager.transaction(context) { newContext ->
                writeAction {
                    newContext.projectDir.findChild(".config")!!.deleteRecursively()
                }
                raise("Abort")
            }
        }.leftOrNull()
        assertEquals(SnapshotError.Aborted("Abort"), result)
        project.guessProjectDir()!!.refresh(false, true)
        assertNotNull(project.guessProjectDir()!!.findChild(".config"))
        val clonedCommitList = getCommitList(git)
        assertEquals(originalCommitList.size, clonedCommitList.size)
        assert(project.isOpen)
    }

    fun testExceptionInTransaction() {
        myFixture.copyDirectoryToProject("flatProject", "")
        val project = myFixture.project
        val git = gitInitOrOpen()
        val context = createContext(project)
        runBlocking { ProjectCloningGitService().commitChanges(createContext(project)) }
        val originalCommitList = getCommitList(git)

        val snapshotManager = ProjectCloningGitSnapshotManager(project)
        val result = runBlocking {
            snapshotManager.transaction<String, _>(context) { newContext ->
                writeAction {
                    newContext.projectDir.findChild(".config")!!.deleteRecursively()
                }
                throw Exception("Abort")
            }
        }.leftOrNull()

        assertEquals("Abort", (result as? SnapshotError.TransactionFailed)?.error?.message)
        project.guessProjectDir()!!.refresh(false, true)
        assertNotNull(project.guessProjectDir()!!.findChild(".config"))
        val clonedCommitList = getCommitList(git)
        assertEquals(originalCommitList.size, clonedCommitList.size)
        assert(project.isOpen)
    }

    private fun gitInitOrOpen(): Git {
        val projectDir: File = myFixture.project.guessProjectDir()!!.toNioPath().toFile()
        if (projectDir.resolve(".git").exists()) {
            println("open project")
            return Git.open(projectDir)
        }
        println("init project")
        return Git.init()
            .setDirectory(myFixture.project.guessProjectDir()!!.toNioPath().toFile())
            .call().apply {
                commit().setMessage("init commit").call()
            }
    }

    private fun commitListEmpty(git: Git): Boolean {
        return try {
            git.log().call() // This will throw NoHeadException if there are no commits
            false
        } catch (e: NoHeadException) {
            true
        } finally {
            git.close()
        }
    }

    /* returns all commits in the chronically reversed order */
    /* getCommitList(git)[0] == HEAD */
    private fun getCommitList(git: Git): List<String> {
        myFixture.project.guessProjectDir()!!.refresh(false, true)
        if (commitListEmpty(git)) {
            return listOf()
        }
        return git.log().all().call().map { it.name }
    }

    private fun getTrackedFiles(): List<String> {
        val repoDir = File(myFixture.project.guessProjectDir()!!.path)
        val git = Git.open(repoDir)
        val repository = git.repository
        val trackedFiles = mutableListOf<String>()

        // Resolve HEAD to the latest commit
        val headCommit = repository.resolve("HEAD^{tree}")

        // Traverse the tree of the latest commit
        TreeWalk(repository).use { treeWalk ->
            treeWalk.addTree(headCommit)
            treeWalk.isRecursive = true // List all files recursively
            while (treeWalk.next()) {
                trackedFiles.add(treeWalk.pathString)
            }
        }

        git.close()
        return trackedFiles
    }
}

class ProjectCloningGitSnapshotHeavyTest :
    ProjectCloningGitSnapshotTest<HeavyIJDDContext>(),
    TestWithContext<HeavyIJDDContext> by TestWithHeavyContext()


class ProjectCloningGitSnapshotLightTest :
    ProjectCloningGitSnapshotTest<LightIJDDContext>(),
    TestWithContext<LightIJDDContext> by TestWithLightContext()
