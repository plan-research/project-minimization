package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.context.WithImportRefCounterContext
import org.plan.research.minimization.plugin.model.context.WithInstanceLevelGraphContext
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem
import org.plan.research.minimization.plugin.model.monad.IJDDContextMonad

class GraphFunctionDeletingLens<C> : FunctionDeletingLens<C>() where C : WithInstanceLevelGraphContext<C>, C : WithImportRefCounterContext<C> {
    context(IJDDContextMonad<C>)
    override fun prepare(itemsToDelete: List<PsiStubDDItem>) {
        super.prepare(itemsToDelete)
        val graph = context.graph
        val itemsSet = itemsToDelete.toSet()
        val verticesToDelete = graph.vertices.filter { it.underlyingVertexes.all { it !in itemsSet } }.toSet()  // FIXME
        updateContext { context.copy(graph = graph.withoutNodes(verticesToDelete)) }
    }
}
