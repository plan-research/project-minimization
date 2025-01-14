package org.plan.research.minimization.plugin.psi.graph

import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.plugin.model.PsiStubDDItem

sealed class PsiIJEdge : GraphEdge<PsiStubDDItem>() {
    data class Overload(override val from: PsiStubDDItem, override val to: PsiStubDDItem) : PsiIJEdge()
    data class PSITreeEdge(override val from: PsiStubDDItem, override val to: PsiStubDDItem): PsiIJEdge()
    data class UsageInPSIElement(override val from: PsiStubDDItem, override val to: PsiStubDDItem): PsiIJEdge()
    data class ObligatoryOverride(override val from: PsiStubDDItem, override val to: PsiStubDDItem) : PsiIJEdge()
}