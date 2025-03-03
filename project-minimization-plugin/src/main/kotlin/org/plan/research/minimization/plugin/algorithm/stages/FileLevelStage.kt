package org.plan.research.minimization.plugin.algorithm.stages

import arrow.core.raise.Raise
import mu.KLogger
import mu.KotlinLogging
import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDD
import org.plan.research.minimization.plugin.algorithm.MinimizationError
import org.plan.research.minimization.plugin.algorithm.file.FileTreeHierarchyFactory
import org.plan.research.minimization.plugin.context.HeavyIJDDContext
import org.plan.research.minimization.plugin.context.impl.FileLevelStageContext
import org.plan.research.minimization.plugin.context.snapshot.SnapshotMonad
import org.plan.research.minimization.plugin.util.withProgress

class FileLevelStage(
    private val baseAlgorithm: DDAlgorithm,
) : MinimizationStageBase<FileLevelStageContext>() {
    override val stageName: String = "File-Level"
    override val logger: KLogger = KotlinLogging.logger {}

    context(SnapshotMonad<FileLevelStageContext>, Raise<MinimizationError>)
    override suspend fun execute() {
        val hierarchicalDD = HierarchicalDD(baseAlgorithm)

        logger.info { "Initialise file hierarchy" }
        val hierarchy = FileTreeHierarchyFactory
            .createFromContext(context)
            .bind()

        logger.info { "Minimize" }
        withProgress {
            hierarchicalDD.minimize(hierarchy)
        }
    }

    context(Raise<MinimizationError>)
    override suspend fun createContext(context: HeavyIJDDContext<*>): FileLevelStageContext =
        FileLevelStageContext(context.projectDir, context.project, context.originalProject)
}