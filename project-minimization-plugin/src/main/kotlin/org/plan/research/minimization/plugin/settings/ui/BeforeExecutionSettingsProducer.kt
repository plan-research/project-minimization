package org.plan.research.minimization.plugin.settings.ui

import org.plan.research.minimization.plugin.settings.MinimizationPluginSettingsConfigurable

import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.dialog
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel

class BeforeExecutionSettingsProducer(project: Project) : SettingsProducerBase(project) {
    private var onOtherSettings: (() -> Unit)? = null

    fun onOtherSettings(listener: () -> Unit) {
        onOtherSettings = listener
    }

    override fun getPanel(): DialogPanel =
        panel {
            gradleTask()
            gradleOptions()

            separator()

            pathPanel()

            row {
                link("Other settings") {
                    ShowSettingsUtil.getInstance()
                        .showSettingsDialog(project, MinimizationPluginSettingsConfigurable::class.java)
                    onOtherSettings?.invoke()
                }.align(AlignX.RIGHT)
            }
        }
}

fun beforeExecutionDialog(project: Project): DialogWrapper {
    val producer = BeforeExecutionSettingsProducer(project)
    val panel = producer.getPanel()
    val dialog = dialog("Minimization Plugin Settings", panel) {
        panel.validateAll()
    }

    producer.onOtherSettings { dialog.doCancelAction() }

    return dialog
}
