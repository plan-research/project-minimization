package org.plan.research.minimization.plugin.algorithm.stages

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.impl.graph.GraphDD
import org.plan.research.minimization.core.algorithm.dd.withCondensation
import org.plan.research.minimization.plugin.algorithm.MinimizationError
import org.plan.research.minimization.plugin.algorithm.tester.PropertyTesterFactory
import org.plan.research.minimization.plugin.context.HeavyIJDDContext
import org.plan.research.minimization.plugin.context.impl.DeclarationLevelStageContext
import org.plan.research.minimization.plugin.context.snapshot.SnapshotMonad
import org.plan.research.minimization.plugin.modification.item.PsiStubDDItem
import org.plan.research.minimization.plugin.modification.lenses.FunctionDeletingLens
import org.plan.research.minimization.plugin.modification.psi.CallTraceParameterCache
import org.plan.research.minimization.plugin.modification.psi.KtSourceImportRefCounter
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService
import org.plan.research.minimization.plugin.util.WithProgressReporterMonadProvider

import arrow.core.getOrElse
import arrow.core.raise.Raise
import com.intellij.openapi.components.service
import mu.KLogger
import mu.KotlinLogging

class DeclarationGraphLevelStage(
    private val isFunctionParametersEnabled: Boolean,
    private val ddAlgorithm: DDAlgorithm,
) : MinimizationStageBase<DeclarationLevelStageContext>() {
    override val stageName: String = "Declaration-Graph-Level"
    override val logger: KLogger = KotlinLogging.logger {}

    context(SnapshotMonad<DeclarationLevelStageContext>, Raise<MinimizationError>)
    override suspend fun execute() {
        val graphDD = GraphDD(ddAlgorithm, WithProgressReporterMonadProvider()).withCondensation()
        val propertyTester = PropertyTesterFactory
            .createGraphPropertyTester(
                FunctionDeletingLens(),
                context,
                stageName,
            ).getOrElse {
                logger.error { "Property checker creation failed. Aborted" }
                raise(MinimizationError.PropertyCheckerFailed)
            }

        graphDD.minimize(context.graph, propertyTester)
    }

    context(Raise<MinimizationError>)
    override suspend fun createContext(context: HeavyIJDDContext<*>): DeclarationLevelStageContext {
        val importRefCounter = KtSourceImportRefCounter.create(context).getOrElse {
            raise(MinimizationError.AnalysisFailed)
        }

        val graph = service<MinimizationPsiManagerService>()
            .buildDeletablePsiGraph(context, isFunctionParametersEnabled)

        val callTraceParameterCache = CallTraceParameterCache.create(
            context,
            graph.vertexSet().filterIsInstance<PsiStubDDItem.CallablePsiStubDDItem>(),
        )

        return DeclarationLevelStageContext(
            context.projectDir, context.project,
            context.originalProject, graph,
            importRefCounter, callTraceParameterCache,
        )
    }
}
