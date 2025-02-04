package org.plan.research.minimization.plugin.algorithm

import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.modification.item.IJDDItem

import arrow.core.Either

typealias ProjectHierarchyProducerResult<C, T> = Either<HierarchyBuildError, IJHierarchicalDDGenerator<C, T>>

interface ProjectHierarchyProducer<C : IJDDContext, T : IJDDItem> {
    suspend fun produce(
        context: C,
    ): ProjectHierarchyProducerResult<C, T>
}
