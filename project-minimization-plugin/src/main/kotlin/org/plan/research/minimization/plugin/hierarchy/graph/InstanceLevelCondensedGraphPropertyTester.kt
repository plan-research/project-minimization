package org.plan.research.minimization.plugin.hierarchy.graph

import com.intellij.util.io.createDirectories
import guru.nidi.graphviz.attribute.Attributes
import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.ForNode
import guru.nidi.graphviz.engine.Engine
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.graph
import guru.nidi.graphviz.model.MutableGraph
import mu.KotlinLogging
import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.PropertyTester
import org.plan.research.minimization.core.model.PropertyTesterWithGraph
import org.plan.research.minimization.core.model.graph.GraphCut
import org.plan.research.minimization.core.utils.graph.GraphToImageDumper
import org.plan.research.minimization.core.utils.graph.NodeAttributes
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.PsiStubDDItem
import org.plan.research.minimization.plugin.psi.graph.CondensedInstanceLevelEdge
import org.plan.research.minimization.plugin.psi.graph.CondensedInstanceLevelGraph
import org.plan.research.minimization.plugin.psi.graph.CondensedInstanceLevelNode
import org.plan.research.minimization.plugin.psi.graph.InstanceLevelGraph
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.writeText

private typealias NodeAttributes = Array<Attributes<out ForNode>>

/**
 * A class that tests properties of a condensed instance-level graph within a delta debugging context
 * utilizing a backing linear property tester.
 *
 *
 * @property backingPropertyTester An instance of [PropertyTester] used for testing linearized graph elements.
 */
class InstanceLevelCondensedGraphPropertyTester(val backingPropertyTester: PropertyTester<IJDDContext, PsiStubDDItem>) :
    PropertyTesterWithGraph<
            IJDDContext,
            CondensedInstanceLevelNode,
            CondensedInstanceLevelEdge,
            CondensedInstanceLevelGraph
            > {
    /**
     * A logging location for the saved condensed graph. The graph dumps are used for the debugging.
     */
    private val loggingLocation = Path(System.getProperty("idea.log.path"), "graphs")
    private var iteration = 0

    @Suppress("unused")
    private val logger = KotlinLogging.logger { }

    init {
        if (!loggingLocation.exists()) {
            loggingLocation.createDirectories()
        }
    }

    override suspend fun test(
        context: IJDDContext,
        cut: GraphCut<CondensedInstanceLevelNode>
    ): PropertyTestResult<IJDDContext> {
        return backingPropertyTester.test(
            context,
            cut.selectedVertices.flatMap { it.underlyingVertexes }
        )
//            .also {
//                val graph =
//                    requireNotNull(context.graph) { "To use graph algorithms graph should be set and condensed graph should exists" }
                  // FIXME: add a settings flag to enable it
//                logger.debug { "Testing iteration=$iteration, result=$it" }
//                graph.dump(cut)
//            }
    }

    @Suppress("unused")
    private fun CondensedInstanceLevelGraph.dump(cut: GraphCut<CondensedInstanceLevelNode>) {
        val cutVertices = cut.selectedVertices.flatMap { it.underlyingVertexes }.toSet()

        val stringifyFunction = { it: PsiStubDDItem ->
            it.toString()
        }
        val nodeAttributes: (PsiStubDDItem) -> NodeAttributes = { it: PsiStubDDItem ->
            arrayOf(
                when (it) {
                    in cutVertices -> Color.RED
                    else -> Color.BLACK
                }
            )
        }
        val instanceLevelGraph = InstanceLevelGraph(
            vertices.flatMap { it.underlyingVertexes },
            edges.flatMap { it.originalEdges } + vertices.flatMap { it.edgesInCondensed }
        )
        val representation = GraphToImageDumper.dumpGraph(
            instanceLevelGraph,
            stringify = stringifyFunction,
            nodeAttributes = nodeAttributes
        )
        val clusters = addClusters(stringifyFunction, nodeAttributes)
        val representationWithClusters = representation.toMutable().add(clusters.map { it.setCluster(true) })

        val projectRoot = loggingLocation
        val graphViz = Graphviz.fromGraph(representationWithClusters).height(5000).width(5000)
        projectRoot.resolve("${iteration}.dot").writeText(representationWithClusters.toString())
        graphViz.render(Format.SVG).toFile(projectRoot.resolve("${iteration}.svg").toFile())
        iteration++
    }

    private fun CondensedInstanceLevelGraph.addClusters(
        stringify: (PsiStubDDItem) -> String,
        nodeAttributes: (PsiStubDDItem) -> NodeAttributes,
    ): List<MutableGraph> = vertices
        .mapIndexed { idx, it ->
            graph("cluster_${idx}") {
                it.underlyingVertexes.forEach {
                    stringify(it).get(*nodeAttributes(it))
                }
            }
        }
}