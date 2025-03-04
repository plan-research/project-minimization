# Compilation and Exceptions

The project minimization plugin is built around a robust compilation system that not only compiles code but also handles errors and exceptions in a structured manner. The compilation module is organized into a core set of functionalities along with several specialized submodules that reside as folders within it. Each submodule contributes its own unique aspect to the overall compilation and exception management process.

## Core Compilation
At the top level, the compilation module contains the following key components:
- **[CompilationPropertyCheckerError.kt](../../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/compilation/CompilationPropertyCheckerError.kt)**: Handles errors related to property checks during the compilation process.
- **[DumbCompiler.kt](../../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/compilation/DumbCompiler.kt)**: Implements a straightforward, minimal compiler based on the needs of the plugin.
- **[BuildExceptionProvider.kt](../../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/compilation/BuildExceptionProvider.kt)**: Serves as the cornerstone for capturing and managing exceptions generated during compilation.

## Submodules within the Compilation Module
Within the compilation directory, several subfolders further specialize in various aspects of error handling and reporting:

### Exception Submodule
This submodule focuses on defining and translating compiler exceptions to ensure that error messages are clear and actionable:
- **[KotlincErrorSeverity.kt](../../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/compilation/exception/KotlincErrorSeverity.kt)**: Specifies the error severity levels as reported by the Kotlinc compiler.
- **[KotlincException.kt](../../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/compilation/exception/KotlincException.kt)**: Defines the structure of exceptions thrown during Kotlin compilation.
- **[KotlincExceptionTranslator.kt](../../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/compilation/exception/KotlincExceptionTranslator.kt)**: Translates raw compiler exceptions into structured and understandable formats.
- **[CaretPosition.kt](../../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/compilation/exception/CaretPosition.kt)**: Identifies the specific location in the source code where errors occur.
- **[CompilationException.kt](../../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/compilation/exception/CompilationException.kt)**: Represents general exceptions during the compilation process.
- **[IdeaCompilationException.kt](../../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/compilation/exception/IdeaCompilationException.kt)**: Caters to exceptions specific to IDEA-based compilation scenarios.

### Comparator Submodule
This submodule provides mechanisms to compare and analyze exceptions, making it easier to identify similar error patterns:
- **[StacktraceExceptionComparator.kt](../../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/compilation/comparator/StacktraceExceptionComparator.kt)**: Compares exceptions based on their stack trace details.
- **[ExceptionComparator.kt](../../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/compilation/comparator/ExceptionComparator.kt)**: Offers a general-purpose comparison tool for exceptions.
- **[SimpleExceptionComparator.kt](../../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/compilation/comparator/SimpleExceptionComparator.kt)**: Implements a lightweight approach for exception comparison.

### Gradle Submodule
Tailored for Gradle-based builds, this submodule handles exceptions and output from the Gradle build system:
- **[GradleBuildExceptionProvider.kt](../../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/compilation/gradle/GradleBuildExceptionProvider.kt)**: Provides exceptions related to Gradle build failures.
- **[GradleConsoleRunResult.kt](../../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/compilation/gradle/GradleConsoleRunResult.kt)**: Captures and represents outputs from Gradle tasks executed via the console.
- **[StringBuildOutputInstantReader.kt](../../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/compilation/gradle/StringBuildOutputInstantReader.kt)**: Enables real-time reading of build outputs from Gradle tasks.

### Transformer Submodule
This submodule transforms raw outputs of exceptions into formats that are easy to compare:
- **[BuildExceptionProviderWithTransformers.kt](../../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/compilation/transformer/BuildExceptionProviderWithTransformers.kt)**: Combines exception provisioning with transformation logic to enrich error messaging.
- **[ExceptionTransformer.kt](../../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/compilation/transformer/ExceptionTransformer.kt)**: Adjusts and refines exception details for better comprehensibility.
- **[PathRelativizationTransformer.kt](../../../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/compilation/transformer/PathRelativizationTransformer.kt)**: Converts absolute file paths in error outputs to relative paths, making them easier to interpret.
