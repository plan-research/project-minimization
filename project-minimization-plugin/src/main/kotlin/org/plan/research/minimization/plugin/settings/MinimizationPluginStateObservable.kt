package org.plan.research.minimization.plugin.settings

data class MinimizationPluginStateObservable(private val stateGetter: () -> MinimizationPluginState) {
    var compilationStrategy = StateDelegate(
        getter = { stateGetter().compilationStrategy },
        setter = { stateGetter().compilationStrategy = it },
    )
    var gradleTask = StateDelegate(
        getter = { stateGetter().gradleTask },
        setter = { stateGetter().gradleTask = it },
    )
    var gradleOptions = StateDelegate(
        getter = { stateGetter().gradleOptions },
        setter = { stateGetter().gradleOptions = it },
    )
    var temporaryProjectLocation = StateDelegate(
        getter = { stateGetter().temporaryProjectLocation },
        setter = { stateGetter().temporaryProjectLocation = it },
    )
    var snapshotStrategy = StateDelegate(
        getter = { stateGetter().snapshotStrategy },
        setter = { stateGetter().snapshotStrategy = it },
    )
    var exceptionComparingStrategy = StateDelegate(
        getter = { stateGetter().exceptionComparingStrategy },
        setter = { stateGetter().exceptionComparingStrategy = it },
    )
    var stages = StateDelegate(
        getter = { stateGetter().stages },
        setter = { stateGetter().stages = it },
    )
    var minimizationTransformations = StateDelegate(
        getter = { stateGetter().minimizationTransformations },
        setter = { stateGetter().minimizationTransformations = it },
    )

    var ignorePaths = StateDelegate(
        getter = { stateGetter().ignorePaths },
        setter = { stateGetter().ignorePaths = it },
    )
}

class StateDelegate<T>(private val getter: () -> T, private val setter: (T) -> Unit) {
    private val subscribers = mutableListOf<ChangeDelegate<*>>()

    fun <V> observe(transform: (T) -> V) = ChangeDelegate(transform).also { subscribers.add(it) }
    fun mutable() = MutableChangeDelegate()

    inner class ChangeDelegate<V>(private val transform: (T) -> V) {
        private var value: V = transform(getter())
        private var rawValue: T = getter()

        operator fun getValue(thisRef: Any?, property: Any?): V {
            if (rawValue != getter()) {
                onValueChanged(getter())
            }

            return value
        }

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
