package org.plan.research.minimization.core.model

import java.util.IdentityHashMap

/**
 * Interface representing additional information for DD algorithm.
 */
interface DDInfo<T : DDItem> {
    fun of(item: T): DDItemInfo

    fun importanceOf(items: List<T>): IdentityHashMap<T, Boolean> {
        val result = IdentityHashMap<T, Boolean>()
        items.forEach { result[it] = of(it).likelyImportant }
        return result
    }

    companion object {
        fun <T : DDItem> empty(): DDInfo<T> = object : DDInfo<T> {
            override fun of(item: T): DDItemInfo = DDItemInfo()
        }

        fun <T : DDItem> fromImportance(isImportant: (T) -> Boolean): DDInfo<T> = object : DDInfo<T> {
            override fun of(item: T): DDItemInfo = DDItemInfo(likelyImportant = isImportant(item))
        }
    }
}

data class DDItemInfo(val likelyImportant: Boolean = false)
