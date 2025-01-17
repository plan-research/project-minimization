package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.core.utils.graph.GraphToImageDumper
import org.plan.research.minimization.plugin.model.context.impl.DefaultProjectContext
import org.plan.research.minimization.plugin.psi.graph.PsiIJEdge

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.Style
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class TestGraphService(private val project: Project, private val coroutineScope: CoroutineScope) {
    fun dumpGraph() = coroutineScope.launch {
        val context = DefaultProjectContext(project)
        val graph = service<MinimizationPsiManagerService>()
            .buildDeletablePsiGraph(context)
        val representation = GraphToImageDumper.dumpGraph(
            graph,
            stringify = { it.toString() },
            edgeAttributes = { from, edge ->
                when (edge) {
                    is PsiIJEdge.PSITreeEdge -> arrayOf(Style.SOLID)
                    is PsiIJEdge.UsageInPSIElement -> arrayOf(Style.DOTTED)
                    is PsiIJEdge.Overload -> arrayOf(Style.DASHED)
                    is PsiIJEdge.ObligatoryOverride -> arrayOf(Style.DASHED, Color.RED)
                }
            },
        )
        val projectRoot = project.guessProjectDir()!!.toNioPath()
        val graphViz = Graphviz.fromGraph(representation)
        graphViz.render(Format.SVG).toFile(projectRoot.resolve("instance-level-graph.svg").toFile())
    }
}
