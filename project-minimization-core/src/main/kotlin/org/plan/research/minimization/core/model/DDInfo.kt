package org.plan.research.minimization.core.model

import java.util.IdentityHashMap

/**
 * Interface representing additional information for DD algorithm.
 */
interface DDInfo<in T : DDItem> {
    fun of(item: T): DDItemInfo

    companion object {
        fun <T : DDItem> (DDInfo<T>).importanceOf(items: List<T>): IdentityHashMap<T, Boolean> {
            val result = IdentityHashMap<T, Boolean>()
            items.forEach { result[it] = of(it).likelyImportant }
            return result
        }

        fun <T : DDItem> fromImportance(isImportant: (T) -> Boolean): DDInfo<T> = object : DDInfo<T> {
            override fun of(item: T): DDItemInfo = DDItemInfo(likelyImportant = isImportant(item))
        }
    }
}

@Suppress("OBJECT_NAME_INCORRECT")
object EmptyDDInfo : DDInfo<DDItem> {
    override fun of(item: DDItem): DDItemInfo = DDItemInfo()
}

data class DDItemInfo(val likelyImportant: Boolean = false)
