package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.settings.MinimizationPluginState
import org.plan.research.minimization.plugin.settings.MinimizationPluginStateObservable

import com.intellij.openapi.components.*
import com.intellij.openapi.observable.properties.AtomicBooleanProperty

@Service(Service.Level.PROJECT)
@State(
    name = "org.plan.research.minimization.plugin.services.MinimizationPluginSettings",
    storages = [Storage("MinimizationPluginSettings.xml")],
)
class MinimizationPluginSettings : PersistentStateComponent<MinimizationPluginState> {
    val stateObservable = MinimizationPluginStateObservable()
    var settingsEnabled = AtomicBooleanProperty(true)

    inline fun <T> withFrozenState(block: () -> T): T {
        try {
            settingsEnabled.set(false)
            return block()
        } finally {
            settingsEnabled.set(true)
        }
    }

    override fun getState(): MinimizationPluginState =
        stateObservable.state

    override fun loadState(newState: MinimizationPluginState) {
        stateObservable.updateState(newState)
    }
}
