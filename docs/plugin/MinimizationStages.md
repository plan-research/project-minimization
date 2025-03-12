# Minimization Stages

This document describes the different minimization stages implemented by the plugin. Each stage is responsible for
reducing the project size by applying delta-debugging (DD) algorithms along with specialized lenses and PSI services
provided by the [MinimizationService][service].

## File-Level Stage

The File-Level stage focuses on minimizing the project at the granularity of individual source files. Key steps include:

- Initializing a file hierarchy using the FileTreeHierarchyFactory, which organizes source files into a structured tree
  based on their paths.
- Running a hierarchical delta debugging
  algorithm ([HierarchicalDD](../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/algorithm/stages/FileLevelStage.kt))
  on the constructed file hierarchy. This iterative process removes files while ensuring the minimized project still
  meets the required property.
- Logging progress and handling any errors via the MinimizationError mechanism.

## Function-Level Stage

The Function-Level stage targets code elements that contain executable bodies, such as functions, lambdas, class
initializers, and custom getter/setters.
For Java, replacing method and lambda bodies is supported.
Key aspects are:

- Collecting all top-level PSI elements with bodies using
  the [MinimizationPsiManagerService](../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/services/MinimizationPsiManagerService.kt)
  to construct a set of items (PsiWithBodyDDItem).
- Using a FunctionModificationLens to focus on the parts of these elements that can be safely minimized.
- Applying the DD algorithm (DDAlgorithm) with a property tester (commonly based on SameExceptionPropertyTester) to
  validate that the minimized version retains essential properties like compilation and exception behavior.
- Replacing the minimized function body with a placeholder (e.g., `TODO("Replaced by DD")` or
  `throw new UnsupportedOperationException("Removed by DD")`) to signify that the body has
  been reduced.
- The process is integrated into the overall minimization flow provided by the [MinimizationService][service].

## Declaration-Graph-Level Stage

The Declaration-Graph-Level stage operates at the level of code declarations and their dependencies by leveraging a
graph-based approach:

- Building a deletable PSI graph that represents code declarations and their interdependencies through
  the [MinimizationPsiManagerService](../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/services/MinimizationPsiManagerService.kt).
- Employing a graph delta debugging
  algorithm ([GraphDD](../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/algorithm/stages/DeclarationGraphLevelStage.kt))
  enhanced with condensation to simplify the graph structure.
- Creating a property tester using a FunctionDeletingLens to ensure that deletion of certain declarations does not
  result in errors.
- Integrating supporting mechanisms such as a KtSourceImportRefCounter for automatically removing unused import
  statements, and a call trace parameter cache to correctly manage function call dependencies.

## Common Execution Pattern

All stages follow a common framework as defined in the MinimizationStageBase:

- They create a specialized context (e.g., FileLevelStageContext, FunctionLevelStageContext,
  DeclarationLevelStageContext) derived from the overall project context.
- Execution runs within a snapshot monad that preserves state and manages progress logging.
- Each stage logs its start and end, and any errors are reported transparently via the logging mechanism, ensuring
  reliable tracking of the minimization process.

[service]: ../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/services/MinimizationService.kt
