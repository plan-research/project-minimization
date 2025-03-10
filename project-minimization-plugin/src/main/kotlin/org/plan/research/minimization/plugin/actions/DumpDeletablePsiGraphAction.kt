package org.plan.research.minimization.plugin.actions

import org.plan.research.minimization.plugin.services.TestGraphService

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class DumpDeletablePsiGraphAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        project.service<TestGraphService>().dumpGraph()
    }
}
