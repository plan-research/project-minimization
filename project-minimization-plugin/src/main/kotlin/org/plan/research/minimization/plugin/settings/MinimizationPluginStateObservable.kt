package org.plan.research.minimization.plugin.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.findOrCreateDirectory
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.div

class MinimizationPluginStateObservable(private val project: Project) {
    val state: MinimizationPluginState = MinimizationPluginState()
    var compilationStrategy = StateDelegate(
        getter = { state.compilationStrategy },
        setter = { state.compilationStrategy = it },
    )
    var gradleTask = StateDelegate(
        getter = { state.gradleTask },
        setter = { state.gradleTask = it },
    )
    var gradleOptions = StateDelegate(
        getter = { state.gradleOptions },
        setter = { state.gradleOptions = it },
    )
    var temporaryProjectLocation = StateDelegate(
        getter = {
            state.temporaryProjectLocation ?: run {
                val newPath = createNewTempPath()
                state.temporaryProjectLocation = newPath
                newPath
            }
        },
        setter = { state.temporaryProjectLocation = it },
    )
    var logsLocation = StateDelegate(
        getter = { state.logsLocation },
        setter = { state.logsLocation = it },
    )
    var snapshotStrategy = StateDelegate(
        getter = { state.snapshotStrategy },
        setter = { state.snapshotStrategy = it },
    )
    var exceptionComparingStrategy = StateDelegate(
        getter = { state.exceptionComparingStrategy },
        setter = { state.exceptionComparingStrategy = it },
    )
    var stages = StateDelegate(
        getter = { state.stages },
        setter = { state.stages = it },
    )
    var minimizationTransformations = StateDelegate(
        getter = { state.minimizationTransformations },
        setter = { state.minimizationTransformations = it },
    )
    var ignorePaths = StateDelegate(
        getter = { state.ignorePaths },
        setter = { state.ignorePaths = it },
    )

    fun updateState(newState: MinimizationPluginState) {
        compilationStrategy.set(newState.compilationStrategy)
        gradleTask.set(newState.gradleTask)
        gradleOptions.set(newState.gradleOptions)
        temporaryProjectLocation.set(newState.temporaryProjectLocation)
        logsLocation.set(newState.logsLocation)
        snapshotStrategy.set(newState.snapshotStrategy)
        exceptionComparingStrategy.set(newState.exceptionComparingStrategy)
        stages.set(newState.stages)
        minimizationTransformations.set(newState.minimizationTransformations)
        ignorePaths.set(newState.ignorePaths)
    }

    private fun createNewTempPath(): String =
        (project.guessProjectDir()?.toNioPath() ?: Path("").absolute())
            .parent
            .div("${project.name}-minimization-temp")
            .findOrCreateDirectory()
            .absolutePathString()
}

class StateDelegate<T>(private val getter: () -> T, private val setter: (T) -> Unit) {
    private val subscribers = mutableListOf<ChangeDelegate<*>>()

    fun <V> observe(transform: (T) -> V) = ChangeDelegate(transform).also { subscribers.add(it) }

    fun set(value: T?) {
        value ?: return
        setter(value)
        subscribers.forEach { it.onValueChanged(value) }
    }

    fun get(): T = getter()

    fun mutate(transform: (T) -> T) = set(transform(getter()))

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
}
