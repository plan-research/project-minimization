package org.plan.research.minimization.plugin.algorithm.stages

import org.plan.research.minimization.core.algorithm.dd.DDAlgorithm
import org.plan.research.minimization.plugin.algorithm.MinimizationError
import org.plan.research.minimization.plugin.algorithm.tester.SameExceptionPropertyTester
import org.plan.research.minimization.plugin.compilation.BuildExceptionProvider
import org.plan.research.minimization.plugin.compilation.CompilationResult
import org.plan.research.minimization.plugin.compilation.DumbCompiler
import org.plan.research.minimization.plugin.context.HeavyIJDDContext
import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.context.impl.FunctionLevelStageContext
import org.plan.research.minimization.plugin.context.snapshot.SnapshotMonad
import org.plan.research.minimization.plugin.logging.LoggingPropertyCheckingListener
import org.plan.research.minimization.plugin.modification.lenses.FunctionModificationLens
import org.plan.research.minimization.plugin.services.MinimizationPluginSettings
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService
import org.plan.research.minimization.plugin.util.getExceptionComparator

import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.right
import com.intellij.openapi.components.service
import mu.KLogger
import mu.KotlinLogging

class FunctionLevelStage(
    private val ddAlgorithm: DDAlgorithm,
) : MinimizationStageBase<FunctionLevelStageContext>() {
    override val stageName: String = "Function-Level"
    override val logger: KLogger = KotlinLogging.logger {}

    context(SnapshotMonad<FunctionLevelStageContext>, Raise<MinimizationError>)
    override suspend fun execute() {
        val lens = FunctionModificationLens<FunctionLevelStageContext>()
        val firstLevel = service<MinimizationPsiManagerService>()
            .findAllPsiWithBodyItems(context)

        // val propertyChecker = PropertyTesterFactory
        // .createPropertyTester(lens, context, stageName)
        // .getOrElse {
        // logger.error { "Property checker creation failed. Aborted" }
        // raise(MinimizationError.PropertyCheckerFailed)
        // }

        // TODO: Remove
        class MyExceptionProvider : BuildExceptionProvider {
            private val THROWABLE = Throwable("Test")
            override suspend fun checkCompilation(context: IJDDContext): CompilationResult =
                DumbCompiler.DumbException(THROWABLE).right()
        }

        val propertyChecker = SameExceptionPropertyTester.create(
            MyExceptionProvider(),
            context.originalProject.service<MinimizationPluginSettings>().state
                .exceptionComparingStrategy
                .getExceptionComparator(),
            lens,
            context,
            listOfNotNull(LoggingPropertyCheckingListener.create(stageName)),
        ).getOrElse {
            logger.error { "Property checker creation failed. Aborted" }
            raise(MinimizationError.PropertyCheckerFailed)
        }

        ddAlgorithm.minimize(
            firstLevel,
            propertyChecker,
        )
    }

    context(Raise<MinimizationError>)
    override suspend fun createContext(context: HeavyIJDDContext<*>): FunctionLevelStageContext =
        FunctionLevelStageContext(context.projectDir, context.project, context.originalProject)
}
