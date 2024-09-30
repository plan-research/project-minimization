import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runWithModalProgressBlocking
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import junit.framework.TestCase
import org.plan.research.minimization.plugin.getAllParents
import org.plan.research.minimization.plugin.model.dd.IJDDContext
import org.plan.research.minimization.plugin.model.dd.IJDDItem.VirtualFileDDItem
import org.plan.research.minimization.plugin.model.snapshot.SnapshotDecision
import org.plan.research.minimization.plugin.services.SnapshottingService
import org.plan.research.minimization.plugin.snapshot.VirtualFileProjectModifier
import java.nio.file.Path
import kotlin.io.path.relativeTo
import kotlin.test.assertContains
import kotlin.test.assertEquals

class SnapshotServiceTest : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/projectCloning"
    }

    fun testOneFileProjectLinear() {
        myFixture.configureByFile("oneFileProject.txt")
        doFullLinearTest()
    }

    fun testFlatProjectLinear() {
        myFixture.copyDirectoryToProject("flatProject", "")
        doFullLinearTest()
    }

    fun testTreeProjectLinear() {
        myFixture.copyDirectoryToProject("treeProject", "")
        doFullLinearTest()
    }

    fun testIterativeFlatProject() {
        myFixture.copyDirectoryToProject("flatProject", "")
        doIterativeTest()
    }
    fun testIterativeTreeProject() {
        myFixture.copyDirectoryToProject("treeProject", "")
        doIterativeTest()
    }

    private fun doFullLinearTest() {
        val project = myFixture.project
        val content = project.getAllFiles()
        val service = project.service<SnapshottingService>()
        var snapshot = runWithModalProgressBlocking(project, "") {
            service.initialSnapshot().getOrNull()!!
        }
        for (i in 0 until 10) {
            val clonedContent = snapshot.project.getAllFiles()
            TestCase.assertEquals(content, clonedContent)
            snapshot = runWithModalProgressBlocking(project, "") {
                service.makeTransaction(snapshot) { SnapshotDecision.Commit }.getOrNull()!!
            }
        }
        runWithModalProgressBlocking(project, "") {
            snapshot.rollback().getOrNull()!!
        }
    }

    private fun doIterativeTest() {
        val project = myFixture.project

        val selectedFiles = buildList {
            VfsUtilCore.iterateChildrenRecursively(project.guessProjectDir()!!, null) {
                add(VirtualFileDDItem(it))
            }
        }.filter { it.vfs.path != project.guessProjectDir()!!.path }


        val snapshottingService = project.service<SnapshottingService>()
        val content = project.getAllFiles()
        var snapshot = runWithModalProgressBlocking(project, "") { snapshottingService.initialSnapshot().getOrNull()!! }
        val snapshotContent = snapshot.project.getAllFiles()
        val context = IJDDContext(snapshot)
        assertEquals(content, snapshotContent)
        selectedFiles.indices.reversed().forEach {
            val selected = selectedFiles.take(it + 1)
            val cloningFunction = project.service<VirtualFileProjectModifier>().modifyWith(context, selected)!!
            runWithModalProgressBlocking(project, "") {
                snapshot = snapshottingService.makeTransaction(snapshot) { clonedProject ->
                    cloningFunction(clonedProject)
                    SnapshotDecision.Commit
                }.getOrNull()!!
            }
            val clonedContent = snapshot.project.getAllFiles()
            val expected = selected.map(VirtualFileDDItem::vfs).getAllFiles(project)
            println("Selected: ${expected.toList().map { it.first.toString().takeIf(String::isNotBlank) ?: "root" }}")
            println("Taken: ${clonedContent.toList().map { it.first.toString().takeIf(String::isNotBlank) ?: "root" }}")
            assertEquals(
                expected,
                clonedContent,
                "Selected files: ${
                    expected.toList().map { it.first.toString().takeIf(String::isNotBlank) ?: "root" }
                }"
            )
        }
        assertTrue(ProjectManager.getInstance().openProjects.contains(snapshot.project))
        runWithModalProgressBlocking(project, "") {
            snapshot.rollback().getOrNull()!!
        }
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        assertSize(1, ProjectManager.getInstance().openProjects)
        assertContains(ProjectManager.getInstance().openProjects, project)
    }

    private fun VirtualFile.getPathContentPair(project: Project): Pair<Path, String?> =
        toNioPath().relativeTo(project.guessProjectDir()!!.toNioPath()) to
                this.takeIf { it.isFile }?.contentsToByteArray()?.toString(Charsets.UTF_8)

    private fun Project.getAllFiles(): Set<Pair<Path, String?>> {
        return buildSet {
            val index = ProjectRootManager.getInstance(this@getAllFiles).fileIndex
            index.iterateContent {
                add(it.getPathContentPair(this@getAllFiles))
            }
        }
    }

    private fun VirtualFile.getAllFiles(project: Project): Set<Pair<Path, String?>> = buildSet {
        add(getPathContentPair(project))
        if (isFile)
            return@buildSet
        children.forEach {
            addAll(it.getAllFiles(project))
        }
    }

    private fun List<VirtualFile>.getAllFiles(project: Project): Set<Pair<Path, String?>> =
        flatMap { it.getAllFiles(project) }.toSet() + getAllParents(project.guessProjectDir()!!).map {
            it.getPathContentPair(
                project
            )
        }.toSet()

}