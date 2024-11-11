package org.plan.research.minimization.plugin

import org.plan.research.minimization.plugin.model.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.services.MinimizationService
import org.plan.research.minimization.plugin.services.ProjectCloningService

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.impl.ProjectViewPane
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.ui.MessageDialogBuilder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * An action class responsible for minimizing a given project.
 */
class MinimizeProjectAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread =
        ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = true
        val project = e.project ?: run {
            e.presentation.isEnabled = false
            return
        }

        val isDumb = DumbService.isDumb(project)
        e.presentation.isEnabled = !isDumb
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val minimizationService = project.service<MinimizationService>()
        minimizationService.minimizeProject(project) { context ->
            when (context) {
                is HeavyIJDDContext -> {
                    /* do nothing */ }
                is LightIJDDContext -> {
                    val openProject = withContext(Dispatchers.EDT) {
                        MessageDialogBuilder.yesNo(
                            "Open Minimized Project",
                            "Do you want to open the minimized project?",
                        ).ask(project)
                    }

                    if (openProject) {
                        val cloningService = project.service<ProjectCloningService>()
                        cloningService.openProject(context.projectDir.toNioPath(), true)
                    } else {
                        withContext(Dispatchers.EDT) {
                            readAction {
                                val projectView = ProjectView.getInstance(project)
                                projectView.changeView(ProjectViewPane.ID)
                                projectView.select(null, context.projectDir, false)
                            }
                        }
                    }
                }
            }
        }
    }
}
