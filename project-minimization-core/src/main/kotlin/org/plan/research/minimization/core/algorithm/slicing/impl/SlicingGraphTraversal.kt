package org.plan.research.minimization.core.algorithm.slicing.impl

import mu.KotlinLogging
import org.plan.research.minimization.core.model.SlicingGraphNode

open class SlicingGraphTraversal<T : SlicingGraphNode> {
    private val visitedMutable: MutableSet<T> = mutableSetOf()
    private val logger = KotlinLogging.logger {}
    val visited: Set<T>
        get() = visitedMutable

    protected open suspend fun onVisit(node: T) {
        logger.trace { "Visiting $node"}
        visitedMutable.add(node)
    }

    protected open suspend fun onEdge(from: T, to: T) {
        logger.trace { "Visiting $to from $from"}
    }

    private suspend fun visit(node: T) {
        if (visited.contains(node))
            return
        onVisit(node)
        node.getOutwardEdges().forEach { nextNode ->
            require(nextNode::class == node::class) {
                "Alien Node class is found. Expecting: ${node::class.simpleName}, got: ${nextNode::class.simpleName}"
            }

            @Suppress("UNCHECKED_CAST")
            onEdge(node, nextNode as T)
            visit(nextNode)
        }
    }
    suspend fun visitAll(roots: List<T>) = roots.forEach { visit(it) }
}