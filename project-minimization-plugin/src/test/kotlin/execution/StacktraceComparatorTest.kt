package execution

import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import org.plan.research.minimization.plugin.execution.comparable.SimpleStacktraceComparator
import org.plan.research.minimization.plugin.execution.comparable.StacktraceComparator
import java.io.File
import java.nio.file.Paths

class StacktraceComparatorTest: JavaCodeInsightFixtureTestCase() {
    private var stacktraceComparator: StacktraceComparator? = null

    override fun setUp() {
        super.setUp()
        stacktraceComparator = SimpleStacktraceComparator()
    }

    fun testEqual() {
        val stackPath = getFilePathFromResources("testData/stacktraceExamples/cinema-planner-1.txt")
        val stacktrace: String = File(stackPath).readText(Charsets.UTF_8)

        assert(stacktraceComparator?.areEqual(stacktrace, stacktrace) ?: throw Error())
    }

    fun testSimilar() {
        val stackPath1 = getFilePathFromResources("testData/stacktraceExamples/cinema-planner-1.txt")
        val stacktrace1: String = File(stackPath1).readText(Charsets.UTF_8)

        val stackPath2 = getFilePathFromResources("testData/stacktraceExamples/cinema-planner-2.txt")
        val stacktrace2: String = File(stackPath2).readText(Charsets.UTF_8)

        assert(stacktraceComparator?.areEqual(stacktrace1, stacktrace2) ?: throw Error())
    }

    fun testSimilar2() {
        val stackPath1 = getFilePathFromResources("testData/stacktraceExamples/kaliningraph-1.txt")
        val stacktrace1: String = File(stackPath1).readText(Charsets.UTF_8)

        val stackPath2 = getFilePathFromResources("testData/stacktraceExamples/kaliningraph-2.txt")
        val stacktrace2: String = File(stackPath2).readText(Charsets.UTF_8)

        assert(stacktraceComparator?.areEqual(stacktrace1, stacktrace2) ?: throw Error())
    }

    fun testDifference() {
        val stackPath1 = getFilePathFromResources("testData/stacktraceExamples/cinema-planner-1.txt")
        val stacktrace1: String = File(stackPath1).readText(Charsets.UTF_8)

        val stackPath2 = getFilePathFromResources("testData/stacktraceExamples/kaliningraph-1.txt")
        val stacktrace2: String = File(stackPath2).readText(Charsets.UTF_8)

        assert(!(stacktraceComparator?.areEqual(stacktrace1, stacktrace2) ?: throw Error()))
    }

    private fun getFilePathFromResources(resourcePath: String): String {
        val resourceUrl = this::class.java.classLoader.getResource(resourcePath)
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")
        return Paths.get(resourceUrl.toURI()).toString()
    }
}