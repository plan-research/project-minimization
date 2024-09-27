package org.plan.research.minimization.plugin.model

import arrow.core.Either
import com.intellij.openapi.project.Project
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.plugin.errors.HierarchyBuildError

interface ProjectHierarchyProducer {
    suspend fun produce(
        from: Project
    ): Either<HierarchyBuildError, HierarchicalDDGenerator<IJDDContext, PsiDDItem>>
}
