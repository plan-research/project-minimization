package org.plan.research.minimization.plugin.settings

import org.plan.research.minimization.plugin.services.MinimizationPluginSettings

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

fun saveStateToFile(project: Project, filePath: String) {
    val state = project.service<MinimizationPluginSettings>().state
    val element = serializeState(state)

    Files.newBufferedWriter(Paths.get(filePath)).use { writer ->
        JDOMUtil.writeElement(element, writer, System.lineSeparator())
    }
}

fun loadStateFromFile(project: Project, filePath: String) {
    val path = Paths.get(filePath)
    if (!Files.exists(path)) {
        throw NoSuchFileException(File(filePath))
    }

    val element = JDOMUtil.load(Files.newBufferedReader(path))
    deserializeState(project, element)
}

private fun serializeState(state: MinimizationPluginState): Element = XmlSerializer.serialize(state)

private fun deserializeState(project: Project, element: Element) {
    val newState = XmlSerializer.deserialize(element, MinimizationPluginState::class.java)
    project.service<MinimizationPluginSettings>().updateState(newState)
}
