import arrow.core.Either
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runWithModalProgressBlocking
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.model.state.CompilationStrategy
import org.plan.research.minimization.plugin.services.CompilationPropertyCheckerService
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings
import kotlin.test.assertContains
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
    fun testKt71260() {
        // Example of Internal Error
        val root = myFixture.copyDirectoryToProject("kt-71260", ".")
        val compilationResult = doCompilation(root)
        assertIs<Either.Right<Throwable>>(compilationResult)
        assertContains(compilationResult.value.message!!, "Details: Internal error in file lowering")
    }
    fun testMavenProject() {
        val root = myFixture.copyDirectoryToProject("maven-project", ".")
        val compilationResult = doCompilation(root, checkGradle = false)
        assertIs<Either.Left<*>>(compilationResult)
        assertEquals(CompilationPropertyCheckerError.InvalidBuildSystem, compilationResult.value)
    }
    fun testEmptyProject() {
        val compilationResult = doCompilation(project.guessProjectDir()!!, checkGradle = false)
        assertIs<Either.Left<*>>(compilationResult)
        assertEquals(CompilationPropertyCheckerError.InvalidBuildSystem, compilationResult.value)
    }

    private fun doCompilation(root: VirtualFile, checkGradle: Boolean = true): Either<CompilationPropertyCheckerError, Throwable> {
        importGradleProject(root)
        if (checkGradle) assertGradleLoaded()

        val project = myFixture.project
        val propertyCheckerService = project.service<CompilationPropertyCheckerService>()
        return runWithModalProgressBlocking(project, "") { propertyCheckerService.checkCompilation(project) }
    }
}