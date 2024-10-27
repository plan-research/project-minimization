# Minimization Stages

TODO

## File-level stage

TODO

## Function-level stage

Currently, the function stage is implemented as a DD algorithm that tries to minimize the number of the top-level
elements with body.
If the element (function, lambda, class initializer, custom getter/setters)
in chosen to be minimized the body of that element is replaced with `TODO("Replaced by DD")`.

The stage is works in several stages:

1. On the first stage the top-level (out of these elements) elements that contain body are collected
   using [PsiWithBodiesCollectorService][psi-collector].
   Under the hood it iterates over all project files and parses it using [BodyElementAcquiringKtVisitor][body-getter].
2. On the second stage, the DD algorithm is run using SameExceptionPropertyTester (described
   in [Compilation and Exceptions][compilation]) with custom lens: on this stage we
   use [FunctionModificationLens][function-lens] to focus on the selected functions
3. The lens is using [ModifyingBodyKtVisitor][modifier-visitor] to modify the bodies according to the selected elements.
4. The visitor uses [PsiModificationManager][psi-manager] to actually run the modification operations.

[psi-collector]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/services/PsiWithBodiesCollectorService.kt
[body-getter]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/psi/BodyElementAcquiringKtVisitor.kt
[compilation]: CompilationAndExceptions.md
[function-lens]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/psi/FunctionModificationLens.kt
[modifier-visitor]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/psi/ModifyingBodyKtVisitor.kt
[psi-manager]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/psi/PsiModificationManager.kt