package hierarchy.file

import HeavyTestContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import getAllFiles
import getPathContentPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.plugin.compilation.DumbCompiler
import org.plan.research.minimization.plugin.settings.data.FileLevelStageData
import org.plan.research.minimization.plugin.settings.data.CompilationStrategy
import org.plan.research.minimization.plugin.settings.data.DDStrategy
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import org.plan.research.minimization.plugin.services.ProjectCloningService
import org.plan.research.minimization.plugin.services.ProjectOpeningService
import org.plan.research.minimization.plugin.util.getMinimizationStage

class FileLevelStageTest : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/fileTreeHierarchy"
    }

    override fun runInDispatchThread(): Boolean = false

    override fun setUp() {
        super.setUp()
        project.service<MinimizationPluginSettings>().stateObservable.compilationStrategy.set(CompilationStrategy.DUMB)
        project.service<MinimizationPluginSettings>().stateObservable.minimizationTransformations.set(emptyList())
        service<ProjectOpeningService>().isTest = true
    }

    fun testFullProject() {
        val root = myFixture.copyDirectoryToProject("complicated-project", "")
        minimizeProjectTest(root, null)
    }

    fun testOneTargetFileProject() {
        val root = myFixture.copyDirectoryToProject("complicated-project", "")
        minimizeProjectTest(
            root, listOf(
                "0/1/2/3/4/5/6/7/8/9/10.txt",
            )
        )
    }

    fun testPartialProject() {
        val root = myFixture.copyDirectoryToProject("complicated-project", "")
        minimizeProjectTest(
            root, listOf(
                "0/1.txt",
                "0/1/2/3/4/5.txt",
                "0/1/2/3/4/5/6.txt",
            )
        )
    }

    private fun minimizeProjectTest(root: VirtualFile, targetPaths: List<String>?) {
        DumbCompiler.targetPaths = targetPaths
        val project = myFixture.project
        val stage = FileLevelStageData(
            DDStrategy.PROBABILISTIC_DD
        ).getMinimizationStage()
        val context = HeavyTestContext(project)

        val targetFiles = if (targetPaths == null) {
            project.getAllFiles()
        } else {
            targetPaths.map { root.findFileByRelativePath(it)!! }
                .getAllFiles(context.projectDir) + root.getPathContentPair(context.projectDir.toNioPath())
        }

        val clonedContext = runBlocking {
            project.service<ProjectCloningService>().clone(context)!!
        }
        val minimizedProject = runBlocking {
            stage.executeStage(context)
        }.getOrNull()?.projectDir
        assertNotNull(minimizedProject)

        val minimizedFiles = minimizedProject!!.getAllFiles(minimizedProject.toNioPath())
        assertEquals(targetFiles, minimizedFiles)

        runBlocking(Dispatchers.EDT) {
            ProjectManagerEx.getInstanceEx().forceCloseProject(clonedContext.project)
        }

        DumbCompiler.targetPaths = null // it's important
    }
}