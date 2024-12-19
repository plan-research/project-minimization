package org.plan.research.minimization.plugin.testActions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import org.plan.research.minimization.plugin.services.TestGraphService

class DumpDeletablePsiGraphAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        project.service<TestGraphService>().dumpGraph()
    }

}