import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.plan.research.minimization.plugin.services.ProjectCloningService

class ProjectCloningTest : ProjectCloningBaseTest() {
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

    private fun doFullCloningTest(originalFileSet: Set<PathContent>? = null): Set<PathContent> {
        val project = myFixture.project
        val files = originalFileSet ?: project.getAllFiles()
        val projectCloningService = project.service<ProjectCloningService>()
        val clonedProject = runBlocking { projectCloningService.clone(project) }
        assertNotNull(clonedProject)
        val clonedFiles = clonedProject!!.getAllFiles()
        assertEquals(files, clonedFiles)
        runBlocking(Dispatchers.EDT) {
            ProjectManager.getInstance().closeAndDispose(clonedProject)
        }
        return files
    }

    private fun doFullCloningOfClonedTest(originalFileSet: Set<PathContent>? = null): Set<PathContent> {
        val project = myFixture.project
        val files = originalFileSet ?: project.getAllFiles()
        val projectCloningService = project.service<ProjectCloningService>()
        val clonedProject = runBlocking { projectCloningService.clone(project) }
        assertNotNull(clonedProject)
        val clonedFiles = clonedProject!!.getAllFiles()
        assertEquals(files, clonedFiles)

        val clonedClonedProject =
            runBlocking { projectCloningService.clone(clonedProject) }
        assertNotNull(clonedClonedProject)
        val clonedClonedFiles = clonedClonedProject!!.getAllFiles()
        assertEquals(files, clonedClonedFiles)
        assertNotEquals(clonedProject, clonedClonedProject)

        runBlocking(Dispatchers.EDT) {
            ProjectManager.getInstance().closeAndDispose(clonedClonedProject)
            ProjectManager.getInstance().closeAndDispose(clonedProject)
        }
        return files
    }
}
