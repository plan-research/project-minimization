# Project Minimization

The project minimization tool is an IntelliJ IDEA plugin that aims to reduce the size of projects localizing Kotlin
compiler faults.

## How to build

You need to run the following command to build the plugin:

```bash
./gradlew buildPlugin
```

The plugin artefact will be placed in
`project-minimization-plugin/build/distributions/project-minimization-plugin-*.zip`.

## How to run

There are two ways to run the plugin:

- Get from the releases or build the plugin as a `zip` file and install it into your IDE
  via [a standard pipeline](https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk).
- Run the following command, which executes a new instance of IntelliJ IDEA with the preinstalled plugin:

```bash
./gradlew runIde
```

As soon as an IDE with the installed plugin is executed, you can perform an action `Minimize Project` on an opened
project.
The action can be found in the `Tools` menu bar or via search.

It is expected that during minimization, several new projects can be opened and closed.
It's necessary to perform safe actions without affecting the original project. At the end of the minimization process,
only one extra window with the minimized project should be opened.

## Limitations

Now, minimization supports only gradle projects.

It also minimizes well only kotlin files, java source files have limited support, and other files are minimized without
complex modifications.

As a consequence, this tool doesn't minimize build files and files related to project building, e.g. `buildSrc`.

## Tips

You can use settings to adjust minimization, speeding up the process.

Here are some tips that can significantly help and improve minimization:

### Use a simple build task

As faster your Gradle task works, as faster minimization will be performed.

Consider that in some cases, during minimization, the program can become executable.
So if your task is supposed to execute the program (for example,
tests), it could execute them increasing minimization time.

That is why try to choose a task that will execute only compilation, e.g. `compileKotlin`,
and try to avoid tasks like `build` and `test`.

### Exclude build files

Generally, you can provide paths to exclude them from minimization.
For example, in case when you are sure that these files cannot be minimized well or cannot be minimized at all.

This will significantly speed up your minimization.
As a rule, build files such as a `buildSrc` folder cannot be minimized, so it will be good to exclude them.

## Settings

- **Gradle task**: Specifies the Gradle task to reproduce the fault.

- **Gradle options**: Specifies the additional options to the specified Gradle task.
  Options `--no-configuration-cache`, `--no-build-cache` and `--quiet` are always passed.

- **Temporary project location**: Specifies the folder's name for storing project snapshots.
  The resulting project will be placed into this directory.

- **Logs location**: Specifies the folder's name where logs should be placed.

- **Snapshot strategy**: Determines the snapshot strategy, namely how the tool makes temporary modifications.

- **Exception comparing strategy**: Configures the exception comparison strategy.
  It's recommended to use the `STACKTRACE` comparison.

- **Stages**: Defines a list of minimization stages. It's recommended to use default setting.

- **Ignore paths**: List of directories/files excluded from minimization.

## Possible issues

### Incorrect result

The resulting project can be incorrect in terms of reproducing the initial compiler problem,
namely, the initial exception can be lost.

There are several possible reasons:

1. Exceptions were too similar in terms of *Exception comparing strategy*. Possible solution: change the corresponding
   setting.
2. The initial exception isn't reproduced at the beginning of minimization. Possible solution: clean the build cache and
   restart your IDE.
3. The exception is cached, so modifications didn't affect the build task. Possible solution: remake the build task to
   clean all caches after the end of its execution.

### Minimization cannot be executed or ended without results

The main reason for this error is an unexpected exception during minimization.

One of the most common errors is that the initial compiler issue isn't reproduced at the beginning of minimization.
Possible solution: clean the build cache and restart your IDE.

### Minimization took longer than expected

One of the possible reasons for this is that the result of minimization is quite big,
and the project cannot be minimized well.

In such cases you can try to exclude some parts of the project from minimization that cannot be minimized well.
Tip: use the *Ignore paths* setting.

## Documentation

The project documentation, including detailed descriptions of the algorithms and general pipeline, can be
found [here](docs/OverallArchitecture.md).
