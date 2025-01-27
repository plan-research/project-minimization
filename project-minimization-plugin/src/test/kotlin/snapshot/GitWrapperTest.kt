package snapshot

import HeavyTestContext
import LightTestContext
import PathContent
import TestWithContext
import TestWithHeavyContext
import TestWithLightContext
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import getAllFiles
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.plugin.model.context.IJDDContextBase
import org.plan.research.minimization.plugin.services.GitWrapperService
import org.plan.research.minimization.plugin.services.ProjectCloningService


abstract class GitWrapperTest<C : IJDDContextBase<C>> : ProjectCloningBaseTest(), TestWithContext<C> {
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
        val context = createContext(project)
        val git = runBlocking {
            gitWrapperService.gitInit(context.indexProjectDir) { file -> ProjectCloningService.isImportant(file, context.projectDir) }
        }
        val originalCommitList = gitWrapperService.getCommitList(git)
        val clonedContext = runBlocking {gitWrapperService.commitChanges(context, git) }
        val clonedGit = git
        assertNotNull(clonedContext)
        val clonedFiles = clonedContext.projectDir.getAllFiles(clonedContext.projectDir.toNioPath())
        val clonedCommitList = gitWrapperService.getCommitList(clonedGit)
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
        val context = createContext(project)
        val git = runBlocking {
            gitWrapperService.gitInit(context.indexProjectDir) { _ -> true }
        }
        val originalCommitList = gitWrapperService.getCommitList(git)
        val clonedContext = runBlocking {gitWrapperService.commitChanges(context, git) }
        assertNotNull(clonedContext)
        val clonedFiles = clonedContext.projectDir.getAllFiles(clonedContext.projectDir.toNioPath())
        val clonedGit = git
        val clonedCommitList = gitWrapperService.getCommitList(clonedGit)
        assertEquals(files.filter { !it.path.startsWith(".git") && !it.path.toString().contains("/.git/")}.toSet(),
            clonedFiles.filter { !it.path.startsWith(".git") && !it.path.toString().contains("/.git/")}.toSet())

        val clonedClonedContext =
            runBlocking { gitWrapperService.commitChanges(clonedContext, clonedGit) }
        assertNotNull(clonedClonedContext)
        val clonedCLonedGit = git
        val clonedClonedFiles = clonedClonedContext.projectDir.getAllFiles(clonedClonedContext.projectDir.toNioPath())
        val clonedClonedCommitList = gitWrapperService.getCommitList(clonedCLonedGit)
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

class GitWrapperLightTest :
    GitWrapperTest<LightTestContext>(),
    TestWithContext<LightTestContext> by TestWithLightContext()

class GitWrapperHeavyTest :
    GitWrapperTest<HeavyTestContext>(),
    TestWithContext<HeavyTestContext> by TestWithHeavyContext()
