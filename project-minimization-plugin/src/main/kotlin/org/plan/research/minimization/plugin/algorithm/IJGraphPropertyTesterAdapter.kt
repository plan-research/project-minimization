package org.plan.research.minimization.plugin.algorithm

import org.plan.research.minimization.core.model.GraphCut
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.plugin.compilation.BuildExceptionProvider
import org.plan.research.minimization.plugin.compilation.comparator.ExceptionComparator
import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.context.IJDDContextBase
import org.plan.research.minimization.plugin.context.snapshot.SnapshotMonad
import org.plan.research.minimization.plugin.modification.item.PsiStubDDItem
import org.plan.research.minimization.plugin.modification.lenses.ProjectItemLens

import arrow.core.raise.option

/**
 * A class that tests properties of a condensed instance-level graph within a delta debugging context
 * utilizing a backing linear property tester.
 */
class IJGraphPropertyTesterAdapter<C : IJDDContext> private constructor(
    private val innerTester: IJPropertyTester<C, PsiStubDDItem>,
) : IJGraphPropertyTester<C, PsiStubDDItem> {
    context(SnapshotMonad<C>)
    override suspend fun test(
        retainedCut: GraphCut<PsiStubDDItem>,
        deletedCut: GraphCut<PsiStubDDItem>,
    ): PropertyTestResult = innerTester.test(
        retainedCut.toList(),
        deletedCut.toList(),
    )

    companion object {
        suspend fun <C : IJDDContextBase<C>> create(
            compilerPropertyChecker: BuildExceptionProvider,
            exceptionComparator: ExceptionComparator,
            lens: ProjectItemLens<C, PsiStubDDItem>,
            context: C,
            listeners: Listeners<PsiStubDDItem> = emptyList(),
        ) =
            option {
                val tester = SameExceptionPropertyTester.create(
                    compilerPropertyChecker,
                    exceptionComparator,
                    lens,
                    context,
                    listeners,
                ).bind()
                IJGraphPropertyTesterAdapter(tester)
            }
    }
}
