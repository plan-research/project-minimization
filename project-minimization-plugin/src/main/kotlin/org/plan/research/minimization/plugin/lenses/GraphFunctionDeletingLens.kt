package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiStubDDItem

class GraphFunctionDeletingLens : FunctionDeletingLens() {
    override fun currentLevel(context: IJDDContext): List<PsiStubDDItem> =
        context.graph!!.vertices.flatMap { it.underlyingVertexes }

    override fun prepareContext(context: IJDDContext, items: List<PsiStubDDItem>): IJDDContext? {
        val graph = context.graph ?: return null
        val itemsSet = items.toSet()
        val verticesToDelete = graph.vertices.filter { it.underlyingVertexes.all { it !in itemsSet } }.toSet() // FIXME
        return context.copy(graph = graph.withoutNodes(verticesToDelete))
    }
}