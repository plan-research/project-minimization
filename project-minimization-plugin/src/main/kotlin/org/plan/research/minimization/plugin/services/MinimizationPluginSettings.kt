package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.settings.MinimizationPluginState
import org.plan.research.minimization.plugin.settings.MinimizationPluginStateObservable

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.PROJECT)
@State(
    name = "org.plan.research.minimization.plugin.services.MinimizationPluginSettings",
    storages = [Storage("MinimizationPluginSettings.xml")],
)
class MinimizationPluginSettings : SimplePersistentStateComponent<MinimizationPluginState>(MinimizationPluginState()) {
    var stateObservable: MinimizationPluginStateObservable = MinimizationPluginStateObservable { state }
    var freezeSettings: Boolean = false
}
