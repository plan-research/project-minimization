import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.NoHeadException
import org.junit.Assert.assertNotEquals
import org.plan.research.minimization.plugin.model.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.services.ProjectCloningGitService

@Suppress("UNCHECKED_CAST")
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
        val projectCloningGitService = project.service<ProjectCloningGitService>()
        val context = createContext(project)
        val git = gitInit()
        val originalCommitList = getCommitList(git)
        val clonedContext = runBlocking { projectCloningGitService.clone(context) }
        assertNotNull(clonedContext)
        val clonedFiles = clonedContext!!.projectDir.getAllFiles(clonedContext.projectDir.toNioPath())
        val clonedCommitList = getCommitList(git)
        assertEquals(files.filter { !it.path.startsWith(".git") && !it.path.toString().contains("/.git/")},
                    clonedFiles.filter { !it.path.startsWith(".git") && !it.path.toString().contains("/.git/")})
        assertEquals(myFixture.project.guessProjectDir()!!.name, clonedContext.projectDir.name)
        assertEquals(originalCommitList.size + 1, clonedCommitList.size)
        deleteContext(clonedContext as C)
        return files
    }

    private fun doFullCloningOfClonedTest(originalFileSet: Set<PathContent>? = null): Set<PathContent> {
        val project = myFixture.project
        val files = originalFileSet ?: project.getAllFiles()
        val projectCloningGitService = project.service<ProjectCloningGitService>()
        val context = createContext(project)
        val git = gitInit()
        val originalCommitList = getCommitList(git)
        val clonedContext = runBlocking { projectCloningGitService.clone(context) }
        assertNotNull(clonedContext)
        val clonedFiles = clonedContext!!.projectDir.getAllFiles(clonedContext.projectDir.toNioPath())
        val clonedCommitList = getCommitList(git)
        assertEquals(files.filter { !it.path.startsWith(".git") && !it.path.toString().contains("/.git/")},
            clonedFiles.filter { !it.path.startsWith(".git") && !it.path.toString().contains("/.git/")})

        val clonedClonedContext =
            runBlocking { projectCloningGitService.clone(clonedContext) }
        assertNotNull(clonedClonedContext)
        val clonedClonedFiles = clonedClonedContext!!.projectDir.getAllFiles(clonedClonedContext.projectDir.toNioPath())
        val clonedClonedCommitList = getCommitList(git)
        assertEquals(files.filter { !it.path.startsWith(".git") && !it.path.toString().contains("/.git/")},
            clonedClonedFiles.filter { !it.path.startsWith(".git") && !it.path.toString().contains("/.git/")})
        assertNotEquals(clonedContext, clonedClonedContext)

        assertEquals(myFixture.project.guessProjectDir()!!.name, clonedContext.projectDir.name)
        assertEquals(myFixture.project.guessProjectDir()!!.name, clonedClonedContext.projectDir.name)

        assertEquals(originalCommitList.size + 1, clonedCommitList.size)
        assertEquals(originalCommitList.size + 2, clonedClonedCommitList.size)

        assertEquals(clonedCommitList[0], clonedClonedCommitList[1])

        deleteContext(clonedContext as C)
        deleteContext(clonedClonedContext as C)
        return files
    }

    private fun gitInit(): Git {
        return Git.init()
            .setDirectory(myFixture.project.guessProjectDir()!!.toNioPath().toFile())
            .call()
    }

    private fun commitListEmpty(git: Git): Boolean {
        return try {
            git.log().call() // This will throw NoHeadException if there are no commits
            false
        } catch (e: NoHeadException) {
            true
        } finally {
            git.close()
        }
    }

    /* returns all commits in the chronically reversed order */
    /* getCommitList(git)[0] == HEAD */
    private fun getCommitList(git: Git): List<String> {
        if (commitListEmpty(git)) {
            return listOf()
        }
        return git.log().call().map { it.name }
    }
}

class ProjectCloningGitLightTest :
    ProjectCloningGitTest<LightIJDDContext>(),
    TestWithContext<LightIJDDContext> by TestWithLightContext()

class ProjectCloningGitHeavyTest :
    ProjectCloningGitTest<HeavyIJDDContext>(),
    TestWithContext<HeavyIJDDContext> by TestWithHeavyContext()
