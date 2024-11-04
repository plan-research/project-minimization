package org.plan.research.minimization.plugin.settings

import com.intellij.openapi.components.BaseState

class MinimizationPluginStateObservable : BaseState() {
    private var localState = MinimizationPluginState(AppSettings.getInstance())
    var compilationStrategy = StateDelegate(
        getter = { localState.compilationStrategy },
        setter = { localState.compilationStrategy = it },
    )
    var temporaryProjectLocation = StateDelegate(
        getter = { localState.temporaryProjectLocation },
        setter = { localState.temporaryProjectLocation = it },
    )
    var snapshotStrategy = StateDelegate(
        getter = { localState.snapshotStrategy },
        setter = { localState.snapshotStrategy = it },
    )
    var exceptionComparingStrategy = StateDelegate(
        getter = { localState.exceptionComparingStrategy },
        setter = { localState.exceptionComparingStrategy = it },
    )
    var stages = StateDelegate(
        getter = { localState.stages },
        setter = { localState.stages = it },
    )
    var minimizationTransformations = StateDelegate(
        getter = { localState.minimizationTransformations },
        setter = { localState.minimizationTransformations = it },
    )

    fun updateSettingsState() {
        localState = MinimizationPluginState(AppSettings.getInstance())
    }
}

class StateDelegate<T>(private val getter: () -> T, private val setter: (T) -> Unit) {
    private val subscribers = mutableListOf<ChangeDelegate<*>>()

    fun <V> onChange(transform: (T) -> V) = ChangeDelegate(transform).also { subscribers.add(it) }
    fun mutable() = MutableChangeDelegate()

    inner class ChangeDelegate<V>(private val transform: (T) -> V) {
        private var value: V = transform(getter())

        operator fun getValue(thisRef: Any?, property: Any?): V = value

        fun onValueChanged(newValue: T) {
            value = transform(newValue)
        }
    }

    inner class MutableChangeDelegate {
        operator fun setValue(thisRef: Any?, property: Any?, value: T) {
            setter(value)
            subscribers.forEach { it.onValueChanged(value) }
        }
        operator fun getValue(thisRef: Any?, property: Any?): T = getter()
    }
}
