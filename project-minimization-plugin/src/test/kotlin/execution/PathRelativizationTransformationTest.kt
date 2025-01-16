package execution

import HeavyTestContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.plugin.execution.exception.KotlincErrorSeverity
import org.plan.research.minimization.plugin.execution.exception.KotlincException
import org.plan.research.minimization.plugin.execution.transformer.PathRelativizationTransformation
import org.plan.research.minimization.plugin.model.CaretPosition
import org.plan.research.minimization.plugin.services.ProjectCloningService
import org.plan.research.minimization.plugin.services.ProjectOpeningService

class PathRelativizationTransformationTest : JavaCodeInsightFixtureTestCase() {

    override fun setUp() {
        super.setUp()
        service<ProjectOpeningService>().isTest = true
    }

    override fun runInDispatchThread(): Boolean = false

    fun testGenericKotlinCompilationError() {
        val project = myFixture.project
        val projectLocation = project.guessProjectDir()!!.toNioPath()
        val transformation = PathRelativizationTransformation()
        val context = HeavyTestContext(project)

        val genericKotlinError = KotlincException.GeneralKotlincException(
            position = CaretPosition(
                lineNumber = 1,
                columnNumber = 1,
                filePath = projectLocation.resolve("Test.kt")
            ),
            message = "Unresolved reference: good compiler",
            severity = KotlincErrorSeverity.ERROR
        )
        val transformedError = runBlocking { genericKotlinError.apply(transformation, context) }
        assert(!transformedError.message.contains(projectLocation.toString()))
        assertNotNull(transformedError.position)
        assert(!transformedError.position!!.filePath.startsWith(projectLocation))
        assert(transformedError.position!!.filePath.startsWith("Test.kt"))
        assertEquals(1, transformedError.position!!.lineNumber)
        assertEquals(1, transformedError.position!!.columnNumber)

        val snapshot = runBlocking { project.service<ProjectCloningService>().clone(context)!! }
        val snapshotLocation = snapshot.projectDir.toNioPath()

        val genericErrorInSnapshot = genericKotlinError.copy(
            position = genericKotlinError.position!!.copy(
                filePath = snapshotLocation.resolve("Test.kt")
            )
        )

        val transformedInSnapshot = runBlocking { genericErrorInSnapshot.apply(transformation, snapshot) }
        kotlin.test.assertNotNull(transformedInSnapshot)
        assert(!transformedInSnapshot.message.contains(projectLocation.toString()))
        assertNotNull(transformedInSnapshot.position)
        assert(!transformedInSnapshot.position!!.filePath.startsWith(snapshotLocation))
        assert(!transformedInSnapshot.position!!.filePath.startsWith(projectLocation))
        assert(transformedInSnapshot.position!!.filePath.startsWith("Test.kt"))
        assertEquals(1, transformedInSnapshot.position!!.lineNumber)
        assertEquals(1, transformedInSnapshot.position!!.columnNumber)
        runBlocking(Dispatchers.EDT) { ProjectManager.getInstance().closeAndDispose(snapshot.project) }
    }

    fun testBackendCompilationError() {
        val project = myFixture.project
        val projectLocation = project.guessProjectDir()!!.toNioPath()
        val transformation = PathRelativizationTransformation()
        val context = HeavyTestContext(project)

        val rootException = KotlincException.BackendCompilerException(
            position = CaretPosition(
                lineNumber = 1,
                columnNumber = 1,
                filePath = projectLocation.resolve("Test.kt")
            ),
            stacktrace = "some stacktrace",
            additionalMessage = "Details: Internal error in file lowering: java.lang.IllegalStateException: should not be called"
        )
        val transformed = runBlocking { rootException.apply(transformation, context) }
        kotlin.test.assertNotNull(transformed)
        assert(transformed.additionalMessage?.contains(projectLocation.toString()) != true)
        assert(!transformed.position.filePath.startsWith(projectLocation))
        assert(transformed.position.filePath.startsWith("Test.kt"))
        assertEquals(1, transformed.position.lineNumber)
        assertEquals(1, transformed.position.columnNumber)

        val snapshot = runBlocking { project.service<ProjectCloningService>().clone(context)!! }
        val snapshotLocation = snapshot.projectDir.toNioPath()

        val exceptionInSnapshot = rootException.copy(
            position = rootException.position.copy(
                filePath = snapshotLocation.resolve("Test.kt")
            )
        )
        val transformed2 = runBlocking { exceptionInSnapshot.apply(transformation, snapshot) }
        kotlin.test.assertNotNull(transformed2)
        assert(transformed2.additionalMessage?.contains(projectLocation.toString()) != true)
        assert(!transformed2.position.filePath.startsWith(projectLocation))
        assert(transformed2.position.filePath.startsWith("Test.kt"))
        assertEquals(1, transformed2.position.lineNumber)
        assertEquals(1, transformed2.position.columnNumber)
        runBlocking(Dispatchers.EDT) { ProjectManager.getInstance().closeAndDispose(snapshot.project) }
    }

    fun testGenericCompilationException() {
        val project = myFixture.project
        val projectLocation = project.guessProjectDir()!!.toNioPath()
        val transformation = PathRelativizationTransformation()
        val context = HeavyTestContext(project)

        val rootException = KotlincException.GenericInternalCompilerException(
            stacktrace = "some tracktrace",
            message = "While analysing ${projectLocation.resolve("Test.kt")}:19:3: java.lang.IllegalArgumentException: Failed requirement."
        )
        val transformed = runBlocking { rootException.apply(transformation, context) }
        kotlin.test.assertNotNull(transformed)
        assert(!transformed.message.contains(projectLocation.toString()))

        val snapshot = runBlocking { project.service<ProjectCloningService>().clone(context)!! }
        val snapshotLocation = snapshot.projectDir.toNioPath()

        val snapshotException = KotlincException.GenericInternalCompilerException(
            stacktrace = "some tracktrace",
            message = "While analysing ${
                snapshotLocation.resolve("Test.kt")
            }:19:3: java.lang.IllegalArgumentException: Failed requirement."
        )
        val transformed2 = runBlocking { snapshotException.apply(transformation, snapshot) }
        kotlin.test.assertNotNull(transformed2)
        assert(!transformed2.message.contains(projectLocation.toString()))
        runBlocking(Dispatchers.EDT) { ProjectManager.getInstance().closeAndDispose(snapshot.project) }
    }
}