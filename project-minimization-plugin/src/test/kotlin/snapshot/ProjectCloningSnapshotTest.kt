package snapshot

import TestWithContext
import TestWithHeavyContext
import TestWithLightContext
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.utils.vfs.deleteRecursively
import getAllFiles
import getPathContentPair
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.getAllNestedElements
import org.plan.research.minimization.plugin.model.context.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.LightIJDDContext
import org.plan.research.minimization.plugin.services.ProjectCloningService
import org.plan.research.minimization.plugin.snapshot.ProjectCloningSnapshotManager
import runMonad
import kotlin.io.path.Path
import kotlin.io.path.relativeTo

@Suppress("UNCHECKED_CAST")
abstract class ProjectCloningSnapshotTest<C : IJDDContext> : ProjectCloningBaseTest(), TestWithContext<C> {
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
        val projectDir = project.guessProjectDir()!!
        val snapshotManager = ProjectCloningSnapshotManager(project)
        val projectCloning = project.service<ProjectCloningService>()
        val initialContext = createContext(project)

        val originalFiles =
            selectedFiles.getAllFiles(projectDir) + project.guessProjectDir()!!
                .getPathContentPair(projectDir.toNioPath())

        val clonedProject = runBlocking {
            val clonedContext = projectCloning.clone(initialContext)!!
            clonedContext.runMonad {
                val result = snapshotManager.transaction<Unit, _> {
                    writeAction {
                        VfsUtil.iterateChildrenRecursively(context.projectDir, null) {
                            if (it.getPathContentPair(context.projectDir.toNioPath()) !in originalFiles) {
                                if (it.exists()) {
                                    it.deleteRecursively()
                                }
                            }
                            true
                        }
                    }
                }
                assert(result.isRight())
            }
        }
        val clonedFiles = clonedProject.projectDir.getAllFiles(clonedProject.projectDir.toNioPath())
        assertEquals(originalFiles, clonedFiles)
        deleteContext(clonedProject as C)
    }

    fun testAbortedTransaction() {
        myFixture.copyDirectoryToProject("flatProject", "")
        val project = myFixture.project

        val snapshotManager = ProjectCloningSnapshotManager(project)
        val initialContext = createContext(project)
        runBlocking {
            initialContext.runMonad {
                val result = snapshotManager.transaction {
                    writeAction {
                        context.projectDir.findChild(".config")!!.deleteRecursively()
                    }
                    raise("Abort")
                }.leftOrNull()
                assertEquals(SnapshotError.Aborted("Abort"), result)
            }
        }

        assertNotNull(project.guessProjectDir()!!.findChild(".config"))
        assert(project.isOpen)
    }

    fun testExceptionInTransaction() {
        myFixture.copyDirectoryToProject("flatProject", "")
        val project = myFixture.project
        val initialContext = createContext(project)

        val snapshotManager = ProjectCloningSnapshotManager(project)
        runBlocking {
            initialContext.runMonad {
                val result = snapshotManager.transaction<String, _> {
                    writeAction {
                        context.projectDir.findChild(".config")!!.deleteRecursively()
                    }
                    throw Exception("Abort")
                }.leftOrNull()
                assertEquals("Abort", (result as? SnapshotError.TransactionFailed)?.error?.message)
            }
        }

        assertNotNull(project.guessProjectDir()!!.findChild(".config"))
        assert(project.isOpen)
    }
}

class ProjectCloningSnapshotHeavyTest :
    ProjectCloningSnapshotTest<HeavyIJDDContext>(),
    TestWithContext<HeavyIJDDContext> by TestWithHeavyContext()


class ProjectCloningSnapshotLightTest :
    ProjectCloningSnapshotTest<LightIJDDContext>(),
    TestWithContext<LightIJDDContext> by TestWithLightContext()
