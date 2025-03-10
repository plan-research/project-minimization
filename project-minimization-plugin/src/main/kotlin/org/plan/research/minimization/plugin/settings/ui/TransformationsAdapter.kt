package org.plan.research.minimization.plugin.settings.ui

import org.plan.research.minimization.plugin.settings.MinimizationPluginStateObservable
import org.plan.research.minimization.plugin.settings.data.TransformationDescriptor

import org.jetbrains.kotlin.nj2k.mutate

class TransformationsAdapter(val state: MinimizationPluginStateObservable) {
    private var transformations = state.minimizationTransformations
    private val transformation2Index = TransformationDescriptor.entries
        .withIndex()
        .associate { it.value to it.index }
    private val comparator = compareBy<TransformationDescriptor> { transformation2Index[it] }

    var pathRelativization: Boolean
        get() = TransformationDescriptor.PATH_RELATIVIZATION in transformations.get()
        set(value) {
            update(value, TransformationDescriptor.PATH_RELATIVIZATION)
        }

    init {
        transformations.mutate { it.sortedWith(comparator) }
    }

    private fun update(enable: Boolean, transformation: TransformationDescriptor) {
        transformations.mutate {
            val index = it.binarySearch(transformation, comparator)
            val contains = it.getOrNull(index) == transformation
            it.mutate {
                when {
                    enable && !contains -> add(index, transformation)

                    !enable && contains -> removeAt(index)
                }
            }
        }
    }
}
