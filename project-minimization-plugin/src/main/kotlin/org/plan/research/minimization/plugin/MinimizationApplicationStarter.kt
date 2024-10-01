package org.plan.research.minimization.plugin

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ModernApplicationStarter
import com.intellij.openapi.components.service
import org.plan.research.minimization.plugin.services.MinimizationService
import kotlin.io.path.Path

class MinimizationApplicationStarter : ModernApplicationStarter() {
    override val commandName: String = "minimization"

    override suspend fun start(args: List<String>) {
        val projectPath = args.firstOrNull() ?: return // FIXME
        val project = ProjectUtil.openOrImport(Path(projectPath)) ?: return
        val minimizationService = project.service<MinimizationService>()
        minimizationService.minimizeProject(project)
    }
}