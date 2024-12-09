package snapshot

import TestWithContext
import TestWithHeavyContext
import TestWithLightContext
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.*
import com.intellij.testFramework.utils.vfs.deleteRecursively
import com.intellij.util.application
import getAllFiles
import getPathContentPair
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.ResetCommand
import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.getAllNestedElements
import org.plan.research.minimization.plugin.model.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.services.GitWrapperService
import org.plan.research.minimization.plugin.snapshot.ProjectGitSnapshotManager
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.relativeTo

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
        val snapshotManager = ProjectGitSnapshotManager()
        val gitWrapperService = application.service<GitWrapperService>()
        val git = runBlocking { gitWrapperService.gitInit(projectDir) }
        runBlocking { gitWrapperService.commitChanges(createContext(project)) }
        val originalCommitList = gitWrapperService.getCommitList(git)
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
        val clonedCommitList = gitWrapperService.getCommitList(git)

        assertEquals(originalFiles.filter { !it.path.startsWith(".git") && !it.path.toString().contains("/.git/")}.toSet(),
            clonedFiles.filter { !it.path.startsWith(".git") && !it.path.toString().contains("/.git/")}.toSet())
        assertEquals(originalCommitList.size + 1, clonedCommitList.size)

        git.reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD~1").call()
        File("${projectDir.path}/.git").deleteRecursively()
        project.guessProjectDir()!!.refresh(false, true)
    }

    fun testAbortedTransaction() {
        myFixture.copyDirectoryToProject("flatProject", "")
        val project = myFixture.project
        val projectDir = project.guessProjectDir()!!
        val gitWrapperService = application.service<GitWrapperService>()
        val git = runBlocking { gitWrapperService.gitInit(projectDir) }
        val snapshotManager = ProjectGitSnapshotManager()
        val context = createContext(project)
        runBlocking { gitWrapperService.commitChanges(createContext(project)) }
        val originalCommitList = gitWrapperService.getCommitList(git)
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
        val clonedCommitList = gitWrapperService.getCommitList(git)
        assertEquals(originalCommitList.size, clonedCommitList.size)
        assert(project.isOpen)
    }

    fun testExceptionInTransaction() {
        myFixture.copyDirectoryToProject("flatProject", "")
        val project = myFixture.project
        val projectDir = project.guessProjectDir()!!
        val gitWrapperService = application.service<GitWrapperService>()
        val git = runBlocking { gitWrapperService.gitInit(projectDir) }
        val context = createContext(project)
        runBlocking { gitWrapperService.commitChanges(createContext(project)) }
        val originalCommitList = gitWrapperService.getCommitList(git)

        val snapshotManager = ProjectGitSnapshotManager()
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
        val clonedCommitList = gitWrapperService.getCommitList(git)
        assertEquals(originalCommitList.size, clonedCommitList.size)
        assert(project.isOpen)
    }
}

class ProjectCloningGitSnapshotHeavyTest :
    ProjectCloningGitSnapshotTest<HeavyIJDDContext>(),
    TestWithContext<HeavyIJDDContext> by TestWithHeavyContext()


class ProjectCloningGitSnapshotLightTest :
    ProjectCloningGitSnapshotTest<LightIJDDContext>(),
    TestWithContext<LightIJDDContext> by TestWithLightContext()
