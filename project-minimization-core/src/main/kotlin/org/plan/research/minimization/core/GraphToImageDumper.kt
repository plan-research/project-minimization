package org.plan.research.minimization.core

import guru.nidi.graphviz.attribute.Attributes
import guru.nidi.graphviz.attribute.ForLink
import guru.nidi.graphviz.attribute.ForNode
import guru.nidi.graphviz.attribute.Rank
import guru.nidi.graphviz.graph
import guru.nidi.graphviz.model.Graph
import org.jgrapht.Graph as JGraph

private typealias EdgeAttributes = Array<Attributes<out ForLink>>
private typealias NodeAttributes = Array<Attributes<out ForNode>>

object GraphToImageDumper {
    fun <V, E> dumpGraph(
        g: JGraph<V, E>,
        stringify: (V) -> String = { it.toString() },
        edgeAttributes: (E) -> EdgeAttributes = { emptyArray() },
        nodeAttributes: (V) -> NodeAttributes = { emptyArray() },
    ): Graph =
        graph(directed = true) {
            graph[Rank.dir(Rank.RankDir.TOP_TO_BOTTOM)]
            g.vertexSet().forEach { stringify(it).get(*nodeAttributes(it)) }
            g.edgeSet().forEach { edge ->
                val from = g.getEdgeSource(edge)
                val to = g.getEdgeTarget(edge)
                (stringify(from) - stringify(to)).get(*edgeAttributes(edge))
            }
        }.toImmutable()
}
