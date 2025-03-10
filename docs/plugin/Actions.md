# Actions

This document provides an overview of the available plugin actions for the project-minimization-plugin.

Note: Only the "MinimizeProjectAction" is intended for end-users. The other actions are for internal/developer usage.

Below is a summary of each action:

## MinimizeProjectAction
This action initiates the minimization process on the project. It's the main entry point of the plugin.

- **Plugin Registration**:
  - ID: org.plan.research.minimization.plugin.actions.MinimizeProjectAction
  - UI Text: "Minimize Project"
  - Registered in the Tools menu at the first position

- **Source:** [MinimizeProjectAction]

## BenchmarkAction
This action executes minimization on the provided dataset (see [Dataset]).

- **Plugin Registration**:
  - ID: org.plan.research.minimization.plugin.actions.BenchmarkAction
  - UI Text: "Run Minimization Plugin Benchmarks"
  - Registered in the Tools menu, positioned after the MinimizeProjectAction

- **Source:** [BenchmarkAction]

## DumpDeletablePsiGraphAction
This action generates a dump of the deletable PSI (Program Structure Interface) graph, which can be used to understand dependencies and potential safe code removals.

- **Plugin Registration**:
  - ID: org.plan.research.minimization.plugin.actions.DumpDeletablePsiGraphAction
  - UI Text: "Dump Instance-Level Graph"
  - Description: Dumps instance-level graph
  - Internal: true

- **Source:** [DumpDeletablePsiGraphAction]

[MinimizeProjectAction]: ../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/actions/MinimizeProjectAction.kt
[BenchmarkAction]: ../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/actions/BenchmarkAction.kt
[DumpDeletablePsiGraphAction]: ../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/actions/DumpDeletablePsiGraphAction.kt
[Dataset]: ../Dataset.md


