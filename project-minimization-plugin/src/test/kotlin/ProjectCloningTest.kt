import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runWithModalProgressBlocking
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import org.plan.research.minimization.plugin.getAllNestedElements
import org.plan.research.minimization.plugin.getAllParents
import org.plan.research.minimization.plugin.services.ProjectCloningService
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.relativeTo

class ProjectCloningTest : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/projectCloning"
    }

    fun testOneFileProject() {
        myFixture.configureByFile("oneFileProject.txt")
        doFullCloningTest()
    }

    fun testOneFileProjectMultipleCloning() {
        myFixture.configureByFile("oneFileProject.txt")
        doFullCloningTest(doFullCloningTest(doFullCloningTest()))
    }

    fun testFlatProjectCloning() {
        myFixture.copyDirectoryToProject("flatProject", "")
        doFullCloningTest()
    }

    fun testTreeProjectCloning() {
        myFixture.copyDirectoryToProject("treeProject", "")
        doFullCloningTest()
    }

    fun testTreeProjectMultipleCloning() {
        myFixture.copyDirectoryToProject("treeProject", "")
        doFullCloningTest(doFullCloningTest(doFullCloningTest()))
    }

    fun testOneFileProjectCloningCloning() {
        myFixture.configureByFile("oneFileProject.txt")
        doFullCloningOfClonedTest()
    }

    fun testFlatProjectCloningCloning() {
        myFixture.copyDirectoryToProject("flatProject", "")
        doFullCloningOfClonedTest()
    }

    fun testTreeProjectCloningCloning() {
        myFixture.copyDirectoryToProject("treeProject", "")
        doFullCloningOfClonedTest()
    }

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
        val projectCloningService = project.service<ProjectCloningService>()

        val originalFiles =
            selectedFiles.getAllFiles(project) + setOf(project.guessProjectDir()!!.getPathContentPair(project))

        val clonedProject = runWithModalProgressBlocking(project, "") {
            projectCloningService.clone(project, selectedFiles)
        }
        kotlin.test.assertNotNull(clonedProject)
        val clonedFiles = clonedProject.getAllFiles()
        kotlin.test.assertEquals(originalFiles, clonedFiles)
        ProjectManager.getInstance().closeAndDispose(clonedProject)
    }

    private fun doFullCloningTest(originalFileSet: Set<Pair<Path, String?>>? = null): Set<Pair<Path, String?>> {
        val project = myFixture.project
        val files = originalFileSet ?: project.getAllFiles()
        val projectCloningService = project.service<ProjectCloningService>()
        val clonedProject = runWithModalProgressBlocking(project, "") { projectCloningService.clone(project) }
        kotlin.test.assertNotNull(clonedProject)
        val clonedFiles = clonedProject.getAllFiles()
        kotlin.test.assertEquals(files, clonedFiles)
        ProjectManager.getInstance().closeAndDispose(clonedProject)
        return files
    }

    private fun doFullCloningOfClonedTest(originalFileSet: Set<Pair<Path, String?>>? = null): Set<Pair<Path, String?>> {
        val project = myFixture.project
        val files = originalFileSet ?: project.getAllFiles()
        val projectCloningService = project.service<ProjectCloningService>()
        val clonedProject = runWithModalProgressBlocking(project, "") { projectCloningService.clone(project) }
        kotlin.test.assertNotNull(clonedProject)
        val clonedFiles = clonedProject.getAllFiles()
        kotlin.test.assertEquals(files, clonedFiles)

        val clonedClonedProject =
            runWithModalProgressBlocking(project, "") { projectCloningService.clone(clonedProject) }
        kotlin.test.assertNotNull(clonedClonedProject)
        val clonedClonedFiles = clonedClonedProject.getAllFiles()
        kotlin.test.assertEquals(files, clonedClonedFiles)
        kotlin.test.assertNotEquals(clonedProject, clonedClonedProject)

        ProjectManager.getInstance().closeAndDispose(clonedClonedProject)
        ProjectManager.getInstance().closeAndDispose(clonedProject)
        return files
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
        flatMap { it.getAllFiles(project) }.toSet() + getAllParents(project.guessProjectDir()!!).map { it.getPathContentPair(project) }.toSet()
}

