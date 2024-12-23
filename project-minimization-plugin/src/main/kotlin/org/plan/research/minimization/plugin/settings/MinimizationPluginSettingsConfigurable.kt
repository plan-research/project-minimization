package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.settings.ui.SettingsProducer

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.DslConfigurableBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel

class MinimizationPluginSettingsConfigurable(val project: Project) : DslConfigurableBase(), Configurable.NoScroll, Configurable {
    private val componentProducer = SettingsProducer(project)

    override fun getDisplayName(): String = "Project Minimization Plugin Settings"

    override fun createPanel(): DialogPanel = componentProducer.getPanel()
}
