import arrow.core.Either
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runWithModalProgressBlocking
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.execution.IdeaCompilationException
import org.plan.research.minimization.plugin.execution.exception.KotlincErrorSeverity
import org.plan.research.minimization.plugin.execution.exception.KotlincException
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.exception.CompilationResult
import org.plan.research.minimization.plugin.model.state.CompilationStrategy
import org.plan.research.minimization.plugin.services.BuildExceptionProviderService
import org.plan.research.minimization.plugin.services.SnapshotManagerService
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings
import kotlin.io.path.name
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

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
        val buildErrors = compilationResult.value.kotlincExceptions
        assertSize(2, buildErrors)
        assertIs<List<KotlincException.GeneralKotlincException>>(buildErrors)
        assertEquals(
            "Unresolved reference: fsdfhjlksdfskjlhfkjhsldjklhdfgagjkhldfdkjlhfgahkjldfggdfjkhdfhkjldfvhkjfdvjhk",
            buildErrors[0].message
        )
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
        val buildErrors = compilationResult.value.kotlincExceptions
        assertSize(2, buildErrors)
        assertIs<List<KotlincException.GeneralKotlincException>>(buildErrors)
        assertEquals(
            "Unresolved reference 'fsdfhjlksdfskjlhfkjhsldjklhdfgagjkhldfdkjlhfgahkjldfggdfjkhdfhkjldfvhkjfdvjhk'.",
            buildErrors[0].message
        )
        assertEquals(KotlincErrorSeverity.ERROR, buildErrors[0].severity)
        assertEquals("Unresolved reference 'dfskjhl'.", buildErrors[1].message)
        assertEquals(KotlincErrorSeverity.ERROR, buildErrors[1].severity)

        val compilationResult2 = doCompilation(root, linkProject = false)
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult2)
        assertEquals(compilationResult.value, compilationResult2.value)
    }

    fun testWithFreshlyInitializedProjectSyntaxErrorMigrationK1K2() {
        val root = myFixture.copyDirectoryToProject("fresh-non-compilable", ".")
        copyGradle(useK2 = false)
        val compilationResult = doCompilation(root)
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult)

        val buildErrors = compilationResult.value.kotlincExceptions
        assertSize(2, buildErrors)
        assertIs<List<KotlincException.GeneralKotlincException>>(buildErrors)
        assertEquals(
            "Unresolved reference: fsdfhjlksdfskjlhfkjhsldjklhdfgagjkhldfdkjlhfgahkjldfggdfjkhdfhkjldfvhkjfdvjhk",
            buildErrors[0].message
        )
        assertEquals(KotlincErrorSeverity.ERROR, buildErrors[0].severity)
        assertEquals("Unresolved reference: dfskjhl", buildErrors[1].message)
        assertEquals(KotlincErrorSeverity.ERROR, buildErrors[1].severity)

        copyGradle(useK2 = true)
        val compilationResult2 = doCompilation(root, linkProject = false)
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult2)

        val buildErrors2 = compilationResult2.value.kotlincExceptions
        assertIs<List<KotlincException.GeneralKotlincException>>(buildErrors2)
        assertEquals(
            "Unresolved reference 'fsdfhjlksdfskjlhfkjhsldjklhdfgagjkhldfdkjlhfgahkjldfggdfjkhdfhkjldfvhkjfdvjhk'.",
            buildErrors2[0].message
        )
        assertEquals(KotlincErrorSeverity.ERROR, buildErrors2[0].severity)
        assertEquals("Unresolved reference 'dfskjhl'.", buildErrors2[1].message)
        assertEquals(KotlincErrorSeverity.ERROR, buildErrors2[1].severity)

        assertNotEquals(compilationResult.value, compilationResult2.value)
    }

    fun testKt71260() {
        // Example of Internal Error
        val root = myFixture.copyDirectoryToProject("kt-71260", ".")
        copyGradle(useBuildKts = false)
        val compilationResult = doCompilation(root)
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult)
        val buildErrors = compilationResult.value.kotlincExceptions
        assertIs<List<KotlincException.BackendCompilerException>>(buildErrors)
        assertSize(1, buildErrors)
        assertEquals("Case2.kt", buildErrors[0].position.filePath.name)
        assert(buildErrors[0].stacktrace.isNotBlank())

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

    fun testInterProjectEquals() {
        val root = myFixture.copyDirectoryToProject("kt-71260", ".")
        copyGradle(useBuildKts = false)
        val compilationResult = doCompilation(root)
        val snapshottingService = myFixture.project.service<SnapshotManagerService>()
        val snapshot = runWithModalProgressBlocking(myFixture.project, "") {
            snapshottingService.transaction<Nothing>(IJDDContext(project)) {
                it
            }.getOrNull()
        }
        assertNotNull(snapshot)
        val compilationResult2 = getCompilationResult(snapshot!!.project)
        assertNotEquals(compilationResult, compilationResult2)
    }

    fun testAnalysisProject() {
        val root = myFixture.copyDirectoryToProject("analysis-error", ".")
        copyGradle(useBuildKts = false)
        val compilationResult = doCompilation(root)

        assertIs<Either.Right<IdeaCompilationException>>(compilationResult)
        val buildErrors = compilationResult.value.kotlincExceptions
        assertIs<List<KotlincException.GenericInternalCompilerException>>(buildErrors)
        assertSize(1, buildErrors)
        assert(buildErrors[0].stacktrace.isNotBlank())
        assert(buildErrors[0].stacktrace.lines().all { it.startsWith("\tat")})
        assert(buildErrors[0].message.startsWith("While analysing "))
        assert(buildErrors[0].message.endsWith("java.lang.IllegalArgumentException: Failed requirement."))

        val compilationResult2 = doCompilation(root, linkProject = false)
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult2)
        assertEquals(compilationResult.value, compilationResult2.value)
    }

    private fun doCompilation(
        root: VirtualFile,
        checkGradle: Boolean = true,
        linkProject: Boolean = true,
    ): CompilationResult = runBlocking {
        if (linkProject) importGradleProject(root)
        if (checkGradle) assertGradleLoaded()
        return getCompilationResult(myFixture.project)
    }

    private fun getCompilationResult(project: Project): CompilationResult {
        val propertyCheckerService = myFixture.project.service<BuildExceptionProviderService>()
        propertyCheckerService.checkCompilation(project)
    }
}