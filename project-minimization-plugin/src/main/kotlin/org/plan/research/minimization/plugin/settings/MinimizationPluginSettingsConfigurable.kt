package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.services.MinimizationPluginSettings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls

import javax.swing.JComponent

class MinimizationPluginSettingsConfigurable(private val project: Project) : Configurable {
    private var mySettingsComponent: MinimizationPluginSettingsComponent? = null

    // A default constructor with no arguments is required because
    // this implementation is registered as an applicationConfigurable

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String = "Project Minimization Plugin Settings"

    override fun getPreferredFocusedComponent(): JComponent? = mySettingsComponent?.getPreferredFocusedComponent()

    override fun createComponent(): JComponent? {
        mySettingsComponent = MinimizationPluginSettingsComponent(project)
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
                mySettingsComponent?.ignorePaths != state.ignorePaths
    }

    override fun apply() {
        val settingsComponentState = mySettingsComponent ?: return

        project.service<MinimizationPluginSettings>().stateObservable.apply {
            compilationStrategy.set(settingsComponentState.compilationStrategy)
            gradleTask.set(settingsComponentState.gradleTask)
            gradleOptions.set(settingsComponentState.gradleOptions)
            temporaryProjectLocation.set(settingsComponentState.temporaryProjectLocation)
            snapshotStrategy.set(settingsComponentState.snapshotStrategy)
            exceptionComparingStrategy.set(settingsComponentState.exceptionComparingStrategy)
            stages.set(settingsComponentState.stages)
            minimizationTransformations.set(settingsComponentState.transformations)
            ignorePaths.set(settingsComponentState.ignorePaths)
        }
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
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}
