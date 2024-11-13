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

    fun updateState(newState: MinimizationPluginState) {
        stateObservable
            .apply {
                var compilationStrategy by compilationStrategy.mutable()
                compilationStrategy = newState.compilationStrategy

                var gradleTask by gradleTask.mutable()
                gradleTask = newState.gradleTask

                var gradleOptions by gradleOptions.mutable()
                gradleOptions = newState.gradleOptions

                var temporaryProjectLocation by temporaryProjectLocation.mutable()
                temporaryProjectLocation = newState.temporaryProjectLocation

                var snapshotStrategy by snapshotStrategy.mutable()
                snapshotStrategy = newState.snapshotStrategy

                var exceptionComparingStrategy by exceptionComparingStrategy.mutable()
                exceptionComparingStrategy = newState.exceptionComparingStrategy

                var stages by stages.mutable()
                stages = newState.stages

                var minimizationTransformations by minimizationTransformations.mutable()
                minimizationTransformations = newState.minimizationTransformations
            }
    }
}
