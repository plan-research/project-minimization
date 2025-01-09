package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.plugin.errors.HierarchyBuildError

import arrow.core.Either
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.item.IJDDItem

typealias ProjectHierarchyProducerResult<C, T> = Either<HierarchyBuildError, IJHierarchicalDDGenerator<C, T>>

interface ProjectHierarchyProducer<BC : IJDDContext, T : IJDDItem> {
    suspend fun <C : BC> produce(
        context: C,
    ): ProjectHierarchyProducerResult<C, T>
}
