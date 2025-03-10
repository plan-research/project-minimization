package snapshot

import PathContent
import TestWithContext
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.testFramework.utils.vfs.deleteRecursively
import getAllFiles
import getPathContentPair
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.plugin.context.snapshot.SnapshotError
import org.plan.research.minimization.plugin.context.IJDDContextBase
import org.plan.research.minimization.plugin.context.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.services.ProjectCloningService
import runSnapshotMonadAsync
import kotlin.io.path.relativeTo

abstract class ProjectCloningSnapshotTest<C : IJDDContextBase<C>, S : SnapshotManager>
    : ProjectCloningBaseTest(), TestWithContext<C> {

    /**
     * Fabric method for SnapshotManager.
     */
    abstract fun createSnapshotManager(): S

    fun testOneFileProjectPartialCloning() {
        myFixture.configureByFile("oneFileProject.txt")
        doPartialCloningTest()
    }

    fun testFlatProjectPartialCloning() {
        val root = myFixture.copyDirectoryToProject("flatProject", "")
        root.children.associateBy { it.name }
        doPartialCloningTest()
    }

    fun testTreeProjectPartialCloning() {
        val root = myFixture.copyDirectoryToProject("treeProject", "")
        buildMap {
            VfsUtilCore.iterateChildrenRecursively(root, null) {
                this[it.toNioPath().relativeTo(project.guessProjectDir()!!.toNioPath())] = it
                true
            }
        }
        doPartialCloningTest()
    }

    private fun doPartialCloningTest(
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

                writeAction {
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
        deleteContext(clonedProject)
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