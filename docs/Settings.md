# Settings
Here’s a compact overview of the `MinimizationPluginState` settings fields and guidance on interacting with them in code.

### Configuration Fields

- **compilationStrategy**: Defines the compilation strategy. Default is `GRADLE_IDEA` from `CompilationStrategy`.
- **gradleTask**: Specifies the default Gradle task to execute. Default is `"build"`.
- **temporaryProjectLocation**: Sets the temporary project location for storing project snapshots. Default is `"minimization-project-snapshots"`.
- **snapshotStrategy**: Determines the snapshot strategy. Default is `PROJECT_CLONING` from `SnapshotStrategy`.
- **exceptionComparingStrategy**: Configures the exception comparison strategy. Default is `SIMPLE` from `ExceptionComparingStrategy`.
- **gradleOptions**: List of additional options for Gradle. Default is an empty list.
- **stages**: Defines a list of minimization stages. Default stages are `FunctionLevelStage` and `FileLevelStage`.
- **minimizationTransformations**: Specifies transformations to apply during minimization. Default is `[PATH_RELATIVIZATION]` from `TransformationDescriptors`.
- **ignorePaths**: List of directories/files excluded from minimization algorithm.

### Accessing and Modifying Settings

To interact with the settings in `MinimizationPluginState`, use the following methods:

- **Reading a Field**: Use the `state` property to access fields directly.
  ```kotlin
  project.service<MinimizationPluginSettings>().state.exceptionComparingStrategy
  ```

- **Creating an Observable Field**: Create an observable to watch for changes in a setting field.
  ```kotlin
  private val stages by project.service<MinimizationPluginSettings>()
      .stateObservable
      .stages
      .observe { it }
  ```

- **Modifying a Field**: Use the `mutable()` extension to update a specific field’s value.
  ```kotlin
  var gradleOptions by project.service<MinimizationPluginSettings>()
      .stateObservable
      .gradleOptions.mutable()
  gradleOptions = newState.gradleOptions
  ```

- **Modifying all State**: Update all settings at once by assigning a new `MinimizationPluginState`.
  ```kotlin
  project.service<MinimizationPluginSettings>().updateState(newState)
  ```

### Serializing and Deserializing the Settings

The settings can be saved to and loaded from an XML file using the following methods:

- **Saving State to a File**:
  ```kotlin
  fun saveStateToFile(project: Project, filePath: String)
  ```

- **Loading State from a File**:
  ```kotlin
  fun loadStateFromFile(project: Project, filePath: String)
  ```

These methods allow you to persist configuration data by serializing `MinimizationPluginState` to XML and then deserializing it back into the application.

### How to add new minimization stage in settings

1. In [MinimizationPluginState][plugin-state] add your stage to ```defaultStages```
2. In [AppSetnigsComponent][app-settings-component] 
   1. initialize new boxes by analogy at Stage list
   2. update getter and setter for varibale ```stages```
   3. add new UI objects to ```updateUIState ```
   4. add function similar to ```isFunctionStageEnabled```. It is used in ```updateUIState ``` to correctly freeze/unfreeze the settings.
   5. add function similar to ```functionStagePanelInit```. This function should construct new Panel for your stage.
   6. add this panel to ```stagesPanelInit```

We understand that a more friendly and automated interface could be written for this, but we did not do so hoping that in reality there would be no need for this.

[plugin-state]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/settings/MinimizationPluginState.kt

[app-settings-component]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/settings/AppSettingsComponent.kt