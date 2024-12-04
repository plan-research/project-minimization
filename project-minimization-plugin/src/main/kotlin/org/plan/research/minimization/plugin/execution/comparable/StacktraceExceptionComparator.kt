package org.plan.research.minimization.plugin.execution.comparable

import org.plan.research.minimization.plugin.execution.exception.KotlincException
import org.plan.research.minimization.plugin.model.exception.CompilationException
import org.plan.research.minimization.plugin.model.exception.ExceptionComparator
import org.plan.research.minimization.plugin.model.exception.StacktraceComparator

class StacktraceExceptionComparator(
    private val generalComparator: ExceptionComparator,
    private val stacktraceComparator: StacktraceComparator,
) : ExceptionComparator {
    override fun areEquals(exception1: CompilationException, exception2: CompilationException): Boolean {
        if (exception1 !is KotlincException || exception2 !is KotlincException) {
            return generalComparator.areEquals(exception1, exception2)
        }

        val stacktrace1 = getStacktrace(exception1)
        val stacktrace2 = getStacktrace(exception2)

        return stacktrace1?.let { sTrace1 ->
            stacktrace2?.let { sTrace2 ->
                stacktraceComparator.areEqual(sTrace1, sTrace2)
            }
        } ?: generalComparator.areEquals(exception1, exception2)
    }

    private fun getStacktrace(exception: KotlincException): String? = when (exception) {
        is KotlincException.BackendCompilerException -> exception.stacktrace
        is KotlincException.GenericInternalCompilerException -> exception.stacktrace
        is KotlincException.GeneralKotlincException -> null
        is KotlincException.KspException -> exception.stacktrace
    }
}
