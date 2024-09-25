package org.plan.research.minimization.plugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDD
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDDGenerator
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

@Service(Service.Level.PROJECT)
class HierarchicalDDService(
    project: Project,
) {
    private val baseAlgorithm = project.service<MinimizationPluginSettings>().state.getDDAlgorithm()
    private val hierarchicalDD = HierarchicalDD(baseAlgorithm)
    suspend fun minimizeHierarchy(hierarchy: HierarchicalDDGenerator<PsiDDItem>) = hierarchicalDD.minimize(hierarchy)
}