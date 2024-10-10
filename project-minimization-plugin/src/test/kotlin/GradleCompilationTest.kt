import arrow.core.Either
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runWithModalProgressBlocking
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.execution.IdeaCompilationException
import org.plan.research.minimization.plugin.model.CompilationResult
import org.plan.research.minimization.plugin.model.state.CompilationStrategy
import org.plan.research.minimization.plugin.services.BuildExceptionProviderService
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings
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
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult)
        val buildErrors = compilationResult.value.buildErrors
        assertSize(2, buildErrors)
        assertEquals("Unresolved reference: fsdfhjlksdfskjlhfkjhsldjklhdfgagjkhldfdkjlhfgahkjldfggdfjkhdfhkjldfvhkjfdvjhk", buildErrors[0].message)
        assertEquals("Unresolved reference: dfskjhl", buildErrors[1].message)

        val compilationResult2 = doCompilation(root, linkProject = false)
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult2)
        assertEquals(compilationResult.value, compilationResult2.value)
    }

    fun testWithFreshlyInitializedProjectSyntaxErrorK2() {
        val root = myFixture.copyDirectoryToProject("fresh-non-compilable", ".")
        copyGradle(useK2 = true)
        val compilationResult = doCompilation(root)
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult)
        val buildErrors = compilationResult.value.buildErrors
        assertSize(2, buildErrors)
        assertEquals("Unresolved reference 'fsdfhjlksdfskjlhfkjhsldjklhdfgagjkhldfdkjlhfgahkjldfggdfjkhdfhkjldfvhkjfdvjhk'.", buildErrors[0].message)
        assertEquals("Unresolved reference 'dfskjhl'.", buildErrors[1].message)

        val compilationResult2 = doCompilation(root, linkProject = false)
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult2)
        assertEquals(compilationResult.value, compilationResult2.value)
    }

    fun testKt71260() {
        // Example of Internal Error
        val root = myFixture.copyDirectoryToProject("kt-71260", ".")
        copyGradle(useBuildKts = false)
        val compilationResult = doCompilation(root)
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult)
        val buildErrors = compilationResult.value.buildErrors
        assertSize(1, buildErrors)
        assertEquals("org.jetbrains.kotlin.backend.common.CompilationException: Back-end: Please report this problem https://kotl.in/issue", buildErrors[0].message)

        val compilationResult2 = doCompilation(root, linkProject = false)
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult2)
        assertEquals(compilationResult.value, compilationResult2.value)
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
        checkGradle: Boolean = true,
        linkProject: Boolean = true,
    ): CompilationResult {
        if (linkProject) importGradleProject(root)
        if (checkGradle) assertGradleLoaded()

        val project = myFixture.project
        val propertyCheckerService = project.service<BuildExceptionProviderService>()
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