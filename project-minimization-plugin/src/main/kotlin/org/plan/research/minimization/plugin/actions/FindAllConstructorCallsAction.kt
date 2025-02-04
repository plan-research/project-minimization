package org.plan.research.minimization.plugin.actions

import org.plan.research.minimization.plugin.context.impl.DefaultProjectContext
import org.plan.research.minimization.plugin.services.TestPrimaryConstructorService

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class FindAllConstructorCallsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val context = DefaultProjectContext(project)
        project.service<TestPrimaryConstructorService>().printAllConstructorCalls(context)
    }
}
