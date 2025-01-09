package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.plugin.errors.HierarchyBuildError
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.item.IJDDItem

import arrow.core.Either

typealias ProjectHierarchyProducerResult<C, T> = Either<HierarchyBuildError, IJHierarchicalDDGenerator<C, T>>

interface ProjectHierarchyProducer<B : IJDDContext, T : IJDDItem> {
    suspend fun <C : B> produce(
        context: C,
    ): ProjectHierarchyProducerResult<C, T>
}
