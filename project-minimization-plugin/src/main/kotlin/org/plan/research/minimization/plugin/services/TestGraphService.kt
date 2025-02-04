package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.core.GraphToImageDumper
import org.plan.research.minimization.plugin.context.impl.DefaultProjectContext
import org.plan.research.minimization.plugin.modification.graph.PsiIJEdge

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.Style
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class TestGraphService(private val project: Project, private val coroutineScope: CoroutineScope) {
    fun dumpGraph() = coroutineScope.launch {
        val context = DefaultProjectContext(project)
        val graph = service<MinimizationPsiManagerService>()
            .buildDeletablePsiGraph(context, true)
        val representation = GraphToImageDumper.dumpGraph(
            graph,
            stringify = { it.toString() },
            edgeAttributes = { edge ->
                when (edge) {
                    is PsiIJEdge.PSITreeEdge -> arrayOf(Style.SOLID)
                    is PsiIJEdge.UsageInPSIElement -> arrayOf(Style.DOTTED)
                    is PsiIJEdge.Overload -> arrayOf(Style.DASHED)
                    is PsiIJEdge.ObligatoryOverride -> arrayOf(Style.DASHED, Color.RED)
                    is PsiIJEdge.FileTreeEdge -> arrayOf(Style.SOLID, Color.SALMON)
                }
            },
        )
        val projectRoot = project.guessProjectDir()!!.toNioPath()
        val graphViz = Graphviz.fromGraph(representation)
        val file = projectRoot.resolve("instance-level-graph.svg").toFile()
        
        withContext(Dispatchers.IO) {
            graphViz.render(Format.SVG).toFile(file)
        }
        
        val vf = readAction { LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file) }

        vf?.let { virtualFile ->
            withContext(Dispatchers.EDT) {
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
            }
        }
    }
}
