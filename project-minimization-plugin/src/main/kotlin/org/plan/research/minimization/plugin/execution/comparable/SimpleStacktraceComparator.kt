package org.plan.research.minimization.plugin.execution.comparable

import org.apache.commons.text.similarity.JaccardSimilarity
import org.apache.commons.text.similarity.LevenshteinDistance
import org.plan.research.minimization.plugin.logging.statLogger
import kotlin.math.exp

class SimpleStacktraceComparator : StacktraceComparator {
    override fun areEqual(stack1: String, stack2: String): Boolean {
        val stackList1 = parse(stack1)
        val stackList2 = parse(stack2)

        var similarity: Double = ABSOLUTE_SIMILARITY

        stackList1.zip(stackList2).forEachIndexed { index, (line1, line2) ->
            val lineDifference = jaccardDifference(line1, line2)
            val coefficient = attentionCoefficient(index + 1)
            similarity -= coefficient * lineDifference
        }

        statLogger.info { "Stacktrace similarity: $similarity" }

        return similarity > THRESHOLD
    }

    // leave informative part of stacktrace lines
    private fun parse(stacktrace: String): List<String> = stacktrace.lines()
        .map { it.trim() }
        .filter { it.startsWith("at") }
        .map { it.removePrefix("at").trim() }

    // 1 / e^x differentiation from 0 to +inf is 1, so it works as coefficient of importance for lines
    private fun attentionCoefficient(num: Int): Double {
        return 1 / exp(num.toDouble())  // exponential attention extinction
    }

    /* difference can take values from 0 to 1
       0 means equal
       1 absolutely different */
    private fun levensteinDifference(str1: String, str2: String): Double = LevenshteinDistance().apply(str1, str2).toDouble() / (str1.length + str2.length)

    /* difference can take values from 0 to 1
       0 means equal
       1 absolutely different */
    private fun jaccardDifference(str1: String, str2: String): Double = 1 - JaccardSimilarity().apply(str1, str2)

    companion object {
        const val ABSOLUTE_SIMILARITY = 1.0

        const val THRESHOLD = 0.95  // Threshold value can be changed in future
    }
}
