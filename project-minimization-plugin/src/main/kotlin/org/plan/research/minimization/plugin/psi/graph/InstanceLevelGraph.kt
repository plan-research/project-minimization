package org.plan.research.minimization.plugin.psi.graph

import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedEdge
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedGraph
import org.plan.research.minimization.core.algorithm.graph.condensation.CondensedVertex
import org.plan.research.minimization.core.model.graph.GraphCut
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList
import org.plan.research.minimization.core.utils.graph.GraphToImageDumper
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem

typealias CondensedInstanceLevelGraph = CondensedGraph<PsiStubDDItem, PsiIJEdge>
typealias CondensedInstanceLevelNode = CondensedVertex<PsiStubDDItem, PsiIJEdge>
typealias CondensedInstanceLevelEdge = CondensedEdge<PsiStubDDItem, PsiIJEdge>
private typealias CondensedInstanceLevelAdjacencyList = Map<PsiStubDDItem, List<PsiIJEdge>>

data class InstanceLevelGraph(
    override val vertices: List<PsiStubDDItem>,
    override val edges: List<PsiIJEdge>,
) :
    GraphWithAdjacencyList<PsiStubDDItem, PsiIJEdge, InstanceLevelGraph>() {
    private val reverseAdjacencyList: CondensedInstanceLevelAdjacencyList = edges.groupBy { it.to }
    override fun inDegreeOf(vertex: PsiStubDDItem) = reverseAdjacencyList[vertex]?.size ?: 0
    override fun toString(): String =
        GraphToImageDumper.dumpGraph(this, stringify = { it.childrenPath.last().toString() }).toString()

    override fun induce(cut: GraphCut<PsiStubDDItem>): InstanceLevelGraph {
        val filteredVertices = cut.selectedVertices
        val filteredEdges = edges.filter { it.from in filteredVertices && it.to in filteredVertices }
        return InstanceLevelGraph(filteredVertices.toList(), filteredEdges)
    }
}
