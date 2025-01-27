package snapshot

import PathContent
import TestWithContext
import com.intellij.openapi.application.ApplicationManager
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
import org.plan.research.minimization.plugin.model.context.IJDDContextBase
import org.plan.research.minimization.plugin.model.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.services.ProjectCloningService
import runSnapshotMonadAsync
import kotlin.io.path.Path
import kotlin.io.path.relativeTo

abstract class ProjectCloningSnapshotTest<C : IJDDContextBase<C>, S : SnapshotManager>
    : ProjectCloningBaseTest(), TestWithContext<C> {

    /**
     * Fabric method for SnapshotManager.
     */
    abstract fun createSnapshotManager(): S

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
        val snapshotManager = createSnapshotManager()
        val projectCloning = project.service<ProjectCloningService>()
        val initialContext = createContext(project)

        var originalFiles: Set<PathContent> = emptySet()

        val clonedProject = runBlocking {
            val clonedContext = projectCloning.clone(initialContext)!!
            clonedContext.runSnapshotMonadAsync(snapshotManager) {
                originalFiles =  clonedContext.projectDir.getAllFiles(clonedContext.projectDir.toNioPath()) +
                        clonedContext.projectDir.getPathContentPair(clonedContext.projectDir.toNioPath())
                println("Original files: $originalFiles")

                ApplicationManager.getApplication().runWriteAction {
                    context.projectDir.createChildData(this, "extraFile1.txt")
                    context.projectDir.createChildData(this, "extraFile2.txt")
                    val extraDir = context.projectDir.createChildDirectory(this, "extraDir")
                    extraDir.createChildData(this, "extraFileInDir.txt")
                }

                val result = transaction<Unit> {
                    writeAction {
                        VfsUtil.iterateChildrenRecursively(context.projectDir, null) {
                            if (it.getPathContentPair(context.projectDir.toNioPath()) !in originalFiles) {
                                if (it.exists()) {
                                    println("Delete: ${it.getPathContentPair(context.projectDir.toNioPath())}")
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
        assertEquals(originalFiles.forEach { it.path }, clonedFiles.forEach { it.path })
//        deleteContext(clonedProject)
    }

    fun testAbortedTransaction() {
        myFixture.copyDirectoryToProject("flatProject", "")
        val project = myFixture.project

        val snapshotManager = createSnapshotManager()
        val initialContext = createContext(project)
        runBlocking {
            initialContext.runSnapshotMonadAsync(snapshotManager) {
                val result = transaction {
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

        val snapshotManager = createSnapshotManager()
        runBlocking {
            initialContext.runSnapshotMonadAsync(snapshotManager) {
                val result = transaction<String> {
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