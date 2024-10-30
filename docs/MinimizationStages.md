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
   using [MinimizationPsiManager][psi-manager]. Under the hood it iterates over all kotlin source files and build a
   `PsiWithBodyDDItem`. Psi item is represented by a relative path from projects' root and the path from file's PSI
   element to it.
2. On the second stage, the DD algorithm is run using SameExceptionPropertyTester (described
   in [Compilation and Exceptions][compilation]) with custom lens: on this stage we
   use [FunctionModificationLens][function-lens] to focus on the selected functions
3. The lens is using [PsiItemStorage][psi-item-storage] and [PsiTrie][psi-trie] to quickly find all related PSI
   elements.
4. After it, they are modified using [MinimizationPsiManager][psi-manager].

[compilation]: CompilationAndExceptions.md

[function-lens]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/psi/FunctionModificationLens.kt

[psi-manager]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/psi/MinimizationPsiManager.kt

[psi-item-storage]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/psi/PsiItemStorage.kt

[psi-trie]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/psi/PsiTrie.kt