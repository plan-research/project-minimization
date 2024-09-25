package org.plan.research.minimization.plugin.services

import arrow.core.Either
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.plugin.errors.HierarchyBuildError
import org.plan.research.minimization.plugin.model.ProjectHierarchyProducer
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

@Service(Service.Level.PROJECT)
class ProjectHierarchyProducerService(private val project: Project) {
    private val underlyingObject: ProjectHierarchyProducer
        get() = project.service<MinimizationPluginSettings>().state.getHierarchyCollectionStrategy()

    suspend fun produce(): Either<HierarchyBuildError, HierarchicalDDGenerator<PsiDDItem>> =
        underlyingObject.produce(project)
}