package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.state.*

import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls

import javax.swing.JComponent

class AppSettingsConfigurable : Configurable {
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
        val state = AppSettings.getInstance().state
        return mySettingsComponent?.compilationStrategy != state.compilationStrategy ||
            mySettingsComponent?.temporaryProjectLocation != state.temporaryProjectLocation ||
            mySettingsComponent?.snapshotStrategy != state.snapshotStrategy ||
            mySettingsComponent?.exceptionComparingStrategy != state.exceptionComparingStrategy ||
            mySettingsComponent?.stages != state.stages ||
            mySettingsComponent?.transformations != state.transformations ||
            mySettingsComponent?.isFileStageEnabled != state.isFileStageEnabled ||
            mySettingsComponent?.configMode != state.configMode ||
            mySettingsComponent?.selectedHierarchyStrategy != state.selectedHierarchyStrategy ||
            mySettingsComponent?.selectedDDStrategy != state.selectedDDStrategy ||
            mySettingsComponent?.isHierarchyStrategyEnabled != state.isHierarchyStrategyEnabled ||
            mySettingsComponent?.isDDAlgorithmEnabled != state.isDDAlgorithmEnabled
    }

    override fun apply() {
        AppSettings.getInstance().state.apply {
            compilationStrategy = mySettingsComponent?.compilationStrategy ?: CompilationStrategy.GRADLE_IDEA
            temporaryProjectLocation = mySettingsComponent?.temporaryProjectLocation ?: "minimization-project-snapshots"
            snapshotStrategy = mySettingsComponent?.snapshotStrategy ?: SnapshotStrategy.PROJECT_CLONING
            exceptionComparingStrategy = mySettingsComponent?.exceptionComparingStrategy ?: ExceptionComparingStrategy.SIMPLE
            stages = mySettingsComponent?.stages ?: arrayListOf(
                FileLevelStage(
                    hierarchyCollectionStrategy = HierarchyCollectionStrategy.FILE_TREE,
                    ddAlgorithm = DDStrategy.PROBABILISTIC_DD,
                ),
            )
            transformations = mySettingsComponent?.transformations ?: arrayListOf(
                TransformationDescriptors.PATH_RELATIVIZATION,
            )
            isFileStageEnabled = mySettingsComponent?.isFileStageEnabled ?: false
            configMode = mySettingsComponent?.configMode ?: StageConfigMode.DEFAULT
            selectedHierarchyStrategy = mySettingsComponent?.selectedHierarchyStrategy ?: HierarchyCollectionStrategy.FILE_TREE
            selectedDDStrategy = mySettingsComponent?.selectedDDStrategy ?: DDStrategy.PROBABILISTIC_DD
            isHierarchyStrategyEnabled = mySettingsComponent?.isHierarchyStrategyEnabled ?: false
            isDDAlgorithmEnabled = mySettingsComponent?.isDDAlgorithmEnabled ?: false
        }
    }

    override fun reset() {
        val state = AppSettings.getInstance().state
        mySettingsComponent?.compilationStrategy = state.compilationStrategy
        mySettingsComponent?.temporaryProjectLocation = state.temporaryProjectLocation
        mySettingsComponent?.snapshotStrategy = state.snapshotStrategy
        mySettingsComponent?.exceptionComparingStrategy = state.exceptionComparingStrategy
        mySettingsComponent?.stages = state.stages
        mySettingsComponent?.transformations = state.transformations
        mySettingsComponent?.isFileStageEnabled = state.isFileStageEnabled
        mySettingsComponent?.configMode = state.configMode
        mySettingsComponent?.selectedHierarchyStrategy = state.selectedHierarchyStrategy
        mySettingsComponent?.selectedDDStrategy = state.selectedDDStrategy
        mySettingsComponent?.isHierarchyStrategyEnabled = state.isHierarchyStrategyEnabled
        mySettingsComponent?.isDDAlgorithmEnabled = state.isDDAlgorithmEnabled
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}
