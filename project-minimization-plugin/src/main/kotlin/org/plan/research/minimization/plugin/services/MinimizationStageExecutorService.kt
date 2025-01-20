package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.core.algorithm.dd.hierarchical.ReversedHierarchicalDD
import org.plan.research.minimization.core.algorithm.graph.condensation.StrongConnectivityCondensation
import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.execution.LinearIjPropertyTester
import org.plan.research.minimization.plugin.getDDAlgorithm
import org.plan.research.minimization.plugin.getExceptionComparator
import org.plan.research.minimization.plugin.hierarchy.DeletablePsiElementHierarchyGenerator
import org.plan.research.minimization.plugin.hierarchy.FileTreeHierarchyGenerator
import org.plan.research.minimization.plugin.hierarchy.graph.InstanceLevelLayerHierarchyBuilder
import org.plan.research.minimization.plugin.lenses.FunctionModificationLens
import org.plan.research.minimization.plugin.logging.LoggingPropertyCheckingListener
import org.plan.research.minimization.plugin.logging.statLogger
import org.plan.research.minimization.plugin.model.DeclarationGraphStage
import org.plan.research.minimization.plugin.model.DeclarationLevelStage
import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.FunctionLevelStage
import org.plan.research.minimization.plugin.model.MinimizationStageExecutor
import org.plan.research.minimization.plugin.model.context.*
import org.plan.research.minimization.plugin.model.context.impl.DeclarationGraphLevelStageContext
import org.plan.research.minimization.plugin.model.context.impl.DeclarationLevelStageContext
import org.plan.research.minimization.plugin.model.context.impl.FileLevelStageContext
import org.plan.research.minimization.plugin.model.context.impl.FunctionLevelStageContext
import org.plan.research.minimization.plugin.model.item.PsiDDItem
import org.plan.research.minimization.plugin.model.item.index.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.model.monad.*
import org.plan.research.minimization.plugin.psi.KtSourceImportRefCounter
import org.plan.research.minimization.plugin.psi.PsiUtils

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import mu.KotlinLogging

@Service(Service.Level.PROJECT)
class MinimizationStageExecutorService(private val project: Project) : MinimizationStageExecutor {
    private val logger = KotlinLogging.logger {}

    override suspend fun executeFileLevelStage(context: HeavyIJDDContext<*>, fileLevelStage: FileLevelStage) = either {
        logger.info { "Start File level stage" }
        statLogger.info {
            "File level stage settings, " +
                "DDAlgorithm: ${fileLevelStage.ddAlgorithm}"
        }

        val lightContext = FileLevelStageContext(context.projectDir, context.project, context.originalProject)

        val baseAlgorithm = fileLevelStage.ddAlgorithm.getDDAlgorithm()
        val hierarchicalDD = ReversedHierarchicalDD(baseAlgorithm)

        logger.info { "Initialise file hierarchy" }
        val hierarchy = FileTreeHierarchyGenerator<FileLevelStageContext>()
            .produce(lightContext)
            .getOrElse { raise(MinimizationError.HierarchyFailed(it)) }

        logger.info { "Minimize" }
        lightContext.runMonadWithProgress {
            hierarchicalDD.minimize(hierarchy)
        }
    }.logResult("File")

    override suspend fun executeFunctionLevelStage(
        context: HeavyIJDDContext<*>,
        functionLevelStage: FunctionLevelStage,
    ) = either {
        logger.info { "Start Function level stage" }
        statLogger.info {
            "Function level stage settings. DDAlgorithm: ${functionLevelStage.ddAlgorithm}"
        }

        val lightContext = FunctionLevelStageContext(context.projectDir, context.project, context.originalProject)

        val ddAlgorithm = functionLevelStage.ddAlgorithm.getDDAlgorithm()
        val lens = FunctionModificationLens<FunctionLevelStageContext>()
        val firstLevel = service<MinimizationPsiManagerService>()
            .findAllPsiWithBodyItems(lightContext)
        val propertyChecker = LinearIjPropertyTester.create(
            project.service<BuildExceptionProviderService>(),
            project.service<MinimizationPluginSettings>().state
                .exceptionComparingStrategy
                .getExceptionComparator(),
            lens,
            lightContext,
            listOfNotNull(LoggingPropertyCheckingListener.create("body-replacement")),
        ).getOrElse {
            logger.error { "Property checker creation failed. Aborted" }
            raise(MinimizationError.PropertyCheckerFailed)
        }

        firstLevel.logPsiElements(lightContext)
        lightContext.runMonad {
            ddAlgorithm.minimize(
                firstLevel,
                propertyChecker,
            )
        }
    }.logResult("Function Body Replacement")

    private suspend fun <T : PsiChildrenPathIndex> List<PsiDDItem<T>>.logPsiElements(context: IJDDContext) {
        if (!logger.isTraceEnabled) {
            return
        }
        val text = mapNotNull {
            readAction { PsiUtils.getPsiElementFromItem(context, it)?.text }
        }
        logger.trace {
            "Starting DD Algorithm with following elements:\n" +
                text.joinToString("\n") { "\t- $it" }
        }
    }

    override suspend fun executeDeclarationLevelStage(
        context: HeavyIJDDContext<*>,
        declarationLevelStage: DeclarationLevelStage,
    ) = either {
        logger.info { "Start Function Deleting stage" }
        statLogger.info {
            "Function deleting stage settings, " +
                "DDAlgorithm: ${declarationLevelStage.ddAlgorithm}"
        }

        val importRefCounter = KtSourceImportRefCounter.create(context).getOrElse {
            raise(MinimizationError.AnalysisFailed)
        }

        val lightContext = DeclarationLevelStageContext(
            context.projectDir, context.project,
            context.originalProject, importRefCounter,
        )

        val ddAlgorithm = declarationLevelStage.ddAlgorithm.getDDAlgorithm()
        val hierarchicalDD = ReversedHierarchicalDD(ddAlgorithm)
        val hierarchy = DeletablePsiElementHierarchyGenerator<DeclarationLevelStageContext>(declarationLevelStage.depthThreshold)
            .produce(lightContext)
            .getOrElse { raise(MinimizationError.HierarchyFailed(it)) }

        lightContext.runMonadWithProgress {
            hierarchicalDD.minimize(hierarchy)
        }
    }.logResult("Function Deleting")

    override suspend fun executeDeclarationGraphStage(
        context: HeavyIJDDContext<*>,
        declarationGraphStage: DeclarationGraphStage,
    ) = either {
        logger.info { "Start Function Deleting Graph stage" }
        statLogger.info {
            "Function deleting Graph stage settings, " +
                "DDAlgorithm: ${declarationGraphStage.ddAlgorithm}"
        }

        val importRefCounter = KtSourceImportRefCounter.create(context).getOrElse {
            raise(MinimizationError.AnalysisFailed)
        }
        val graph = service<MinimizationPsiManagerService>().buildDeletablePsiGraph(context)
        val condensedGraph = StrongConnectivityCondensation.compressGraph(graph)

        val lightContext = DeclarationGraphLevelStageContext(
            context.projectDir, context.project,
            context.originalProject, importRefCounter,
            condensedGraph,
        )

        val ddAlgorithm = declarationGraphStage.ddAlgorithm.getDDAlgorithm(isZeroTesting = true)
        val hierarchicalDD = ReversedHierarchicalDD(ddAlgorithm)
        val hierarchy = InstanceLevelLayerHierarchyBuilder<DeclarationGraphLevelStageContext>()
            .produce(lightContext)
            .getOrElse { raise(MinimizationError.HierarchyFailed(it)) }

        lightContext.runMonadWithProgress {
            hierarchicalDD.minimize(hierarchy)
        }
    }.logResult("Function Deleting Graph")

    private fun <A, B> Either<A, B>.logResult(stageName: String) = onRight {
        logger.info { "End $stageName level stage" }
        statLogger.info { "$stageName level stage result: success" }
    }.onLeft { error ->
        logger.info { "End $stageName level stage" }
        statLogger.info { "$stageName level stage result: $error" }
        logger.error { "$stageName level stage failed with error: $error" }
    }

    private suspend inline fun <C : IJDDContext> C.runMonadWithProgress(
        action: IJContextWithProgressMonadF<C, Unit>,
    ): C = runMonad { withProgress(action) }

    private inline fun <C : IJDDContext> C.runMonad(
        action: context(IJDDContextMonad<C>) () -> Unit,
    ): C {
        val monad = IJDDContextMonad(this)
        action(monad)
        return monad.context
    }
}
