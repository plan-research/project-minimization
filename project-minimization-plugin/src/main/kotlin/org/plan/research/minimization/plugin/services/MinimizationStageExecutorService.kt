package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.core.algorithm.dd.hierarchical.HierarchicalDD
import org.plan.research.minimization.plugin.errors.MinimizationError
import org.plan.research.minimization.plugin.execution.SameExceptionPropertyTester
import org.plan.research.minimization.plugin.getDDAlgorithm
import org.plan.research.minimization.plugin.getExceptionComparator
import org.plan.research.minimization.plugin.getHierarchyCollectionStrategy
import org.plan.research.minimization.plugin.logging.statLogger
import org.plan.research.minimization.plugin.model.FileLevelStage
import org.plan.research.minimization.plugin.model.FunctionLevelStage
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.MinimizationStageExecutor
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem
import org.plan.research.minimization.plugin.psi.FunctionModificationLens
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings

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
    private val generalLogger = KotlinLogging.logger {}
    override suspend fun executeFileLevelStage(context: IJDDContext, fileLevelStage: FileLevelStage) = either {
        generalLogger.info { "Start File level stage" }
        statLogger.info {
            "File level stage settings, " +
                "Hierarchy strategy: ${fileLevelStage.hierarchyCollectionStrategy::class.simpleName}, " +
                "DDAlgorithm: ${fileLevelStage.ddAlgorithm::class.simpleName}"
        }

        val baseAlgorithm = fileLevelStage.ddAlgorithm.getDDAlgorithm()
        val hierarchicalDD = HierarchicalDD(baseAlgorithm)

        generalLogger.info { "Initialise file hierarchy" }
        val hierarchy = fileLevelStage
            .hierarchyCollectionStrategy
            .getHierarchyCollectionStrategy()
            .produce(context)
            .getOrElse { raise(MinimizationError.HierarchyFailed(it)) }

        generalLogger.info { "Minimize" }
        context.withProgress {
            hierarchicalDD.minimize(it, hierarchy)
        }
    }.logResult("File")

    override suspend fun executeFunctionLevelStage(
        context: IJDDContext,
        functionLevelStage: FunctionLevelStage,
    ) = either {
        generalLogger.info { "Start Function level stage" }
        statLogger.info {
            "Function level stage settings. DDAlgorithm: ${functionLevelStage.ddAlgorithm::class.simpleName}"
        }
        val ddAlgorithm = functionLevelStage.ddAlgorithm.getDDAlgorithm()
        val buildExceptionProvider = project.service<BuildExceptionProviderService>()
        val exceptionComparator = project
            .service<MinimizationPluginSettings>()
            .state
            .exceptionComparingStrategy
            .getExceptionComparator()
        val lens = FunctionModificationLens()
        val propertyChecker = SameExceptionPropertyTester.create<PsiWithBodyDDItem>(
            buildExceptionProvider,
            exceptionComparator,
            lens,
            context,
        ).getOrElse {
            generalLogger.error { "Property checker creation failed. Aborted" }
            raise(MinimizationError.PropertyCheckerFailed)
        }
        context.withProgress {
            ddAlgorithm.minimize(
                it,
                project.service<PsiAndRootManagerService>().findAllPsiWithBodyItems().also { it.logPsiElements() },
                propertyChecker,
            ).context
        }
    }.logResult("Function")

    private suspend fun List<PsiWithBodyDDItem>.logPsiElements() {
        if (!generalLogger.isDebugEnabled) {
            return
        }
        val psiManagingService = project.service<PsiAndRootManagerService>()
        val text = mapNotNull {
            psiManagingService.getPsiElementFromItem(it)?.let { readAction { it.text } }
        }
        generalLogger.debug {
            "Starting DD Algorithm with following elements:\n" +
                text.joinToString("\n") { "\t- $it" }
        }
    }

    private fun <A, B> Either<A, B>.logResult(stageName: String) = onRight {
        generalLogger.info { "End $stageName level stage" }
        statLogger.info { "$stageName level stage result: success" }
    }.onLeft { error ->
        generalLogger.info { "End $stageName level stage" }
        statLogger.info { "$stageName level stage result: $error" }
        generalLogger.error { "$stageName level stage failed with error: $error" }
    }
}
