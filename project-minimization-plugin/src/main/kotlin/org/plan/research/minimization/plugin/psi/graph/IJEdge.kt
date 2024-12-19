package org.plan.research.minimization.plugin.psi.graph

import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.plugin.model.PsiStubDDItem

sealed interface IJEdge : GraphEdge<PsiStubDDItem> { // TODO: make it extendable
    val from: PsiStubDDItem
    data class Overload(override val from: PsiStubDDItem, override val to: PsiStubDDItem) : IJEdge
    data class PSITreeEdge(override val from: PsiStubDDItem, override val to: PsiStubDDItem): IJEdge
    data class UsageInPSIElement(override val from: PsiStubDDItem, override val to: PsiStubDDItem): IJEdge
}