# Project Minimization

The project minimization tool is an IntelliJ IDEA plugin that aims to reduce the size of projects localizing Kotlin compiler faults.

## How to build

You need to run the following command to build the plugin:
```bash
./gradlew buildPlugin
```

The plugin artefact will be placed in `project-minimization-plugin/build/distributions/project-minimization-plugin-*.zip`.

## How to run

There are two ways to run the plugin:
- Build the plugin as a `zip` file and install it into your IDE via [a standard pipeline](https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk).
- Run the following command, which executes a new instance of IntelliJ IDEA with the preinstalled plugin:
```bash
./gradlew runIde
```

As soon as an IDE with the installed plugin is executed, you can perform an action `Minimize Project` on an opened project. 
The action can be found in the `Code` menu bar or via search.

It is expected that during minimization, several new projects can be opened and closed. 
It's necessary to perform safe actions without affecting the original project. At the end of the minimization process, only one extra window with the minimized project should be opened.

## Settings

**TBD**

## Documentation

The project documentation, including detailed descriptions of the algorithms and general pipeline, can be found [here](docs/OverallArchitecture.md).
