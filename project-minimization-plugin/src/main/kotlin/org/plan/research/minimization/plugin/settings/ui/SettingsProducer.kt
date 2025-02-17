package org.plan.research.minimization.plugin.settings.ui

import com.intellij.openapi.observable.util.bindEnabled
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel

@Suppress("NO_CORRESPONDING_PROPERTY")
class SettingsProducer(project: Project) : SettingsProducerBase(project) {
    override fun getPanel(): DialogPanel =
        panel {
            tempProjectLocation()
            logsLocation()

            separator()

            // compilationStrategy()
            gradleTask()
            gradleOptions()

            separator()

            snapshotStrategy()
            exceptionComparingStrategy()

            separator()

            stagesSettings()

            // separator()
            // 
            // transformations()

            separator()

            pathPanel()
        }.bindEnabled(settings.settingsEnabled)
}
