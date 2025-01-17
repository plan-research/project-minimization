package snapshot

import HeavyTestContext
import LightTestContext
import PathContent
import TestWithContext
import TestWithHeavyContext
import TestWithLightContext
import com.intellij.openapi.components.service
import getAllFiles
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.plan.research.minimization.plugin.model.context.IJDDContextBase
import org.plan.research.minimization.plugin.services.ProjectCloningService

abstract class ProjectCloningTest<C : IJDDContextBase<C>> : ProjectCloningBaseTest(), TestWithContext<C> {
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
        val context = createContext(project)
        val clonedContext = runBlocking { projectCloningService.clone(context) }
        assertNotNull(clonedContext)
        val clonedFiles = clonedContext!!.projectDir.getAllFiles(clonedContext.projectDir.toNioPath())
        assertEquals(files, clonedFiles)
        deleteContext(clonedContext as C)
        return files
    }

    private fun doFullCloningOfClonedTest(originalFileSet: Set<PathContent>? = null): Set<PathContent> {
        val project = myFixture.project
        val files = originalFileSet ?: project.getAllFiles()
        val projectCloningService = project.service<ProjectCloningService>()
        val context = createContext(project)
        val clonedContext = runBlocking { projectCloningService.clone(context) }
        assertNotNull(clonedContext)
        val clonedFiles = clonedContext!!.projectDir.getAllFiles(clonedContext.projectDir.toNioPath())
        assertEquals(files, clonedFiles)

        val clonedClonedContext =
            runBlocking { projectCloningService.clone(clonedContext) }
        assertNotNull(clonedClonedContext)
        val clonedClonedFiles = clonedClonedContext!!.projectDir.getAllFiles(clonedClonedContext.projectDir.toNioPath())
        assertEquals(files, clonedClonedFiles)
        assertNotEquals(clonedContext, clonedClonedContext)

        deleteContext(clonedContext as C)
        deleteContext(clonedClonedContext as C)
        return files
    }
}

class ProjectCloningLightTest :
    ProjectCloningTest<LightTestContext>(),
    TestWithContext<LightTestContext> by TestWithLightContext()

class ProjectCloningHeavyTest :
    ProjectCloningTest<HeavyTestContext>(),
    TestWithContext<HeavyTestContext> by TestWithHeavyContext()
