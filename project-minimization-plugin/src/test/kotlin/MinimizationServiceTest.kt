import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.plan.research.minimization.plugin.model.state.CompilationStrategy
import org.plan.research.minimization.plugin.services.MinimizationService
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

class MinimizationServiceTest : GradleProjectBaseTest() {

    override fun setUp() {
        super.setUp()
        project.service<MinimizationPluginSettings>().state.apply {
            currentCompilationStrategy = CompilationStrategy.GRADLE_IDEA
        }
    }

    fun testKt71260() {
        // Example of Internal Error
        val root = myFixture.copyDirectoryToProject("kt-71260", ".")
        copyGradle(useBuildKts = false)

        runBlocking {
            importGradleProject(root)
            assertGradleLoaded()

            val expectedFiles = readAction { root.getAllFiles(project).filterGradleAndBuildFiles() }

            val service = project.service<MinimizationService>()
            val minimizedProject = service.minimizeProject(project).await().getOrNull()
            assertNotNull(minimizedProject)
            minimizedProject!!

            val actualFiles = readAction {
                minimizedProject.guessProjectDir()!!.getAllFiles(minimizedProject).filterGradleAndBuildFiles()
            }

            withContext(Dispatchers.EDT) {
                ProjectManager.getInstance().closeAndDispose(minimizedProject)
            }

            assertEquals(expectedFiles, actualFiles)
        }
    }
}