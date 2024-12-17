package org.plan.research.minimization.plugin.execution.comparable

import org.plan.research.minimization.plugin.execution.IdeaCompilationException
import org.plan.research.minimization.plugin.execution.exception.KotlincException
import org.plan.research.minimization.plugin.logging.statLogger
import org.plan.research.minimization.plugin.model.exception.CompilationException
import org.plan.research.minimization.plugin.model.exception.ExceptionComparator

import org.apache.commons.text.similarity.JaccardSimilarity
import org.apache.commons.text.similarity.LevenshteinDistance

import kotlin.math.exp
import kotlin.math.max

class StacktraceExceptionComparator(
    private val generalComparator: ExceptionComparator,
) : ExceptionComparator {
    override fun areEquals(exception1: CompilationException, exception2: CompilationException): Boolean {
        if (exception1 is IdeaCompilationException && exception2 is IdeaCompilationException) {
            return exception1.kotlincExceptions.zip(exception2.kotlincExceptions).all {pair -> areEquals(pair.first, pair.second) }
        }

        if (exception1 !is KotlincException || exception2 !is KotlincException) {
            return generalComparator.areEquals(exception1, exception2)
        }

        val exceptionLines1 = extractExceptionLines(exception1) ?: return generalComparator.areEquals(exception1, exception2)
        val exceptionLines2 = extractExceptionLines(exception2) ?: return generalComparator.areEquals(exception1, exception2)

        return isSimilar(exceptionLines1, exceptionLines2)
    }

    // return message + stacktrace if possible
    private fun extractExceptionLines(exception: KotlincException): List<String>? = when (exception) {
        is KotlincException.BackendCompilerException -> parseMessage(exception.additionalMessage) + parseStacktrace(exception.stacktrace)
        is KotlincException.GenericInternalCompilerException -> parseMessage(exception.message) + parseStacktrace(exception.stacktrace ?: "")
        is KotlincException.GeneralKotlincException -> null
        is KotlincException.KspException -> parseMessage(exception.message) + parseStacktrace(exception.stacktrace)
    }

    // leave informative part of stacktrace lines
    private fun parseStacktrace(stacktrace: String): List<String> = stacktrace.lines()
        .map { it.trim() }
        .takeWhile { it.startsWith("at org.jetbrains.kotlin.") }
        .map { it.removePrefix("at org.jetbrains.kotlin.").trim() }

    private fun parseMessage(message: String?): List<String> =
        message?.let { listOf(it) } ?: emptyList()

    private fun isSimilar(stackList1: List<String>, stackList2: List<String>): Boolean {
        var similarity: Double = ABSOLUTE_SIMILARITY

        stackList1.zip(stackList2).forEachIndexed { index, (line1, line2) ->
            val lineDifference = levensteinDifference(line1, line2)
            val coefficient = attentionCoefficient(index + 1)
            similarity -= coefficient * lineDifference
        }

        statLogger.info { "Stacktrace similarity: $similarity" }

        return similarity > THRESHOLD
    }

    // 1 / e^x differentiation from 0 to +inf is 1, so it works as coefficient of importance for lines
    private fun attentionCoefficient(num: Int): Double {
        return 1 / exp(num.toDouble())  // exponential attention extinction
    }

    /* difference can take values from 0 to 1
       0 means equal
       1 absolutely different */
    private fun levensteinDifference(str1: String, str2: String): Double =
        LevenshteinDistance().apply(str1, str2).toDouble() / max(str1.length, str2.length)

    /* difference can take values from 0 to 1
       0 means equal
       1 absolutely different */
    private fun jaccardDifference(str1: String, str2: String): Double = 1 - JaccardSimilarity().apply(str1, str2)

    companion object {
        const val ABSOLUTE_SIMILARITY = 1.0

        const val THRESHOLD = 0.95  // Threshold value can be changed in future
    }
}
