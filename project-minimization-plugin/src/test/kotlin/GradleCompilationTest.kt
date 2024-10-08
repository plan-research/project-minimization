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
        copyGradle()
        val compilationResult = doCompilation(root)
        assertIs<Either.Left<*>>(compilationResult)
        assertEquals(CompilationPropertyCheckerError.CompilationSuccess, compilationResult.value)
    }
    fun testWithFreshlyInitializedProjectK2() {
        val root = myFixture.copyDirectoryToProject("fresh", ".")
        copyGradle(useK2 = true)
        val compilationResult = doCompilation(root)
        assertIs<Either.Left<*>>(compilationResult)
        assertEquals(CompilationPropertyCheckerError.CompilationSuccess, compilationResult.value)
    }

    fun testWithFreshlyInitializedProjectSyntaxError() {
        val root = myFixture.copyDirectoryToProject("fresh-non-compilable", ".")
        copyGradle()
        val compilationResult = doCompilation(root)
        assertIs<Either.Right<*>>(compilationResult)
    }
    fun testWithFreshlyInitializedProjectSyntaxErrorK2() {
        val root = myFixture.copyDirectoryToProject("fresh-non-compilable", ".")
        copyGradle(useK2 = true)
        val compilationResult = doCompilation(root)
        assertIs<Either.Right<*>>(compilationResult)
    }

    fun testKt71260() {
        // Example of Internal Error
        val root = myFixture.copyDirectoryToProject("kt-71260", ".")
        copyGradle(useBuildKts = false)
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

    private fun doCompilation(
        root: VirtualFile,
        checkGradle: Boolean = true
    ): Either<CompilationPropertyCheckerError, Throwable> {
        importGradleProject(root)
        if (checkGradle) assertGradleLoaded()

        val project = myFixture.project
        val propertyCheckerService = project.service<CompilationPropertyCheckerService>()
        return runWithModalProgressBlocking(project, "") { propertyCheckerService.checkCompilation(project) }
    }

    private fun copyGradle(useK2: Boolean = false, useBuildKts: Boolean = true) {
        myFixture.copyDirectoryToProject("core/gradle", "gradle")
        myFixture.copyFileToProject("core/gradle.properties", "gradle.properties")
        myFixture.copyFileToProject("core/settings.gradle.kts", "settings.gradle.kts")
        if (useBuildKts) {
            if (!useK2)
                myFixture.copyFileToProject("core/build.gradle.kts", "build.gradle.kts")
            else
                myFixture.copyFileToProject("core/build.gradle.kts.2", "build.gradle.kts")
        }
        myFixture.copyFileToProject("core/gradlew", "gradlew")
        myFixture.copyFileToProject("core/gradlew.bat", "gradlew.bat")
    }
}