package org.plan.research.minimization.core.utils.graph

import org.plan.research.minimization.core.model.DDItem
import org.plan.research.minimization.core.model.graph.GraphEdge
import org.plan.research.minimization.core.model.graph.GraphWithAdjacencyList

import arrow.core.getOrElse
import guru.nidi.graphviz.attribute.Attributes
import guru.nidi.graphviz.attribute.ForLink
import guru.nidi.graphviz.attribute.Rank
import guru.nidi.graphviz.attribute.Rank.RankDir.TOP_TO_BOTTOM
import guru.nidi.graphviz.graph
import guru.nidi.graphviz.model.Graph

private typealias EdgeAttributes = Array<Attributes<out ForLink>>

object GraphToImageDumper {
    fun <V, E, G> dumpGraph(
        g: G,
        stringify: (V) -> String = Any::toString,
        edgeAttributes: (V, E) -> EdgeAttributes = { a, b -> emptyArray() },
    ): Graph where V : DDItem,
    E : GraphEdge<V>,
    G : GraphWithAdjacencyList<V, E, G> {
        return graph(directed = true) {
            graph[Rank.dir(TOP_TO_BOTTOM)]
            g.vertices.forEach { from ->
                -stringify(from)
                g
                    .edgesFrom(from)
                    .getOrElse { return@forEach }
                    .forEach { edge -> (stringify(from) - stringify(edge.to)).get(*edgeAttributes(from, edge)) }
            }
        }.toImmutable()
    }
}
