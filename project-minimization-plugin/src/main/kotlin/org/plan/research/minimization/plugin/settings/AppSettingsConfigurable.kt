package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.FunctionLevelStage
import org.plan.research.minimization.plugin.model.state.*
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
        mySettingsComponent = AppSettingsComponent()
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
            mySettingsComponent?.isFrozen != project.service<MinimizationPluginSettings>().freezeSettings
    }

    override fun apply() {
        val newState = MinimizationPluginState().apply {
            compilationStrategy = mySettingsComponent?.compilationStrategy ?: CompilationStrategy.GRADLE_IDEA
            gradleTask = mySettingsComponent?.gradleTask ?: "build"
            gradleOptions = mySettingsComponent?.gradleOptions ?: emptyList()
            temporaryProjectLocation = mySettingsComponent?.temporaryProjectLocation ?: "minimization-project-snapshots"
            snapshotStrategy = mySettingsComponent?.snapshotStrategy ?: SnapshotStrategy.PROJECT_CLONING
            exceptionComparingStrategy = mySettingsComponent?.exceptionComparingStrategy ?: ExceptionComparingStrategy.SIMPLE
            stages = mySettingsComponent?.stages ?: listOf(
                FunctionLevelStage(),
                FileLevelStage(),
            )
            minimizationTransformations = mySettingsComponent?.transformations ?: listOf(
                TransformationDescriptors.PATH_RELATIVIZATION,
            )
        }

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
        mySettingsComponent?.isFrozen = project.service<MinimizationPluginSettings>().freezeSettings
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}
