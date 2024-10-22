package org.plan.research.minimization.plugin.settings

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State

@Service(Service.Level.PROJECT)
@State(name = "MinimizationPluginSettings")
class MinimizationPluginSettings : SimplePersistentStateComponent<MinimizationPluginState>(MinimizationPluginState())
