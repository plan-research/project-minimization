package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.MinimizationStage
import org.plan.research.minimization.plugin.model.state.*

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import org.jetbrains.annotations.NonNls

@State(
    name = "org.intellij.sdk.settings.AppSettings",
    storages = [Storage("ProjectMinimizationSettings.xml")],
)
class AppSettings : PersistentStateComponent<AppSettings.State> {
    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    data class State(
        @NonNls
        var compilationStrategy: CompilationStrategy = CompilationStrategy.GRADLE_IDEA,
        var temporaryProjectLocation: String = "minimization-project-snapshots",
        var snapshotStrategy: SnapshotStrategy = SnapshotStrategy.PROJECT_CLONING,
        var exceptionComparingStrategy: ExceptionComparingStrategy = ExceptionComparingStrategy.SIMPLE,
        var stages: List<MinimizationStage> = arrayListOf(
            FileLevelStage(
                hierarchyCollectionStrategy = HierarchyCollectionStrategy.FILE_TREE,
                ddAlgorithm = DDStrategy.PROBABILISTIC_DD,
            ),
        ),
        var transformations: List<TransformationDescriptors> = arrayListOf(
            TransformationDescriptors.PATH_RELATIVIZATION,
        ),
        var configMode: StageConfigMode = StageConfigMode.DEFAULT,
        var isFileStageEnabled: Boolean = true,
    )

    companion object {
        fun getInstance(): AppSettings = ApplicationManager.getApplication().getService(AppSettings::class.java)
    }
}
