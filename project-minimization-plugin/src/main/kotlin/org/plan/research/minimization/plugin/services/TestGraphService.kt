package org.plan.research.minimization.plugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import guru.nidi.graphviz.attribute.Style
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.plan.research.minimization.core.utils.graph.GraphToImageDumper
import org.plan.research.minimization.plugin.model.HeavyIJDDContext
import org.plan.research.minimization.plugin.psi.graph.IJEdge

@Service(Service.Level.PROJECT)
class TestGraphService(private val project: Project, private val coroutineScope: CoroutineScope) {
    fun dumpGraph() = coroutineScope.launch {
        val graph = service<MinimizationPsiManagerService>()
            .buildDeletablePsiGraph(HeavyIJDDContext(project))
        val representation = GraphToImageDumper.dumpGraph(
            graph,
            stringify = { it.childrenPath.lastOrNull()?.toString() ?: "File(localPath=${it.localPath})" },
            edgeAttributes = { from, edge ->
                when (edge) {
                    is IJEdge.PSITreeEdge -> arrayOf(Style.SOLID)
                    is IJEdge.UsageInPSIElement -> arrayOf(Style.DOTTED)
                    is IJEdge.Overload -> arrayOf(Style.DASHED)
                }
            }
        )
        val projectRoot = project.guessProjectDir()!!.toNioPath()
        val graphViz = Graphviz.fromGraph(representation)
        graphViz.render(Format.SVG).toFile(projectRoot.resolve("instance-level-graph.svg").toFile())
    }
}