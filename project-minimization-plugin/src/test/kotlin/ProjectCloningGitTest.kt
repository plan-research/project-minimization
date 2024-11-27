import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.NoHeadException
import org.plan.research.minimization.plugin.model.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.services.GitWrapperService
import java.io.File

abstract class ProjectCloningGitTest<C : IJDDContext> : ProjectCloningBaseTest(), TestWithContext<C> {
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
        val gitWrapperService = project.service<GitWrapperService>()
        val git = gitWrapperService.gitInitOrOpen(project.guessProjectDir()!!)
        val originalCommitList = gitWrapperService.getCommitList(git)
        val context = createContext(project)
        val clonedContext = runBlocking { gitWrapperService.commitChanges(context) }
        assertNotNull(clonedContext)
        val clonedFiles = clonedContext.projectDir.getAllFiles(clonedContext.projectDir.toNioPath())
        val clonedCommitList = gitWrapperService.getCommitList(git)
        assertEquals(files.filter { !it.path.startsWith(".git") && !it.path.toString().contains("/.git/")},
                    clonedFiles.filter { !it.path.startsWith(".git") && !it.path.toString().contains("/.git/")})
        assertEquals(myFixture.project.guessProjectDir()!!.name, clonedContext.projectDir.name)
        assertEquals(originalCommitList.size + 1, clonedCommitList.size)

        return files
    }

    private fun doFullCloningOfClonedTest(originalFileSet: Set<PathContent>? = null): Set<PathContent> {
        val project = myFixture.project
        val files = originalFileSet ?: project.getAllFiles()
        val gitWrapperService = project.service<GitWrapperService>()
        val git = gitWrapperService.gitInitOrOpen(project.guessProjectDir()!!)
        val originalCommitList = gitWrapperService.getCommitList(git)
        val context = createContext(project)
        val clonedContext = runBlocking { gitWrapperService.commitChanges(context) }
        assertNotNull(clonedContext)
        val clonedFiles = clonedContext!!.projectDir.getAllFiles(clonedContext.projectDir.toNioPath())
        val clonedCommitList = gitWrapperService.getCommitList(git)
        assertEquals(files.filter { !it.path.startsWith(".git") && !it.path.toString().contains("/.git/")}.toSet(),
            clonedFiles.filter { !it.path.startsWith(".git") && !it.path.toString().contains("/.git/")}.toSet())

        val clonedClonedContext =
            runBlocking { gitWrapperService.commitChanges(clonedContext) }
        assertNotNull(clonedClonedContext)
        val clonedClonedFiles = clonedClonedContext.projectDir.getAllFiles(clonedClonedContext.projectDir.toNioPath())
        val clonedClonedCommitList = gitWrapperService.getCommitList(git)
        assertEquals(files.filter { !it.path.startsWith(".git") && !it.path.toString().contains("/.git/")}.toSet(),
            clonedClonedFiles.filter { !it.path.startsWith(".git") && !it.path.toString().contains("/.git/")}.toSet())

        assertEquals(myFixture.project.guessProjectDir()!!.name, clonedContext.projectDir.name)
        assertEquals(myFixture.project.guessProjectDir()!!.name, clonedClonedContext.projectDir.name)

        assertEquals(originalCommitList.size + 1, clonedCommitList.size)
        assertEquals(originalCommitList.size + 2, clonedClonedCommitList.size)

        assertEquals(clonedCommitList[0], clonedClonedCommitList[1])

        return files
    }
}

class ProjectCloningGitLightTest :
    ProjectCloningGitTest<LightIJDDContext>(),
    TestWithContext<LightIJDDContext> by TestWithLightContext()

class ProjectCloningGitHeavyTest :
    ProjectCloningGitTest<HeavyIJDDContext>(),
    TestWithContext<HeavyIJDDContext> by TestWithHeavyContext()
