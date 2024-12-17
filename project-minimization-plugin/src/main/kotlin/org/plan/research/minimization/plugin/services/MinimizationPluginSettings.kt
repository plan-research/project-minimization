package org.plan.research.minimization.plugin.services

import com.intellij.openapi.components.*
import org.plan.research.minimization.plugin.settings.MinimizationPluginState
import org.plan.research.minimization.plugin.settings.MinimizationPluginStateObservable

@Service(Service.Level.PROJECT)
@State(
    name = "org.plan.research.minimization.plugin.services.MinimizationPluginSettings",
    storages = [Storage("MinimizationPluginSettings.xml")],
)
class MinimizationPluginSettings : PersistentStateComponent<MinimizationPluginState> {
    val stateObservable = MinimizationPluginStateObservable()

    @Volatile
    var freezeSettings: Boolean = false

    inline fun <T> withFrozenState(block: () -> T): T {
        try {
            freezeSettings = true
            return block()
        } finally {
            freezeSettings = false
        }
    }

    override fun getState(): MinimizationPluginState =
        stateObservable.state

    override fun loadState(newState: MinimizationPluginState) {
        stateObservable.updateState(newState)
    }
}
