import arrow.core.Either
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runWithModalProgressBlocking
import com.intellij.openapi.vfs.VirtualFile
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.model.state.CompilationStrategy
import org.plan.research.minimization.plugin.services.CompilationPropertyCheckerService
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings
import kotlin.test.assertIs

class GradleCompilationTest : GradleProjectBaseTest() {
    override fun setUp() {
        super.setUp()
        project.service<MinimizationPluginSettings>().state.currentCompilationStrategy = CompilationStrategy.GRADLE_IDEA
    }

    fun testWithFreshlyInitializedProject() {
        val root = myFixture.copyDirectoryToProject("fresh", ".")
        val compilationResult = doCompilation(root)
        assertIs<Either.Left<*>>(compilationResult)
        assertEquals(CompilationPropertyCheckerError.CompilationSuccess, compilationResult.value)
    }
    fun testWithFreshlyInitializedProjectSyntaxError() {
        val root =  myFixture.copyDirectoryToProject("fresh-non-compilable", ".")
        val compilationResult = doCompilation(root)
        assertIs<Either.Right<*>>(compilationResult)
    }

    private fun doCompilation(root: VirtualFile): Either<CompilationPropertyCheckerError, Throwable> {
        importGradleProject(root)
        assertGradleLoaded()

        val project = myFixture.project
        val propertyCheckerService = project.service<CompilationPropertyCheckerService>()
        return runWithModalProgressBlocking(project, "") { propertyCheckerService.checkCompilation(project) }
    }
}