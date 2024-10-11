import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.testFramework.utils.vfs.deleteRecursively
import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.getAllNestedElements
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.services.ProjectCloningService
import org.plan.research.minimization.plugin.snapshot.ProjectCloningSnapshotManager
import kotlin.io.path.Path
import kotlin.io.path.relativeTo

class ProjectCloningSnapshotTest : ProjectCloningBaseTest() {
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
        val fileMap = buildMap {
            VfsUtilCore.iterateChildrenRecursively(root, null) {
                this[it.toNioPath().relativeTo(project.guessProjectDir()!!.toNioPath())] = it
                true
            }
        }
        doPartialCloningTest(listOf(root))
        doPartialCloningTest(listOf(fileMap[Path("root-file")]!!))
        doPartialCloningTest(listOf(fileMap[Path("depth-1-a", "depth-2-a", "depth-2-file-a-a")]!!))
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
        val snapshotManager = ProjectCloningSnapshotManager(project)
        val projectCloning = project.service<ProjectCloningService>()

        val originalFiles =
            selectedFiles.getAllFiles(project) + project.guessProjectDir()!!.getPathContentPair(project)

        val clonedProject = runWithModalProgressBlocking(project, "") {
            val context = IJDDContext(projectCloning.clone(project)!!, project)
            snapshotManager.transaction<Unit>(context) { newContext ->
                writeAction {
                    VfsUtil.iterateChildrenRecursively(newContext.project.guessProjectDir()!!, null) {
                        if (it.getPathContentPair(newContext.project) !in originalFiles) {
                            if (it.exists()) {
                                it.deleteRecursively()
                            }
                        }
                        true
                    }
                }
                newContext
            }
        }.getOrNull()?.project
        assertNotNull(clonedProject)
        val clonedFiles = clonedProject!!.getAllFiles()
        assertEquals(originalFiles, clonedFiles)
        ProjectManager.getInstance().closeAndDispose(clonedProject)
    }

    fun testAbortedTransaction() {
        myFixture.copyDirectoryToProject("flatProject", "")
        val project = myFixture.project

        val snapshotManager = ProjectCloningSnapshotManager(project)
        val result = runWithModalProgressBlocking(project, "") {
            snapshotManager.transaction(IJDDContext(project)) { newProject ->
                writeAction {
                    newProject.project.guessProjectDir()!!.findChild(".config")!!.deleteRecursively()
                }
                raise("Abort")
            }
        }.leftOrNull()

        assertEquals(SnapshotError.Aborted("Abort"), result)
        assertNotNull(project.guessProjectDir()!!.findChild(".config"))
        assert(project.isOpen)
    }

    fun testExceptionInTransaction() {
        myFixture.copyDirectoryToProject("flatProject", "")
        val project = myFixture.project

        val snapshotManager = ProjectCloningSnapshotManager(project)
        val result = runWithModalProgressBlocking(project, "") {
            val context = IJDDContext(project)
            snapshotManager.transaction<String>(context) { newProject ->
                writeAction {
                    newProject.project.guessProjectDir()!!.findChild(".config")!!.deleteRecursively()
                }
                throw Exception("Abort")
            }
        }.leftOrNull()

        assertEquals("Abort", (result as? SnapshotError.TransactionFailed)?.error?.message)
        assertNotNull(project.guessProjectDir()!!.findChild(".config"))
        assert(project.isOpen)
    }
}