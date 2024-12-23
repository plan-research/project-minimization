package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.settings.ui.MinimizationPluginSettingsProducer

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.DslConfigurableBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel

class MinimizationPluginSettingsConfigurable(val project: Project) : DslConfigurableBase(), Configurable.NoScroll, Configurable {
    private val componentProducer = MinimizationPluginSettingsProducer(project)

    override fun getDisplayName(): String = "Project Minimization Plugin Settings"

    override fun createPanel(): DialogPanel = componentProducer.getPanel()
}
