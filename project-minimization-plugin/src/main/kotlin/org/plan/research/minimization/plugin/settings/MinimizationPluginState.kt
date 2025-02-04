package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.algorithm.DeclarationGraphStage
import org.plan.research.minimization.plugin.algorithm.FileLevelStage
import org.plan.research.minimization.plugin.algorithm.FunctionLevelStage
import org.plan.research.minimization.plugin.algorithm.MinimizationStage
import org.plan.research.minimization.plugin.settings.enums.CompilationStrategy
import org.plan.research.minimization.plugin.settings.enums.ExceptionComparingStrategy
import org.plan.research.minimization.plugin.settings.enums.SnapshotStrategy
import org.plan.research.minimization.plugin.settings.enums.TransformationDescriptor

import com.intellij.openapi.components.BaseState
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection

class MinimizationPluginState : BaseState() {
    var compilationStrategy by enum(CompilationStrategy.GRADLE_IDEA)
    var gradleTask by property(DEFAULT_GRADLE_TASK) { it == DEFAULT_GRADLE_TASK }
    var temporaryProjectLocation by string()
    var logsLocation by property(DEFAULT_LOGS_LOCATION) { it == DEFAULT_LOGS_LOCATION }
    var snapshotStrategy by enum(SnapshotStrategy.PROJECT_CLONING)
    var exceptionComparingStrategy by enum(ExceptionComparingStrategy.STACKTRACE)

    @get:Tag
    @get:XCollection(style = XCollection.Style.v1, elementName = "option")
    var gradleOptions by property(emptyList<String>()) { it.isEmpty() }

    @get:Tag
    @get:XCollection(
        style = XCollection.Style.v1,
        elementName = "stage",
        elementTypes = [FunctionLevelStage::class, DeclarationGraphStage::class, FileLevelStage::class],
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
        val defaultStages: List<MinimizationStage> = listOf(
            FunctionLevelStage(),
            DeclarationGraphStage(),
            FileLevelStage(),
        )
        val defaultTransformations: List<TransformationDescriptor> = listOf(
            TransformationDescriptor.PATH_RELATIVIZATION,
        )
    }
}
