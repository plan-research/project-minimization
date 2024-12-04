package org.plan.research.minimization.plugin.model.exception

interface StacktraceComparator {
    fun areEqual(stack1: String, stack2: String): Boolean
}
