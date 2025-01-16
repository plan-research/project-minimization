package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.model.DeclarationLevelStage
import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.FunctionLevelStage
import org.plan.research.minimization.plugin.model.MinimizationStage
import org.plan.research.minimization.plugin.model.state.CompilationStrategy
import org.plan.research.minimization.plugin.model.state.ExceptionComparingStrategy
import org.plan.research.minimization.plugin.model.state.SnapshotStrategy
import org.plan.research.minimization.plugin.model.state.TransformationDescriptor

import com.intellij.openapi.components.BaseState
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection

class MinimizationPluginState : BaseState() {
    var compilationStrategy by enum(CompilationStrategy.GRADLE_IDEA)
    var gradleTask by property(DEFAULT_GRADLE_TASK) { it == DEFAULT_GRADLE_TASK }
    var temporaryProjectLocation by property(DEFAULT_TEMP_PROJ_LOCATION) { it == DEFAULT_TEMP_PROJ_LOCATION }
    var logsLocation by property(DEFAULT_LOGS_LOCATION) { it == DEFAULT_LOGS_LOCATION }
    var snapshotStrategy by enum(SnapshotStrategy.PROJECT_CLONING)
    var exceptionComparingStrategy by enum(ExceptionComparingStrategy.SIMPLE)

    @get:Tag
    @get:XCollection(style = XCollection.Style.v1, elementName = "option")
    var gradleOptions by property(emptyList<String>()) { it.isEmpty() }

    @get:Tag
    @get:XCollection(
        style = XCollection.Style.v1,
        elementName = "stage",
        elementTypes = [FunctionLevelStage::class, DeclarationLevelStage::class, FileLevelStage::class],
    )
    var stages by property(defaultStages) { it == defaultStages }

    @get:Tag
    @get:XCollection(style = XCollection.Style.v1, elementName = "transformation")
    var minimizationTransformations by property(defaultTransformations) { it == defaultTransformations }

    @get:Tag
    @get:XCollection(style = XCollection.Style.v1, elementName = "ignore-paths")
    var ignorePaths by property(emptyList<String>()) { it.isEmpty() }

    companion object {
        const val DEFAULT_GRADLE_TASK = "build"
        const val DEFAULT_LOGS_LOCATION: String = "minimization-logs"
        const val DEFAULT_TEMP_PROJ_LOCATION: String = "minimization-project-snapshots"
        val defaultStages: List<MinimizationStage> = listOf(
            FunctionLevelStage(),
            FileLevelStage(),
        )
        val defaultTransformations: List<TransformationDescriptor> = listOf(
            TransformationDescriptor.PATH_RELATIVIZATION,
        )
    }
}
