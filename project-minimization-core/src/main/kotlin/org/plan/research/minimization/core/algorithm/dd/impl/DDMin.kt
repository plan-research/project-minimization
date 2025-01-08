package org.plan.research.minimization.core.algorithm.dd.impl

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithmResult
import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.PropertyTester

import kotlin.math.min
import kotlinx.coroutines.yield
import org.plan.research.minimization.core.model.DDContextMonad

/**
 * Default Delta Debugging algorithm.
 *
 * This version already supports an optimized caching mechanism specialized for DDMin,
 * so it's not necessary to implement your own.
 */
class DDMin : DDAlgorithm {
    context(DDContextMonad<C>)
    @Suppress("NESTED_BLOCK", "TOO_LONG_FUNCTION")
    override suspend fun <C : DDContext, T : DDItem> minimize(
        items: List<T>,
        propertyTester: PropertyTester<C, T>,
    ): DDAlgorithmResult<T> {
        var currentItems = ArrayDeque(items)
        var smallItems = ArrayDeque<T>()
        val currentNodes = ArrayDeque<Node>()
        Node(items.size, parent = null, smallChecked = false, checked = true).let { root ->
            currentNodes.addAll(root.initNext())
        }
        var testSmall = true

        var granularity = 2
        while (currentItems.size > 1) {
            var reduced = false
            while (!reduced) {
                for (i in 0 until granularity) {
                    yield()
                    val node = currentNodes.removeFirst()
                    repeat(node.size) {
                        smallItems.add(currentItems.removeFirst())
                    }
                    if (testSmall) {
                        if (!node.smallChecked) {
                            val toBreak = propertyTester.test(smallItems)
                                .isRight {
                                    granularity = 2
                                    smallItems.let {
                                        smallItems = currentItems
                                        currentItems = it
                                    }
                                    smallItems.clear()
                                    reduced = true
                                    node.parent = null
                                    node.smallChecked = true
                                    node.checked = true
                                    currentNodes.clear()
                                    currentNodes.addAll(node.initNext())
                                    true
                                }
                            if (toBreak) {
                                break
                            }
                        }
                        node.smallChecked = true
                        if (granularity == 2) {
                            currentNodes.single().isCheckedAndMark()
                        }
                    } else {
                        if (granularity == 2 && currentNodes.single().smallChecked) {
                            node.isCheckedAndMark()
                        }
                        if (!node.isCheckedAndMark()) {
                            val toBreak = propertyTester.test(currentItems).isRight {
                                granularity -= 1
                                smallItems.clear()
                                node.delete()
                                reduced = true
                                true
                            }
                            if (toBreak) {
                                break
                            }
                        }
                    }

                    currentNodes.add(node)
                    repeat(node.size) {
                        currentItems.add(smallItems.removeFirst())
                    }
                }
                if (!reduced) {
                    if (!testSmall || granularity == 2) {
                        if (granularity == currentItems.size) {
                            return currentItems
                        }
                        granularity = min(granularity * 2, currentItems.size)
                        val next = currentNodes.flatMap { it.initNext() }
                        currentNodes.clear()
                        currentNodes.addAll(next)
                        testSmall = true
                    } else {
                        testSmall = false
                    }
                }
            }
        }
        return currentItems
    }

    /**
     * @property size a number of items
     * @property parent a parent node
     * @property smallChecked checked only items of the node
     * @property checked check all items without node's items
     */
    private class Node(
        val size: Int,
        var parent: Node?,
        var smallChecked: Boolean = false,
        var checked: Boolean = false,
    ) {
        var left: Node? = null
        var right: Node? = null

        fun initNext(): List<Node> {
            if (size >= 2) {
                left = Node(size / 2, this)
                right = Node((size + 1) / 2, this)
                return listOf(left!!, right!!)
            } else {
                left = Node(1, this, smallChecked, checked)
                return listOf(left!!)
            }
        }

        fun isCheckedAndMark(): Boolean {
            var node = this
            while (node.parent != null) {
                val parent = node.parent!!
                if (node.checked) {
                    return true
                }
                node.checked = true
                val otherNode = if (parent.left === node) {
                    parent.right
                } else {
                    parent.left
                }
                otherNode?.let {
                    return false
                }
                node = parent
            }
            return true
        }

        fun delete() {
            val queue = ArrayDeque<Node>()

            var node = this
            var needToDelete = true
            while (node.parent != null) {
                val parent = node.parent!!
                if (parent.left === node) {
                    if (needToDelete) {
                        parent.left = null
                        node.parent = null
                    }
                    parent.right
                } else {
                    if (needToDelete) {
                        parent.right = null
                        node.parent = null
                    }
                    parent.left
                }?.let {
                    needToDelete = false
                    queue.add(it)
                }
                node = parent
            }

            while (queue.isNotEmpty()) {
                val currentNode = queue.removeFirst().apply {
                    checked = false
                }
                currentNode.left?.let { queue.add(it) }
                currentNode.right?.let { queue.add(it) }
            }
        }
    }
}
