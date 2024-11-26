package org.plan.research.minimization.plugin

import org.plan.research.minimization.plugin.services.TestActionService

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import mu.KotlinLogging

class TestOverriddenDeleteAction : AnAction() {
    private val logger = KotlinLogging.logger {}
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val testService = project.service<TestActionService>()
        testService.dumpDeletableElements { items ->
            logger.debug { "Found deletable items: $items" }
        }
    }
}
