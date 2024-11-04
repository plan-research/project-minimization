import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.plugin.execution.DumbCompiler
import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.state.CompilationStrategy
import org.plan.research.minimization.plugin.model.state.DDStrategy
import org.plan.research.minimization.plugin.model.state.HierarchyCollectionStrategy
import org.plan.research.minimization.plugin.services.MinimizationStageExecutorService
import org.plan.research.minimization.plugin.services.ProjectCloningService
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

class FileLevelStageTest : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/fileTreeHierarchy"
    }

    override fun runInDispatchThread(): Boolean = false

    override fun setUp() {
        super.setUp()
        var compilationStrategy by project.service<MinimizationPluginSettings>().state.compilationStrategy.mutable()
        compilationStrategy = CompilationStrategy.DUMB
        val transformations by project.service<MinimizationPluginSettings>().state.minimizationTransformations.mutable()
        transformations.clear()
        project.service<ProjectCloningService>().isTest = true
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
        val executor = project.service<MinimizationStageExecutorService>()
        val stage = FileLevelStage(
            HierarchyCollectionStrategy.FILE_TREE,
            DDStrategy.PROBABILISTIC_DD
        )

        val targetFiles = if (targetPaths == null) {
            project.getAllFiles()
        } else {
            targetPaths.map { root.findFileByRelativePath(it)!! }
                .getAllFiles(project) + root.getPathContentPair(project)
        }

        val minimizedProject = runBlocking {
            val clonedProject = project.service<ProjectCloningService>().clone(project)!!
            val context = IJDDContext(clonedProject, project)
            stage.apply(context, executor)
        }.getOrNull()?.project
        assertNotNull(minimizedProject)

        val minimizedFiles = minimizedProject!!.getAllFiles()
        assertEquals(targetFiles, minimizedFiles)

        runBlocking(Dispatchers.EDT) {
            ProjectManager.getInstance().closeAndDispose(minimizedProject)
        }

        DumbCompiler.targetPaths = null // it's important
    }
}