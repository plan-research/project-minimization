package org.plan.research.minimization.plugin.execution.comparable

interface StacktraceComparator {
    fun areEqual(stack1: String, stack2: String): Boolean
}
