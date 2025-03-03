package execution

import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.plugin.compilation.exception.KotlincException
import org.plan.research.minimization.plugin.util.getExceptionComparator
import org.plan.research.minimization.plugin.compilation.comparator.ExceptionComparator
import org.plan.research.minimization.plugin.settings.data.ExceptionComparingStrategy
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.exists

class StacktraceComparatorTest: JavaCodeInsightFixtureTestCase() {
    private lateinit var stacktraceComparator: ExceptionComparator
    private var cinema1 = "testData/stacktraceExamples/cinema-planner-1.txt"
    private var cinema2 = "testData/stacktraceExamples/cinema-planner-2.txt"
    private var kaliningraph1 = "testData/stacktraceExamples/kaliningraph-1.txt"
    private var kaliningraph2 = "testData/stacktraceExamples/kaliningraph-2.txt"

    override fun setUp() {
        super.setUp()
        stacktraceComparator = ExceptionComparingStrategy.STACKTRACE.getExceptionComparator()
    }

    fun testEqual() {
        assert(callComparator(cinema1, cinema1))
        assert(callComparator(kaliningraph1, kaliningraph1))
    }

    fun testSimilar() {
        assert(callComparator(cinema1, cinema2))
        assert(callComparator(kaliningraph1, kaliningraph2))
    }

    fun testDifferent() {
        assert(!callComparator(cinema1, kaliningraph1))
        assert(!callComparator(cinema2, kaliningraph2))
    }

    private fun callComparator(resource1: String, resource2: String): Boolean {
        val filePath1 = getFilePathFromResources(resource1)
        val input1: String = File(filePath1).readText(Charsets.UTF_8)
        val (stacktrace1, message1) = parseStacktraceAndMessage(input1)
        val exception1: KotlincException = KotlincException.GenericInternalCompilerException(
            stacktrace = stacktrace1,
            message = message1,
        )

        val filePath2 = getFilePathFromResources(resource2)
        val input2: String = File(filePath2).readText(Charsets.UTF_8)
        val (stacktrace2, message2) = parseStacktraceAndMessage(input2)
        val exception2: KotlincException = KotlincException.GenericInternalCompilerException(
            stacktrace = stacktrace2,
            message = message2,
        )

        return runBlocking { stacktraceComparator.areEquals(exception1, exception2) }
    }

    private fun parseStacktraceAndMessage(input: String): Pair<String, String> {
        val separator = ", message="
        val (stacktrace, message) = input.split(separator, limit = 2).let {
            val stacktracePart = it.getOrNull(0)?.removePrefix("stacktrace=")?.trim() ?: ""
            val messagePart = it.getOrNull(1)?.trim() ?: ""
            stacktracePart to messagePart
        }
        return stacktrace to message
    }

    private fun getFilePathFromResources(resourcePath: String): String {
        val resourceFile = Path("src/test/resources", resourcePath)
            .takeIf { it.exists() }
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")

        return resourceFile.toString()
    }
}