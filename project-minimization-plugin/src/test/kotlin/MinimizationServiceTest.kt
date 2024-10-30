import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.model.state.CompilationStrategy
import org.plan.research.minimization.plugin.model.state.DDStrategy
import org.plan.research.minimization.plugin.model.state.HierarchyCollectionStrategy
import org.plan.research.minimization.plugin.model.state.TransformationDescriptors
import org.plan.research.minimization.plugin.services.MinimizationService
import org.plan.research.minimization.plugin.services.ProjectCloningService
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.test.assertEquals

class MinimizationServiceTest : GradleProjectBaseTest() {

    override fun setUp() {
        super.setUp()
        project.service<MinimizationPluginSettings>().state.apply {
            currentCompilationStrategy = CompilationStrategy.GRADLE_IDEA
            minimizationTransformations.clear()
            minimizationTransformations.add(TransformationDescriptors.PATH_RELATIVIZATION)
            stages.clear()
            stages.add(FileLevelStage(
                hierarchyCollectionStrategy = HierarchyCollectionStrategy.FILE_TREE,
                ddAlgorithm = DDStrategy.DD_MIN
            ))
        }
        project.service<ProjectCloningService>().isTest = true
    }

    fun testKt71260() {
        // Example of Internal Error
        val root = myFixture.copyDirectoryToProject("kt-71260", ".")
        copyGradle(useBuildKts = false, copyProperties = false)

        runBlocking {
            importGradleProject(project)
            assertGradleLoaded(project)

            val expectedFiles = readAction { root.getAllFiles(root.toNioPath()).filterGradleAndBuildFiles() }

            val service = project.service<MinimizationService>()
            val minimizedProject = service.minimizeProject(project).await().getOrNull()
            assertNotNull(minimizedProject)
            minimizedProject!!

            val actualFiles = readAction {
                minimizedProject.projectDir.getAllFiles(minimizedProject.projectDir.toNioPath()).filterGradleAndBuildFiles()
            }

            if (minimizedProject is HeavyIJDDContext) {
                withContext(Dispatchers.EDT) {
                    ProjectManager.getInstance().closeAndDispose(minimizedProject.project)
                }
            }
            val filterFileName = setOf(
                Path("src/jsMain"),
                Path("src/jsTest"),
                Path("src/jvmTest"),
                Path("src/nativeMain"),
                Path("src/nativeTest"),
                Path("src/commonTest"),
            ).flatMap { listOf(it.resolve("kotlin"), it.resolve("resources"), it) }.toSet() +
                    setOf(Path("src/commonMain/resources"), Path("src/jvmMain/resources"))
            val filteredExpected = expectedFiles.filterNot {
                it.path in filterFileName
            }.toSet()

            assertEquals(
                filteredExpected.sortedBy { it.path },
                actualFiles.sortedBy { it.path },
                "Missing files: ${(filteredExpected - actualFiles).map(PathContent::path)}. Extra files: ${
                    (actualFiles - filteredExpected).map(PathContent::path)
                }"
            )
        }
    }
}