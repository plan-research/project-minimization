package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDD
import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.execution.SameExceptionPropertyTester
import org.plan.research.minimization.plugin.getDDAlgorithm
import org.plan.research.minimization.plugin.getExceptionComparator
import org.plan.research.minimization.plugin.getHierarchyCollectionStrategy
import org.plan.research.minimization.plugin.lenses.FunctionModificationLens
import org.plan.research.minimization.plugin.logging.statLogger
import org.plan.research.minimization.plugin.model.*
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

    override suspend fun executeFileLevelStage(context: HeavyIJDDContext, fileLevelStage: FileLevelStage) = either {
        logger.info { "Start File level stage" }
        statLogger.info {
            "File level stage settings, " +
                "Hierarchy strategy: ${fileLevelStage.hierarchyCollectionStrategy}, " +
                "DDAlgorithm: ${fileLevelStage.ddAlgorithm}"
        }

        val lightContext = context.asLightContext()

        val baseAlgorithm = fileLevelStage.ddAlgorithm.getDDAlgorithm()
        val hierarchicalDD = HierarchicalDD(baseAlgorithm)

        logger.info { "Initialise file hierarchy" }
        val hierarchy = fileLevelStage
            .hierarchyCollectionStrategy
            .getHierarchyCollectionStrategy()
            .produce(lightContext)
            .getOrElse { raise(MinimizationError.HierarchyFailed(it)) }

        logger.info { "Minimize" }
        lightContext.withProgress {
            hierarchicalDD.minimize(it, hierarchy)
        }
    }.logResult("File")

    override suspend fun executeFunctionLevelStage(
        context: HeavyIJDDContext,
        functionLevelStage: FunctionLevelStage,
    ) = either {
        logger.info { "Start Function level stage" }
        statLogger.info {
            "Function level stage settings. DDAlgorithm: ${functionLevelStage.ddAlgorithm}"
        }

        val lightContext = context.asLightContext()

        val ddAlgorithm = functionLevelStage.ddAlgorithm.getDDAlgorithm()
        val lens = FunctionModificationLens()
        val firstLevel = service<MinimizationPsiManagerService>()
            .findAllPsiWithBodyItems(lightContext)
        val propertyChecker = SameExceptionPropertyTester.create<PsiChildrenPathDDItem>(
            project.service<BuildExceptionProviderService>(),
            project.service<MinimizationPluginSettings>().state
                .exceptionComparingStrategy
                .getExceptionComparator(),
            lens,
            lightContext,
        ).getOrElse {
            logger.error { "Property checker creation failed. Aborted" }
            raise(MinimizationError.PropertyCheckerFailed)
        }

        firstLevel.logPsiElements(lightContext)
        lightContext
            .copy(currentLevel = firstLevel)
            .withProgress {
                ddAlgorithm.minimize(
                    it,
                    firstLevel,
                    propertyChecker,
                ).context
            }
    }.logResult("Function Body Replacement")

    private suspend fun<T : PsiChildrenPathIndex> List<PsiDDItem<T>>.logPsiElements(context: IJDDContext) {
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

    private fun <A, B> Either<A, B>.logResult(stageName: String) = onRight {
        logger.info { "End $stageName level stage" }
        statLogger.info { "$stageName level stage result: success" }
    }.onLeft { error ->
        logger.info { "End $stageName level stage" }
        statLogger.info { "$stageName level stage result: $error" }
        logger.error { "$stageName level stage failed with error: $error" }
    }
}
