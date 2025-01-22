package org.plan.research.minimization.plugin.execution

import org.plan.research.minimization.core.model.PropertyTestResult
import org.plan.research.minimization.core.model.graph.GraphCut
import org.plan.research.minimization.core.utils.graph.GraphToImageDumper
import org.plan.research.minimization.plugin.benchmark.BenchmarkSettings
import org.plan.research.minimization.plugin.logging.ExecutionDiscriminator
import org.plan.research.minimization.plugin.logging.withLog
import org.plan.research.minimization.plugin.logging.withStatistics
import org.plan.research.minimization.plugin.model.BuildExceptionProvider
import org.plan.research.minimization.plugin.model.IJGraphPropertyTester
import org.plan.research.minimization.plugin.model.ProjectItemLens
import org.plan.research.minimization.plugin.model.context.IJDDContextBase
import org.plan.research.minimization.plugin.model.context.WithImportRefCounterContext
import org.plan.research.minimization.plugin.model.context.WithInstanceLevelGraphContext
import org.plan.research.minimization.plugin.model.exception.CompilationException
import org.plan.research.minimization.plugin.model.exception.ExceptionComparator
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem
import org.plan.research.minimization.plugin.model.monad.IJDDContextMonad
import org.plan.research.minimization.plugin.psi.graph.CondensedInstanceLevelGraph
import org.plan.research.minimization.plugin.psi.graph.CondensedInstanceLevelNode
import org.plan.research.minimization.plugin.psi.graph.InstanceLevelGraph

import arrow.core.raise.option
import com.intellij.openapi.project.Project
import com.intellij.util.io.createDirectories
import guru.nidi.graphviz.attribute.Attributes
import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.ForNode
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.graph
import guru.nidi.graphviz.model.MutableGraph
import mu.KotlinLogging

import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.properties.Delegates

private typealias NodeAttributes = Array<Attributes<out ForNode>>

/**
 * A class that tests properties of a condensed instance-level graph within a delta debugging context
 * utilizing a backing linear property tester.
 */
class GraphIjPropertyTester<C> private constructor(
    rootProject: Project,
    buildExceptionProvider: BuildExceptionProvider,
    comparator: ExceptionComparator,
    lens: ProjectItemLens<C, PsiStubDDItem>,
    initialException: CompilationException,
    listeners: Listeners<PsiStubDDItem> = emptyList(),
) :
    IJGraphPropertyTester<C, CondensedInstanceLevelNode> where C : WithImportRefCounterContext<C>,
C : IJDDContextBase<C>,
C : WithInstanceLevelGraphContext<C> {
    private val innerTester =
        LinearTester(rootProject, buildExceptionProvider, comparator, lens, initialException, listeners)
    private val loggableTester = innerTester
        .withLog()
        .let { if (BenchmarkSettings.isBenchmarkingEnabled) it.withStatistics() else it }

    /**
     * A logging location for the saved condensed graph. The graph dumps are used for the debugging.
     */
    private val loggingLocation = Path(ExecutionDiscriminator.loggingFolder.get(), "instance-level-graphs")
    private var iteration = 0

    @Suppress("unused")
    private val logger = KotlinLogging.logger { }

    init {
        if (!loggingLocation.exists()) {
            loggingLocation.createDirectories()
        }
    }

    context(IJDDContextMonad<C>)
    override suspend fun test(
        retainedCut: GraphCut<CondensedInstanceLevelNode>,
        deletedCut: GraphCut<CondensedInstanceLevelNode>,
    ): PropertyTestResult {
        if (logger.isTraceEnabled) {
            val graph = context.graph
            graph.dump(deletedCut)
        }
        innerTester.cut = deletedCut

        return loggableTester.test(
            retainedCut.selectedVertices.flatMap { it.underlyingVertexes },
            deletedCut.selectedVertices.flatMap { it.underlyingVertexes },
        )
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
                },
            )
        }
        val instanceLevelGraph = InstanceLevelGraph(
            vertices.flatMap { it.underlyingVertexes },
            edges.flatMap { it.originalEdges } + vertices.flatMap { it.edgesInCondensed },
        )
        val representation = GraphToImageDumper.dumpGraph(
            instanceLevelGraph,
            stringify = stringifyFunction,
            nodeAttributes = nodeAttributes,
        )
        val clusters = addClusters(stringifyFunction, nodeAttributes)
        val representationWithClusters = representation.toMutable().add(clusters.map { it.setCluster(true) })

        val projectRoot = loggingLocation
        val graphViz = Graphviz.fromGraph(representationWithClusters).height(DUMP_IMAGE_HEIGHT).width(DUMP_IMAGE_WIDTH)
        projectRoot.resolve("$iteration.dot").writeText(representationWithClusters.toString())
        graphViz.render(Format.SVG).toFile(projectRoot.resolve("$iteration.svg").toFile())
        iteration++
    }

    private fun CondensedInstanceLevelGraph.addClusters(
        stringify: (PsiStubDDItem) -> String,
        nodeAttributes: (PsiStubDDItem) -> NodeAttributes,
    ): List<MutableGraph> = vertices
        .mapIndexed { idx, it ->
            graph("cluster_$idx") {
                it.underlyingVertexes.forEach {
                    stringify(it).get(*nodeAttributes(it))
                }
            }
        }

    private inner class LinearTester(
        rootProject: Project,
        buildExceptionProvider: BuildExceptionProvider,
        comparator: ExceptionComparator,
        lens: ProjectItemLens<C, PsiStubDDItem>,
        initialException: CompilationException,
        listeners: Listeners<PsiStubDDItem> = emptyList(),
    ) : AbstractSameExceptionPropertyTester<C, PsiStubDDItem>(
        rootProject,
        buildExceptionProvider,
        comparator,
        lens,
        initialException,
        listeners,
    ) {
        var cut: GraphCut<CondensedInstanceLevelNode> by Delegates.notNull()

        context(IJDDContextMonad<C>)
        override suspend fun focus(itemsToDelete: List<PsiStubDDItem>) {
            updateContext { it.copy(graph = it.graph.without(cut)) }
            super.focus(itemsToDelete)
        }
    }

    companion object {
        private const val DUMP_IMAGE_HEIGHT = 5000
        private const val DUMP_IMAGE_WIDTH = 5000
        private val logger = KotlinLogging.logger {}
        suspend fun <C> create(
            compilerPropertyChecker: BuildExceptionProvider,
            exceptionComparator: ExceptionComparator,
            lens: ProjectItemLens<C, PsiStubDDItem>,
            context: C,
            listeners: Listeners<PsiStubDDItem> = emptyList(),
        ) where C : WithImportRefCounterContext<C>,
        C : IJDDContextBase<C>,
        C : WithInstanceLevelGraphContext<C> = option {
            val initialException = compilerPropertyChecker.checkCompilation(context).getOrNone().bind()
            GraphIjPropertyTester(
                context.originalProject,
                compilerPropertyChecker,
                exceptionComparator,
                lens,
                initialException,
                listeners,
            )
                .also { logger.debug { "Initial exception is $initialException" } }
        }
    }
}
