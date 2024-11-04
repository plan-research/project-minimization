import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.plan.research.minimization.plugin.model.FileLevelStage
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
            minimizationTransformations = mutableListOf(TransformationDescriptors.PATH_RELATIVIZATION)
            stages = mutableListOf(
                FileLevelStage(
                    hierarchyCollectionStrategy = HierarchyCollectionStrategy.FILE_TREE,
                    ddAlgorithm = DDStrategy.DD_MIN
                ),
                // TODO: JBRes-1977
//                FunctionLevelStage(
//                    ddAlgorithm = DDStrategy.DD_MIN
//                )
            )
        }
        project.service<ProjectCloningService>().isTest = true
    }

    fun testKt71260() {
        return // TODO: JBRes-1977 & JBRes-1799
        // Example of Internal Error
        val root = myFixture.copyDirectoryToProject("kt-71260", ".")
        copyGradle(useBuildKts = false, copyProperties = false)

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
            val filterFileName = setOf(
                Path("src/jsMain"),
                Path("src/jsTest"),
                Path("src/jvmTest"),
                Path("src/nativeMain"),
                Path("src/nativeTest"),
                Path("src/commonTest"),
            ).flatMap { listOf(it.resolve("kotlin"), it.resolve("resources"), it) }.toSet() +
                    setOf(
                        Path("src/commonMain/resources"),
                        Path("src/jvmMain/resources"),
                        Path("src/commonMain/kotlin/Unused.kt")
                    )
            val filteredExpected = expectedFiles.filterNot {
                it.path in filterFileName
            }.toMutableSet()

            val case1Content = filteredExpected.find { it.path.name == "Case1.kt" }!!
            filteredExpected.remove(case1Content)
            filteredExpected.add(
                case1Content.copy(
                    content = case1Content.content?.replace(
                        ORIGINAL_FUNCTION_TEXT,
                        REPLACED_FUNCTION_TEXT
                    )
                )
            )

            assertEquals(
                filteredExpected.sortedBy { it.path },
                actualFiles.sortedBy { it.path },
                "Missing files: ${(filteredExpected - actualFiles).map(PathContent::path)}. Extra files: ${
                    (actualFiles - filteredExpected).map(PathContent::path)
                }"
            )
        }
    }

    companion object {
        private const val ORIGINAL_FUNCTION_TEXT = "fun unused2() {\n" +
                "    println(\"This function is unused too, but stored in the used file. So, it will be deleted by stage 2\")\n" +
                "}"
        private const val REPLACED_FUNCTION_TEXT = "fun unused2() {\n" +
                "    TODO(\"Removed by DD\")\n" +
                "}"
    }
}