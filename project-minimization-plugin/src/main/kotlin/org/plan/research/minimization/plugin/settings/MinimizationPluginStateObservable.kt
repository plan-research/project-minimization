package org.plan.research.minimization.plugin.settings

data class MinimizationPluginStateObservable(private val state: MinimizationPluginState) {
    var compilationStrategy = StateDelegate(
        getter = { state.state.compilationStrategy },
        setter = { state.state.compilationStrategy = it },
    )
    var temporaryProjectLocation = StateDelegate(
        getter = { state.state.temporaryProjectLocation },
        setter = { state.state.temporaryProjectLocation = it },
    )
    var snapshotStrategy = StateDelegate(
        getter = { state.state.snapshotStrategy },
        setter = { state.state.snapshotStrategy = it },
    )
    var exceptionComparingStrategy = StateDelegate(
        getter = { state.state.exceptionComparingStrategy },
        setter = { state.state.exceptionComparingStrategy = it },
    )
    var stages = StateDelegate(
        getter = { state.state.stages },
        setter = { state.state.stages = it },
    )
    var minimizationTransformations = StateDelegate(
        getter = { state.state.minimizationTransformations },
        setter = { state.state.minimizationTransformations = it },
    )
}

class StateDelegate<T>(private val getter: () -> T, private val setter: (T) -> Unit) {
    private val subscribers = mutableListOf<ChangeDelegate<*>>()

    fun <V> observe(transform: (T) -> V) = ChangeDelegate(transform).also { subscribers.add(it) }
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
