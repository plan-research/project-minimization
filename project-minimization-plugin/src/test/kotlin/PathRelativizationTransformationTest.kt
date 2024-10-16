import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.plugin.execution.exception.KotlincErrorSeverity
import org.plan.research.minimization.plugin.execution.exception.KotlincException
import org.plan.research.minimization.plugin.execution.transformer.PathRelativizationTransformation
import org.plan.research.minimization.plugin.model.CaretPosition
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

class PathRelativizationTransformationTest : JavaCodeInsightFixtureTestCase() {
    fun testGenericKotlinCompilationError() {
        val project = myFixture.project
        val projectLocation = project.guessProjectDir()!!.toNioPath()
        val transformation = PathRelativizationTransformation(project)

        val genericKotlinError = KotlincException.GeneralKotlincException(
            position = CaretPosition(
                lineNumber = 1,
                columnNumber = 1,
                filePath = projectLocation.resolve("Test.kt")
            ),
            message = "Unresolved reference: good compiler",
            severity = KotlincErrorSeverity.ERROR
        )
        val transformedError = runBlocking { genericKotlinError.transformBy(transformation).getOrNull() }
        kotlin.test.assertNotNull(transformedError)
        assert(!transformedError.message.contains(projectLocation.toString()))
        assert(!transformedError.position.filePath.startsWith(projectLocation))
        assert(transformedError.position.filePath.startsWith("Test.kt"))
        assertEquals(1, transformedError.position.lineNumber)
        assertEquals(1, transformedError.position.columnNumber)

        val snapshotDir = project.service<MinimizationPluginSettings>().state.temporaryProjectLocation!!
        val snapshotLocation = projectLocation.resolve(snapshotDir)

        val genericErrorInSnapshot = genericKotlinError.copy(
            position = genericKotlinError.position.copy(
                filePath = snapshotLocation.resolve("snapshot").resolve("Test.kt")
            )
        )

        val transformedInSnapshot = runBlocking { genericErrorInSnapshot.transformBy(transformation).getOrNull() }
        kotlin.test.assertNotNull(transformedInSnapshot)
        assert(!transformedInSnapshot.message.contains(projectLocation.toString()))
        assert(!transformedInSnapshot.position.filePath.startsWith(snapshotLocation))
        assert(!transformedInSnapshot.position.filePath.startsWith(projectLocation))
        assert(transformedInSnapshot.position.filePath.startsWith("Test.kt"))
        assertEquals(1, transformedInSnapshot.position.lineNumber)
        assertEquals(1, transformedInSnapshot.position.columnNumber)
    }

    fun testBackendCompilationError() {
        val project = myFixture.project
        val projectLocation = project.guessProjectDir()!!.toNioPath()
        val transformation = PathRelativizationTransformation(project)

        val rootException = KotlincException.BackendCompilerException(
            position = CaretPosition(
                lineNumber = 1,
                columnNumber = 1,
                filePath = projectLocation.resolve("Test.kt")
            ),
            stacktrace = "some stacktrace",
            additionalMessage = "Details: Internal error in file lowering: java.lang.IllegalStateException: should not be called"
        )
        val transformed = runBlocking { rootException.transformBy(transformation).getOrNull() }
        kotlin.test.assertNotNull(transformed)
        assert(transformed.additionalMessage?.contains(projectLocation.toString()) != true)
        assert(!transformed.position.filePath.startsWith(projectLocation))
        assert(transformed.position.filePath.startsWith("Test.kt"))
        assertEquals(1, transformed.position.lineNumber)
        assertEquals(1, transformed.position.columnNumber)

        val snapshotDir = project.service<MinimizationPluginSettings>().state.temporaryProjectLocation!!
        val snapshotLocation = projectLocation.resolve(snapshotDir)

        val exceptionInSnapshot = rootException.copy(
            position = rootException.position.copy(
                filePath = snapshotLocation.resolve("snapshot").resolve("Test.kt")
            )
        )
        val transformed2 = runBlocking { exceptionInSnapshot.transformBy(transformation).getOrNull() }
        kotlin.test.assertNotNull(transformed2)
        assert(transformed2.additionalMessage?.contains(projectLocation.toString()) != true)
        assert(!transformed2.position.filePath.startsWith(projectLocation))
        assert(transformed2.position.filePath.startsWith("Test.kt"))
        assertEquals(1, transformed2.position.lineNumber)
        assertEquals(1, transformed2.position.columnNumber)
    }

    fun testGenericCompilationException() {
        val project = myFixture.project
        val projectLocation = project.guessProjectDir()!!.toNioPath()
        val transformation = PathRelativizationTransformation(project)

        val rootException = KotlincException.GenericInternalCompilerException(
            stacktrace = "some tracktrace",
            message = "While analysing ${projectLocation.resolve("Test.kt")}:19:3: java.lang.IllegalArgumentException: Failed requirement."
        )
        val transformed = runBlocking { rootException.transformBy(transformation).getOrNull() }
        kotlin.test.assertNotNull(transformed)
        assert(!transformed.message.contains(projectLocation.toString()))

        val snapshotDir = project.service<MinimizationPluginSettings>().state.temporaryProjectLocation!!
        val snapshotLocation = projectLocation.resolve(snapshotDir)
        val snapshotException = KotlincException.GenericInternalCompilerException(
            stacktrace = "some tracktrace",
            message = "While analysing ${
                snapshotLocation.resolve("funny-snapshot").resolve("Test.kt")
            }:19:3: java.lang.IllegalArgumentException: Failed requirement."
        )
        val transformed2 = runBlocking { snapshotException.transformBy(transformation).getOrNull() }
        kotlin.test.assertNotNull(transformed2)
        assert(!transformed2.message.contains(projectLocation.toString()))
    }
}