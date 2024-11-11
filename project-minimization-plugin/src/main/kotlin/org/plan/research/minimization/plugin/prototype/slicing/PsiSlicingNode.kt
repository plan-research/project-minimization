package org.plan.research.minimization.plugin.prototype.slicing

import com.intellij.openapi.components.service
import org.plan.research.minimization.core.model.SlicingGraphNode
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem

data class PsiSlicingNode(
    val underlyingItem: PsiWithBodyDDItem,
    val context: IJDDContext,
) : SlicingGraphNode {
    override suspend fun getOutwardEdges() =
        context.indexProject.service<PsiGraphConstructor>()
        .getEdgesFromNode(this)
}