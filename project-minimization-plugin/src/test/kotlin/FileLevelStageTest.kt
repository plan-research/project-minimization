import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.plugin.execution.DumbCompiler
import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.LightIJDDContext
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
        project.service<MinimizationPluginSettings>().state.currentCompilationStrategy = CompilationStrategy.DUMB
        project.service<MinimizationPluginSettings>().state.minimizationTransformations.clear()
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
        val context = LightIJDDContext(project)

        val targetFiles = if (targetPaths == null) {
            project.getAllFiles()
        } else {
            targetPaths.map { root.findFileByRelativePath(it)!! }
                .getAllFiles(context.projectDir) + root.getPathContentPair(context.projectDir.toNioPath())
        }

        val minimizedProject = runBlocking {
            val clonedContext = project.service<ProjectCloningService>().clone(context)!!
            stage.apply(clonedContext, executor)
        }.getOrNull()?.projectDir
        assertNotNull(minimizedProject)

        val minimizedFiles = minimizedProject!!.getAllFiles(minimizedProject.toNioPath())
        assertEquals(targetFiles, minimizedFiles)

        DumbCompiler.targetPaths = null // it's important
    }
}