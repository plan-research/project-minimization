package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.services.MinimizationPluginSettings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls

import javax.swing.JComponent

class AppSettingsConfigurable(private val project: Project) : Configurable {
    private var mySettingsComponent: AppSettingsComponent? = null

    // A default constructor with no arguments is required because
    // this implementation is registered as an applicationConfigurable

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String = "Project Minimization Plugin Settings"

    override fun getPreferredFocusedComponent(): JComponent? = mySettingsComponent?.getPreferredFocusedComponent()

    override fun createComponent(): JComponent? {
        mySettingsComponent = AppSettingsComponent(project)
        return mySettingsComponent?.getPanel()
    }

    override fun isModified(): Boolean {
        val state = project.service<MinimizationPluginSettings>().state
        return mySettingsComponent?.compilationStrategy != state.compilationStrategy ||
            mySettingsComponent?.gradleTask != state.gradleTask ||
            mySettingsComponent?.gradleOptions != state.gradleOptions ||
            mySettingsComponent?.temporaryProjectLocation != state.temporaryProjectLocation ||
            mySettingsComponent?.snapshotStrategy != state.snapshotStrategy ||
            mySettingsComponent?.exceptionComparingStrategy != state.exceptionComparingStrategy ||
            mySettingsComponent?.stages != state.stages ||
            mySettingsComponent?.transformations != state.minimizationTransformations ||
            mySettingsComponent?.ignorePaths != state.ignorePaths ||
            mySettingsComponent?.isFrozen != project.service<MinimizationPluginSettings>().freezeSettings
    }

    override fun apply() {
        val newState = mySettingsComponent?.let { settingsComponentState ->
            MinimizationPluginState().apply {
                compilationStrategy = settingsComponentState.compilationStrategy
                gradleTask = settingsComponentState.gradleTask
                gradleOptions = settingsComponentState.gradleOptions
                temporaryProjectLocation = settingsComponentState.temporaryProjectLocation
                snapshotStrategy = settingsComponentState.snapshotStrategy
                exceptionComparingStrategy = settingsComponentState.exceptionComparingStrategy
                stages = settingsComponentState.stages
                minimizationTransformations = settingsComponentState.transformations
                ignorePaths = settingsComponentState.ignorePaths
            }
        } ?: MinimizationPluginState()

        project.service<MinimizationPluginSettings>().updateState(newState)
    }

    override fun reset() {
        val state = project.service<MinimizationPluginSettings>().state
        mySettingsComponent?.compilationStrategy = state.compilationStrategy
        mySettingsComponent?.gradleTask = state.gradleTask
        mySettingsComponent?.gradleOptions = state.gradleOptions
        mySettingsComponent?.temporaryProjectLocation = state.temporaryProjectLocation
        mySettingsComponent?.snapshotStrategy = state.snapshotStrategy
        mySettingsComponent?.exceptionComparingStrategy = state.exceptionComparingStrategy
        mySettingsComponent?.stages = state.stages
        mySettingsComponent?.transformations = state.minimizationTransformations
        mySettingsComponent?.ignorePaths = state.ignorePaths
        mySettingsComponent?.isFrozen = project.service<MinimizationPluginSettings>().freezeSettings
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}
