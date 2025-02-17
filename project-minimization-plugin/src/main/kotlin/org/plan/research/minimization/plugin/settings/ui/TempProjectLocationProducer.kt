package org.plan.research.minimization.plugin.settings.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.BrowseFolderDescriptor.Companion.withPathToTextConvertor
import com.intellij.openapi.ui.BrowseFolderDescriptor.Companion.withTextToPathConvertor
import com.intellij.ui.dsl.builder.Row
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.relativeTo

class TempProjectLocationProducer {
    private fun createFileChooser(projectLocation: Path?) = FileChooserDescriptor(
        false, true, false, false, false, false,
    ).withTextToPathConvertor {
        projectLocation?.resolve(it)?.absolutePathString() ?: it
    }.withPathToTextConvertor {
        projectLocation?.let { loc -> Path(it).relativeTo(loc).toString() } ?: it
    }

    fun Row.create(project: Project, projectLocation: Path?) =
        textFieldWithBrowseButton(createFileChooser(projectLocation), project)
}
