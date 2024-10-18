package org.plan.research.minimization.plugin.model

import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.plugin.errors.HierarchyBuildError

import arrow.core.Either

typealias ProjectHierarchyProducerResult<T> = Either<HierarchyBuildError, HierarchicalDDGenerator<IJDDContext, T>>

interface ProjectHierarchyProducer<T : IJDDItem> {
    suspend fun produce(
        fromContext: IJDDContext,
    ): ProjectHierarchyProducerResult<T>
}
