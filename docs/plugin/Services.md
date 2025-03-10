# Minimization Plugin Services

This document provides an overview of the various services provided by the minimization plugin located in the services folder.

## TestGraphService

- **Purpose:** Generates a visual representation of the PSI (Program Structure Interface) graph for debugging purposes.
- **Key Actions:**
  - Leverages the MinimizationPsiManagerService to build a graph of deletable PSI elements.
  - Dumps the graph to an SVG file (instance-level-adapters.svg) in the project root.
  - Opens the generated SVG file in the IDE for visual inspection.
- **Usage:** Useful for verifying and visualizing the structure of PSI elements during minimization.

## ProjectCloningService

- **Purpose:** Clones the project directory to create a duplicate environment for minimization operations.
- **Key Actions:**
  - Supports two cloning modes: light cloning (copying the project directory) and heavy cloning (opening the cloned project).
  - Utilizes temporary directories and logs locations as specified in the plugin settings.
  - Filters out unwanted directories (e.g., the project's directory store folder) during the clone process.
- **Usage:** Essential for creating isolated copies of the project to test minimization without affecting the original.

## RootsManagerService

- **Purpose:** Manages and determines the important file system roots (content, source, and ignored directories) within a project.
- **Key Actions:**
  - Propagates and merges various roots based on the project's structure.
  - Filters out ignored paths and adjusts source roots appropriately.
  - Provides a list of relative paths representing valid roots for indexing.
- **Usage:** Prepares the project structure for indexing and minimization by determining which directories should be included.

## SnapshotManagerService

- **Purpose:** Manages snapshots of the project's state during the minimization process.
- **Key Actions:**
  - Delegates snapshot creation to an underlying strategy defined in the plugin settings.
  - Provides a monad-based approach for handling snapshots, enabling rollback and state analysis.
- **Usage:** Facilitates state management and rollback capabilities during project minimization.

## BuildExceptionProviderService

- **Purpose:** Checks project compilation and handles build exceptions by applying error transformations.
- **Key Actions:**
  - Uses an underlying build exception provider, combined with a customizable compilation strategy.
  - Applies a series of exception transformers from the plugin settings to standardize error reporting.
- **Usage:** Aids in diagnosing and resolving compilation errors during the minimization process.

## MinimizationPsiManagerService

- **Purpose:** Provides functionalities to analyze and manipulate PSI elements within the project.
- **Key Actions:**
  - Identifies deletable PSI elements and constructs a corresponding graph of PSI modifications.
  - Retrieves PSI elements that can be modified or removed, including those with bodies or stubs.
  - Plays a central role in the core minimization logic by interfacing directly with the project's code structure.
- **Usage:** Core service for analyzing, transforming, and minimizing PSI elements in the project.

## ProjectOpeningService

- **Purpose:** Facilitates the opening of projects and the creation of project contexts.
- **Key Actions:**
  - Integrates with the IDE's mechanisms to open a project.
  - Is utilized by the ProjectCloningService during heavy cloning to open a cloned project.
- **Usage:** Enables dynamic project management and ensures cloned projects are properly opened for further operations.

## BenchmarkService

- **Purpose:** Conducts and records performance benchmarks related to minimization operations.
- **Key Actions:**
  - Provides utilities to measure execution times and performance metrics of various minimization tasks.
- **Usage:** Useful for performance analysis and optimizing the minimization process.

## MinimizationPluginSettings

- **Purpose:** Manages the configuration and reactive state for the minimization plugin.
- **Key Actions:**
  - Stores settings such as temporary project locations, compilation strategies, snapshot strategies, and paths to ignore.
  - Provides observables that allow other services to react to configuration changes.
- **Usage:** Acts as the central configuration hub influencing the behavior of all other services.

---

This documentation is intended to serve as an overview of the responsibilities and interactions of the various services within the minimization plugin.


