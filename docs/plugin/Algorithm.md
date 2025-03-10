# Algorithm

The Minimization Algorithm is designed to reduce a project to its minimal viable form for analysis, debugging, and testing. It is implemented primarily within the [MinimizationService][service], combining a number of stages and context transformations into a streamlined workflow.

## Overview

This algorithm is responsible for:
- Cloning the original project to preserve its state.
- Removing extraneous content such as KDocs and unused imports.
- Sequentially applying a series of customizable minimization stages (see [Stages](MinimizationStages.md)).
- Transforming and updating the project context between lightweight and heavy representations.
- Performing post-processing operations like reindexing and cleaning up project dependencies.

## Content of the Algorithm Folder

The algorithm folder is organized to support various aspects of the minimization process. Its structure is as follows:

1. **tester/**
   - Contains property testing classes. For example, `PropertyTesterFactory.kt` provides factory methods for generating property testers, `SameExceptionPropertyTester.kt` defines testers to verify exception behavior, and `PropertyCheckingListener.kt` listens for property checking events. These files help validate the algorithm's expected behavior.

2. **stages/**
   - Houses implementations of different minimization stages. These include:
     - `FileLevelStage.kt`: Implements file-level minimization transformations.
     - `FunctionLevelStage.kt`: Implements transformations at the function level.
     - `MinimizationStage.kt`: Defines the common interface for all stages.
     - `MinimizationStageBase.kt`: Provides a base implementation for stages to reduce repeated code.
     - `DeclarationGraphLevelStage.kt`: Focuses on transformations using a declaration graph approach.

3. **file/**
   - Contains utilities for handling file hierarchies during minimization. For instance, `FileTreeHierarchicalDDGenerator.kt` generates hierarchical representations of the file tree based on dependency data, and `FileTreeHierarchyFactory.kt` helps construct these hierarchies. These tools are essential for analyzing and minimizing the file structure.

4. **adapters/**
   - Provides adapter classes that interface with external systems, particularly the IntelliJ platform. Examples include:
     - `IJPropertyTester.kt` and `IJGraphPropertyTester.kt`: Enable property testing within the IntelliJ environment.
     - `IJGraphPropertyTesterAdapter.kt`: Acts as a bridge to adapt property testing for graph structures.
     - `IJHierarchicalDDGenerator.kt`: Integrates hierarchical generation utilities with IntelliJ.
   - These adapters ensure that the algorithm can interact seamlessly with IDE-specific features for enhanced analysis and testing.

5. **MinimizationError.kt**
   - Defines error types and handling mechanisms specific to the minimization process, ensuring robust error management throughout the algorithm.

Together, these components make the minimization algorithm modular, testable, and well-integrated with development environments, enabling efficient reduction and analysis of projects.

[project-cloning]: ../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/services/ProjectCloningService.kt
[kdoc-remover]: ../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/modification/psi/KDocRemover.kt
[import-cleaner]: ../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/modification/psi/PsiImportCleaner.kt
[heavy-transformer]: ../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/services/MinimizationService.kt#heavytransformer
[service]: ../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/services/MinimizationService.kt
