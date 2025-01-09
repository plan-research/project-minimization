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
- **ignorePaths**: List of directories/files excluded from minimization.

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

- **Modifying a Field**: Use the `set` or `modify` functions to update a specific field’s value.
  ```kotlin
  project.service<MinimizationPluginSettings>()
    .stateObservable
    .gradleOptions
    .set(newState.gradleOptions)
  ```

- **Modifying all State**: Update all settings at once by assigning a new `MinimizationPluginState`.
  ```kotlin
  project.service<MinimizationPluginSettings>()
    .stateObservable
    .updateState(newState)
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

### How to add a new minimization stage in settings

1. In [MinimizationPluginState][plugin-state] add new stage class to ```elementTypes``` parameter of the ```@get:Xcollection``` annotation.
2. _Optional_: Update default stages in [MinimizationPluginState][plugin-state]
3. In [StagesSettingsProducer][stages-settings-component]
   1. add a new function that describes the stage and provides settings for it (like `fileLevelPanel`)
   2. add minimization stage data in the `createStagesData` function

#### Important notes
1. Remember to use bindings: utilize `GraphProperty` **only** which can be created using `PropertyGraph`. Do not use `bind(getter, setter)`.
2. [StagesSettingsProducer][stages-settings-component] provides the `stageProperty` extension function for `PropertyGraph`
   to easily create properties for specific stage's fields.
   The function uses [Lens](https://arrow-kt.io/learn/immutable-data/lens/), so it's strongly recommended to use `@optics` for the stage class.
   Here is a guide on how to do this correctly: [link](https://arrow-kt.io/learn/immutable-data/intro/#meet-optics).


[plugin-state]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/settings/MinimizationPluginState.kt

[stages-settings-component]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/settings/ui/StagesSettingsProducer.kt
