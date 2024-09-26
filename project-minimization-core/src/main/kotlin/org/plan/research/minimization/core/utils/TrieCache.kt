package org.plan.research.minimization.core.utils

import java.util.IdentityHashMap

class TrieCache<T, R> {

    internal val root: Node = Node()

    internal inner class Node {
        val children = IdentityHashMap<T, Node>()
        var value: R? = null
    }

    operator fun get(key: Collection<T>): R? {
        val ordered = orderedKey(key)
        var node = root
        for (item in ordered) {
            node = node.children[item] ?: return null
        }
        return node.value
    }

    operator fun set(key: Collection<T>, value: R) {
        val ordered = orderedKey(key)
        var node = root
        for (item in ordered) {
            node = node.children.getOrPut(item) { Node() }
        }
        node.value = value
    }

    internal inline fun getOrPut(key: Collection<T>, default: () -> R): R {
        val ordered = orderedKey(key)
        var node = root
        for (item in ordered) {
            node = node.children.getOrPut(item) { Node() }
        }
        node.value?.let { return it }
        return default().also { node.value = it }
    }
}

internal fun <T> orderedKey(key: Collection<T>): List<T> {
    return key.sortedBy { System.identityHashCode(it) }
}
