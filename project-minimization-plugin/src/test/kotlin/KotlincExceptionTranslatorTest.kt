import arrow.core.Either
import com.intellij.build.FilePosition
import com.intellij.build.events.FileMessageEvent
import com.intellij.build.events.FileMessageEventResult
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.MessageEvent.Kind
import com.intellij.build.events.MessageEventResult
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError.CompilationSuccess
import org.plan.research.minimization.plugin.execution.exception.KotlincErrorSeverity
import org.plan.research.minimization.plugin.execution.exception.KotlincException
import org.plan.research.minimization.plugin.execution.exception.KotlincExceptionTranslator
import org.plan.research.minimization.plugin.model.CaretPosition
import java.io.File
import kotlin.io.path.Path
import kotlin.test.assertIs

class KotlincExceptionTranslatorTest : JavaCodeInsightFixtureTestCase() {
    private open class MockBuildEvent(
        private val message: String,
        private val description: String,
        private val kind: Kind
    ) : MessageEvent {
        override fun getId() = "testBuildeEvent"

        override fun getParentId(): Any? = null

        override fun getEventTime(): Long = 0

        override fun getMessage(): String = message

        override fun getHint() = null

        override fun getDescription(): String? = description.takeIf { it.isNotBlank() }

        override fun getKind(): Kind = kind

        override fun getGroup(): String = "TestGroup"

        override fun getNavigatable(project: Project): Navigatable? = null

        override fun getResult(): MessageEventResult = MessageEventResult { this@MockBuildEvent.kind }
    }

    private class MockFileMessageEvent(
        message: String,
        description: String,
        kind: Kind,
        private val filePosition: FilePosition
    ) : FileMessageEvent, MockBuildEvent(message, description, kind) {
        override fun getFilePosition(): FilePosition = filePosition
        override fun getResult() = object : FileMessageEventResult {
            override fun getKind(): Kind = this@MockFileMessageEvent.kind

            override fun getFilePosition(): FilePosition = this@MockFileMessageEvent.filePosition
        }
    }


    fun testGenericKotlinError() {
        val translator = KotlincExceptionTranslator()
        val error = MockFileMessageEvent(
            "Unresolved reference: good compiler",
            "e: file://Test.kt:3:5 Unresolved reference: good compiler",
            Kind.ERROR,
            FilePosition(File("Test.kt"), 3, 5)
        )
        val translated = translator.parseException(error).getOrNull()
        kotlin.test.assertNotNull(translated)
        assertIs<KotlincException.GeneralKotlincException>(translated)
        assertEquals("Unresolved reference: good compiler", translated.message)
        assertEquals(KotlincErrorSeverity.ERROR, translated.severity)
        assertEquals(CaretPosition(Path("Test.kt"), 3, 5), translated.position)

        val warning = MockFileMessageEvent(
            "Unresolved reference: good compiler",
            "w: file://Test.kt:3:5 Unresolved reference: good compiler",
            Kind.WARNING,
            FilePosition(File("Test2.kt"), 30, 566)
        )
        val translated2 = translator.parseException(warning).getOrNull()
        kotlin.test.assertNotNull(translated2)
        assertIs<KotlincException.GeneralKotlincException>(translated2)
        assertEquals("Unresolved reference: good compiler", translated2.message)
        assertEquals(KotlincErrorSeverity.WARNING, translated2.severity)
        assertEquals(CaretPosition(Path("Test2.kt"), 30, 566), translated2.position)
    }

    fun testBackendCompilerError() {
        val translator = KotlincExceptionTranslator()
        val backendError = MockBuildEvent(
            "org.jetbrains.kotlin.backend.common.CompilationException: Back-end: Please report this problem https://kotl.in/issue",
            "org.jetbrains.kotlin.backend.common.CompilationException: Back-end: Please report this problem https://kotl.in/issue\n" +
                    "Test.kt:-1:-1\n" +
                    "Details: Internal error in file lowering: java.lang.IllegalStateException: should not be called\n" +
                    "\tat org.jetbrains.kotlin.utils.addToStdlib.AddToStdlibKt.shouldNotBeCalled(addToStdlib.kt:323)\n" +
                    "\tat org.jetbrains.kotlin.utils.addToStdlib.AddToStdlibKt.shouldNotBeCalled\$default(addToStdlib.kt:322)\n" +
                    "\tat org.jetbrains.kotlin.ir.symbols.impl.IrFakeOverrideSymbolBase.getOwner(IrFakeOverrideSymbol.kt:41)\n" +
                    "\tat org.jetbrains.kotlin.ir.symbols.impl.IrFakeOverrideSymbolBase.getOwner(IrFakeOverrideSymbol.kt:20)\n" +
                    "\tat org.jetbrains.kotlin.backend.jvm.lower.ExternalPackageParentPatcherLowering\$Visitor.visitMemberAccess(ExternalPackageParentPatcherLowering.kt:39)\n" +
                    "\tat stripped",
            Kind.ERROR,
        )
        val translated = translator.parseException(backendError).getOrNull()
        kotlin.test.assertNotNull(translated)
        assertIs<KotlincException.BackendCompilerException>(translated)
        assertEquals(CaretPosition(Path("Test.kt"), -1, -1), translated.position)
        assertEquals(
            "Details: Internal error in file lowering: java.lang.IllegalStateException: should not be called",
            translated.additionalMessage
        )
        assert(translated.stacktrace.lines().all { it.startsWith("\tat ") })

        val analysisError = MockBuildEvent(
            "org.jetbrains.kotlin.util.FileAnalysisException: While analysing Test.kt:19:3: java.lang.IllegalArgumentException: Failed requirement.",
            "org.jetbrains.kotlin.util.FileAnalysisException: While analysing Test.kt:19:3: java.lang.IllegalArgumentException: Failed requirement.\n" +
                    "\tat org.jetbrains.kotlin.util.AnalysisExceptionsKt.wrapIntoFileAnalysisExceptionIfNeeded(AnalysisExceptions.kt:57)\n" +
                    "\tat org.jetbrains.kotlin.fir.FirCliExceptionHandler.handleExceptionOnFileAnalysis(Utils.kt:249)\n" +
                    "\tat org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer.transformFile(FirDeclarationsResolveTransformer.kt:1667)\n" +
                    "\tat org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher.transformFile(FirAbstractBodyResolveTransformerDispatcher.kt:57)\n" +
                    "\tat org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher.transformFile(FirAbstractBodyResolveTransformerDispatcher.kt:24)\n" +
                    "\tat org.jetbrains.kotlin.fir.declarations.FirFile.transform(FirFile.kt:46)\n" +
                    "\tat org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformerAdapter.transformFile(FirBodyResolveTransformerAdapters.kt:41)\n" +
                    "\tat stripped",
            Kind.ERROR
        )
        val translated2 = translator.parseException(analysisError).getOrNull()
        kotlin.test.assertNotNull(translated2)
        assertIs<KotlincException.GenericInternalCompilerException>(translated2)
        assertEquals(
            "While analysing Test.kt:19:3: java.lang.IllegalArgumentException: Failed requirement.",
            translated2.message
        )
        assert(translated2.stacktrace.lines().all { it.startsWith("\tat ") })
    }

    fun testInvalidException() {
        val translator = KotlincExceptionTranslator()
        val hasKotlinIssueCiteButNoException = MockBuildEvent(
            "hey! This is a very cool text!\tat kek. Please report this problem https://kotl.in/issue",
            "hey! This is a very cool text!\tat kek. Please report this problem https://kotl.in/issue",
            Kind.INFO
        )
        val translated = translator.parseException(hasKotlinIssueCiteButNoException)
        assertIs<Either.Left<CompilationSuccess>>(translated)

        val hasErrorTypeButNoStacktrace = MockBuildEvent(
            "org.example.CompilationError: wow such an important text Please report this problem https://kotl.in/issue",
            "org.example.CompilationError: wow such an important text Please report this problem https://kotl.in/issue" +
                    "This is an additional text",
            Kind.ERROR
        )
        val translated2 = translator.parseException(hasErrorTypeButNoStacktrace)
        assertIs<Either.Left<CompilationSuccess>>(translated2)
        val backendErrorShortText = "org.jetbrains.kotlin.backend.common.CompilationException: Lol that's an end. Please report this problem https://kotl.in/issue"
        val backendErrorShort = MockBuildEvent(
            backendErrorShortText,
            backendErrorShortText,
            Kind.ERROR
        )
        val translated3 = translator.parseException(backendErrorShort)
        assertIs<Either.Left<CompilationSuccess>>(translated3)

        val backendErrorWithFilePositionWithoutStacktrace = """org.jetbrains.kotlin.backend.common.CompilationException: okay, good one Please report this problem https://kotl.in/issue
            |File.kt:-1:-1
        """.trimMargin()
        val backendErrorWithFilePosition = MockBuildEvent(
            backendErrorWithFilePositionWithoutStacktrace.lines().first(),
            backendErrorWithFilePositionWithoutStacktrace,
            Kind.ERROR
        )
        val translated4 = translator.parseException(backendErrorWithFilePosition)
        assertIs<Either.Left<CompilationSuccess>>(translated4)
        val backendErrorWithFilePositionWithAdditionalWoStackTraceText =
            "$backendErrorWithFilePositionWithoutStacktrace\nsafsdjhfsjdfdshjkfsd"
        val backendErrorWithFilePositionWithAdditionalWoStackTrace = MockBuildEvent(
            backendErrorWithFilePositionWithAdditionalWoStackTraceText.lines().first(),
            backendErrorWithFilePositionWithAdditionalWoStackTraceText,
            Kind.ERROR
        )
        val translated5 = translator.parseException(backendErrorWithFilePositionWithAdditionalWoStackTrace)
        assertIs<Either.Left<CompilationSuccess>>(translated5)
    }
}