package org.plan.research.minimization.core.model

/**
 * DDContext is a marker interface representing the context for a delta debugging process.
 *
 * This interface is used to provide contextual information relevant to the delta debugging operations.
 * Implementations of this interface should contain the necessary state and data to guide the debugging
 * and minimization processes.
 */
interface DDContext

interface DDContextWithLevel<C : DDContextWithLevel<C>> : DDContext {
    val currentLevel: List<DDItem>?
    fun withCurrentLevel(currentLevel: List<DDItem>): C
}
