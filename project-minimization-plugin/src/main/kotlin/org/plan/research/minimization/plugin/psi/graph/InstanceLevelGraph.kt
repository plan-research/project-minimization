package org.plan.research.minimization.plugin.psi.graph

import arrow.core.Option
import arrow.core.getOrNone
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList
import org.plan.research.minimization.core.utils.graph.GraphToImageDumper
import org.plan.research.minimization.plugin.model.PsiStubDDItem

data class InstanceLevelGraph(
    override val vertices: List<PsiStubDDItem>,
    val edges: Collection<IJEdge>
) :
    GraphWithAdjacencyList<PsiStubDDItem, IJEdge>() {
    private val adjacencyList: Map<PsiStubDDItem, List<IJEdge>> = edges.groupBy { it.from }
    private val reverseAdjacencyList: Map<PsiStubDDItem, List<IJEdge>> = edges.groupBy { it.to }
    override fun inDegreeOf(vertex: PsiStubDDItem) = reverseAdjacencyList[vertex]?.size ?: 0
    override fun outDegreeOf(vertex: PsiStubDDItem) = adjacencyList[vertex]?.size ?: 0
    override fun edgesFrom(vertex: PsiStubDDItem) = adjacencyList.getOrNone(vertex)
    override fun toString(): String =
        GraphToImageDumper.dumpGraph(this, stringify = { it.childrenPath.last().toString() }).toString()
}