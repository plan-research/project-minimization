package gradle

import HeavyTestContext
import LightTestContext
import TestWithContext
import TestWithHeavyContext
import TestWithLightContext
import arrow.core.Either
import com.intellij.openapi.components.service
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.plugin.compilation.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.compilation.exception.IdeaCompilationException
import org.plan.research.minimization.plugin.compilation.comparator.SimpleExceptionComparator
import org.plan.research.minimization.plugin.compilation.comparator.StacktraceExceptionComparator
import org.plan.research.minimization.plugin.compilation.exception.KotlincErrorSeverity
import org.plan.research.minimization.plugin.compilation.exception.KotlincException
import org.plan.research.minimization.plugin.compilation.transformer.PathRelativizationTransformer
import org.plan.research.minimization.plugin.compilation.CompilationResult
import org.plan.research.minimization.plugin.context.HeavyIJDDContext
import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.context.IJDDContextBase
import org.plan.research.minimization.plugin.settings.data.CompilationStrategy
import org.plan.research.minimization.plugin.services.BuildExceptionProviderService
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import org.plan.research.minimization.plugin.services.ProjectCloningService
import org.plan.research.minimization.plugin.services.ProjectOpeningService
import kotlin.io.path.name
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

abstract class GradleCompilationTest<C : IJDDContextBase<C>> : GradleProjectBaseTest(), TestWithContext<C> {
    override fun setUp() {
        super.setUp()
        project.service<MinimizationPluginSettings>()
            .stateObservable.compilationStrategy.set(CompilationStrategy.GRADLE_IDEA)
        service<ProjectOpeningService>().isTest = true
    }

    fun testWithFreshlyInitializedProject() {
        myFixture.copyDirectoryToProject("fresh", ".")
        copyGradle()
        val context = createContext(project)
        val compilationResult = doCompilation(context)
        assertIs<Either.Left<*>>(compilationResult)
        assertEquals(CompilationPropertyCheckerError.CompilationSuccess, compilationResult.value)
    }

    fun testWithFreshlyInitializedProjectK2() {
        myFixture.copyDirectoryToProject("fresh", ".")
        copyGradle(useK2 = true)
        val context = createContext(project)
        val compilationResult = doCompilation(context)
        assertIs<Either.Left<*>>(compilationResult)
        assertEquals(CompilationPropertyCheckerError.CompilationSuccess, compilationResult.value)
    }

    fun testWithFreshlyInitializedProjectSyntaxError() {
        myFixture.copyDirectoryToProject("fresh-non-compilable", ".")
        copyGradle()
        val context = createContext(project)
        val compilationResult = doCompilation(context)
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult)
        val buildErrors = compilationResult.value.kotlincExceptions
        assertSize(2, buildErrors)
        assertIs<List<KotlincException.GeneralKotlincException>>(buildErrors)
        assertEquals(
            "Unresolved reference: fsdfhjlksdfskjlhfkjhsldjklhdfgagjkhldfdkjlhfgahkjldfggdfjkhdfhkjldfvhkjfdvjhk",
            buildErrors[0].message
        )
        assertEquals("Unresolved reference: dfskjhl", buildErrors[1].message)

        val compilationResult2 = doCompilation(context, linkProject = false)
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult2)
        assertEquals(compilationResult.value, compilationResult2.value)
    }

    fun testWithFreshlyInitializedProjectSyntaxErrorK2() {
        myFixture.copyDirectoryToProject("fresh-non-compilable", ".")
        copyGradle(useK2 = true)
        val context = createContext(project)
        val compilationResult = doCompilation(context)
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

        val compilationResult2 = doCompilation(context, linkProject = false)
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult2)
        assertEquals(compilationResult.value, compilationResult2.value)
    }

    fun testWithFreshlyInitializedProjectSyntaxErrorMigrationK1K2() {
        myFixture.copyDirectoryToProject("fresh-non-compilable", ".")
        copyGradle(useK2 = false)
        val context = createContext(project)
        val compilationResult = doCompilation(context)
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
        val compilationResult2 = doCompilation(context, linkProject = false)
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
        myFixture.copyDirectoryToProject("kt-71260", ".")
        copyGradle(useBuildKts = false)
        val context = createContext(project)
        val compilationResult = doCompilation(context)
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult)
        val buildErrors = compilationResult.value.kotlincExceptions
        assertIs<List<KotlincException.BackendCompilerException>>(buildErrors)
        assertSize(1, buildErrors)
        assertEquals("Case2.kt", buildErrors[0].position.filePath.name)
        assert(buildErrors[0].stacktrace.isNotBlank())

        val compilationResult2 = doCompilation(context, linkProject = false)
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult2)
        assertEquals(compilationResult.value, compilationResult2.value)
    }

    fun testMavenProject() {
        myFixture.copyDirectoryToProject("maven-project", ".")
        val context = createContext(project)
        val compilationResult = doCompilation(context, checkGradle = false)
        assertIs<Either.Left<*>>(compilationResult)
        assertEquals(CompilationPropertyCheckerError.InvalidBuildSystem, compilationResult.value)
    }

    fun testEmptyProject() {
        val context = createContext(project)
        val compilationResult = doCompilation(context, checkGradle = false)
        assertIs<Either.Left<*>>(compilationResult)
        assertEquals(CompilationPropertyCheckerError.InvalidBuildSystem, compilationResult.value)
    }

    fun testInterProjectEquals() {
        disableDeduplication()
        myFixture.copyDirectoryToProject("kt-71260", ".")
        copyGradle(useBuildKts = false)
        val context = createContext(project)
        val compilationResult = doCompilation(context)
        val cloningService = myFixture.project.service<ProjectCloningService>()
        val snapshot = runBlocking {
            cloningService.clone(context)
        }
        kotlin.test.assertNotNull(snapshot)
        val compilationResult2 = doCompilation(snapshot, checkGradle = false)

        assertNotEquals(compilationResult, compilationResult2)
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult)
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult2)

        val transformer = PathRelativizationTransformer()
        val transformedResults = runBlocking {
            listOf(
                compilationResult.value.apply(transformer, context),
                compilationResult2.value.apply(transformer, snapshot)
            )
        }
        val compartor = StacktraceExceptionComparator(SimpleExceptionComparator())
        runBlocking {
            assertTrue(compartor.areEquals(transformedResults[0], transformedResults[1]))
        }

        deleteContext(snapshot)
    }

    fun testAnalysisProject() {
        myFixture.copyDirectoryToProject("analysis-error", ".")
        copyGradle(useBuildKts = false)
        val context = createContext(project)
        val compilationResult = doCompilation(context)

        assertIs<Either.Right<IdeaCompilationException>>(compilationResult)
        val buildErrors = compilationResult.value.kotlincExceptions
        assertIs<List<KotlincException.GenericInternalCompilerException>>(buildErrors)
        assertSize(1, buildErrors)
        assert(buildErrors[0].stacktrace!!.isNotBlank())
        assert(buildErrors[0].stacktrace!!.lines().all { it.startsWith("\tat") })
        assert(buildErrors[0].message.startsWith("While analysing "))
        assert(buildErrors[0].message.endsWith("java.lang.IllegalArgumentException: Failed requirement."))

        val compilationResult2 = doCompilation(context, linkProject = false)
        assertIs<Either.Right<IdeaCompilationException>>(compilationResult2)
        assertEquals(compilationResult.value, compilationResult2.value)
    }

    private fun doCompilation(
        context: IJDDContext,
        checkGradle: Boolean = true,
        linkProject: Boolean = true,
    ): CompilationResult = runBlocking {
        if (context is HeavyIJDDContext<*>) {
            if (linkProject) importGradleProject(context.project)
            if (checkGradle) assertGradleLoaded(context.project)
        }
        getCompilationResult(context)
    }

    private suspend fun getCompilationResult(context: IJDDContext): CompilationResult {
        val propertyCheckerService = myFixture.project.service<BuildExceptionProviderService>()
        return propertyCheckerService.checkCompilation(context)
    }
}

class GradleCompilationHeavyTest :
    GradleCompilationTest<HeavyTestContext>(),
    TestWithContext<HeavyTestContext> by TestWithHeavyContext()

class GradleCompilationLightTest :
    GradleCompilationTest<LightTestContext>(),
    TestWithContext<LightTestContext> by TestWithLightContext()
