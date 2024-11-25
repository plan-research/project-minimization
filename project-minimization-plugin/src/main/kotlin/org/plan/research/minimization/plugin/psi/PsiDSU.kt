package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.PsiStubDDItem

class PsiDSU<T, R>(private val availableElements: List<Pair<T, R>>, private val compareFunction: (R, R) -> Int) {
    private val elementToIndexMap = availableElements.withIndex().associate { it.value.first to it.index }
    private val parents = MutableList(availableElements.size) { it }
    private val ranks = MutableList(availableElements.size) { 1 }
    private val representativeElement = availableElements.map { it.second }.toMutableList()
    private fun getParent(index: Int): Int {
        if (parents[index] == index) return index
        parents[index] = getParent(parents[index])
        return parents[index]
    }

    fun union(leftElement: T, rightElement: T) {
        val leftIndex = elementToIndexMap[leftElement]
        val rightIndex = elementToIndexMap[rightElement]
        if (leftIndex == null || rightIndex == null) {
            return
        }
        val leftParent = getParent(leftIndex)
        val rightParent = getParent(rightIndex)
        if (leftParent == rightParent) {
            return
        }
        if (ranks[leftIndex] == ranks[rightIndex]) {
            ranks[leftIndex]++
        }
        val (newParent, newChild) = if (ranks[leftIndex] < ranks[rightIndex]) {
            rightIndex to leftIndex
        } else {
            leftIndex to rightIndex
        }
        parents[newChild] = newParent
        if (compareFunction(representativeElement[newParent], representativeElement[newChild]) > 0) {
            representativeElement[newParent] = representativeElement[newChild]
        }
    }

    fun representativeElementOf(element: T) = elementToIndexMap[element]?.let { representativeElement[it] }
    val classes: Map<R, List<R>>
        get() = parents
            .asSequence()
            .mapIndexed { idx, parent -> idx to getParent(parent) }
            .groupBy(
                keySelector = { representativeElement[it.second] },
                valueTransform = { availableElements[it.first].second }
            )
}