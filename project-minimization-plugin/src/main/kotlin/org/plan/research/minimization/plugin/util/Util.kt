package org.plan.research.minimization.plugin.util

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.impl.DDMin
import org.plan.research.minimization.core.algorithm.dd.impl.ProbabilisticDD
import org.plan.research.minimization.plugin.algorithm.stages.DeclarationGraphLevelStage
import org.plan.research.minimization.plugin.algorithm.stages.FileLevelStage
import org.plan.research.minimization.plugin.algorithm.stages.FunctionLevelStage
import org.plan.research.minimization.plugin.algorithm.stages.MinimizationStage
import org.plan.research.minimization.plugin.compilation.BuildExceptionProvider
import org.plan.research.minimization.plugin.compilation.DumbCompiler
import org.plan.research.minimization.plugin.compilation.comparator.SimpleExceptionComparator
import org.plan.research.minimization.plugin.compilation.comparator.StacktraceExceptionComparator
import org.plan.research.minimization.plugin.compilation.gradle.GradleBuildExceptionProvider
import org.plan.research.minimization.plugin.compilation.transformer.PathRelativizationTransformer
import org.plan.research.minimization.plugin.context.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.context.snapshot.impl.ProjectCloningSnapshotManager
import org.plan.research.minimization.plugin.context.snapshot.impl.ProjectLocalHistorySnapshotManager
import org.plan.research.minimization.plugin.logging.withLog
import org.plan.research.minimization.plugin.logging.withLogging
import org.plan.research.minimization.plugin.settings.data.CompilationStrategy
import org.plan.research.minimization.plugin.settings.data.DDStrategy
import org.plan.research.minimization.plugin.settings.data.DeclarationGraphStageData
import org.plan.research.minimization.plugin.settings.data.ExceptionComparingStrategy
import org.plan.research.minimization.plugin.settings.data.FileLevelStageData
import org.plan.research.minimization.plugin.settings.data.FunctionLevelStageData
import org.plan.research.minimization.plugin.settings.data.MinimizationStageData
import org.plan.research.minimization.plugin.settings.data.SnapshotStrategy
import org.plan.research.minimization.plugin.settings.data.TransformationDescriptor

import com.intellij.openapi.project.Project

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun SnapshotStrategy.getSnapshotManager(project: Project): SnapshotManager =
    when (this) {
        SnapshotStrategy.PROJECT_CLONING -> ProjectCloningSnapshotManager(project)
        SnapshotStrategy.LOCAL_STORAGE -> ProjectLocalHistorySnapshotManager()
    }

fun DDStrategy.getDDAlgorithm(): DDAlgorithm =
    when (this) {
        DDStrategy.DD_MIN -> DDMin()
        DDStrategy.PROBABILISTIC_DD -> ProbabilisticDD()
    }.withLog()

fun CompilationStrategy.getCompilationStrategy(): BuildExceptionProvider =
    when (this) {
        CompilationStrategy.GRADLE_IDEA -> GradleBuildExceptionProvider()
        CompilationStrategy.DUMB -> DumbCompiler
    }

fun ExceptionComparingStrategy.getExceptionComparator() = when (this) {
    ExceptionComparingStrategy.SIMPLE -> SimpleExceptionComparator()
    ExceptionComparingStrategy.STACKTRACE -> StacktraceExceptionComparator(SimpleExceptionComparator())
}.withLogging()

fun TransformationDescriptor.getExceptionTransformations() = when (this) {
    TransformationDescriptor.PATH_RELATIVIZATION -> PathRelativizationTransformer()
}

fun MinimizationStageData.getMinimizationStage(): MinimizationStage = when (this) {
    is DeclarationGraphStageData -> DeclarationGraphLevelStage(
        isFunctionParametersEnabled,
        ddAlgorithm.getDDAlgorithm(),
    )

    is FileLevelStageData -> FileLevelStage(ddAlgorithm.getDDAlgorithm())
    is FunctionLevelStageData -> FunctionLevelStage(ddAlgorithm.getDDAlgorithm())
}

fun getCurrentTimeString(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
