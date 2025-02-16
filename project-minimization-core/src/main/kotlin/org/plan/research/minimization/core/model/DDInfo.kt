package org.plan.research.minimization.core.model

/**
 * Interface representing additional information for DD algorithm.
 */
interface DDInfo<T : DDItem> {
    fun of(item: T): DDItemInfo

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
