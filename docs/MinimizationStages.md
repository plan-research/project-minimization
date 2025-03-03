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
   using [MinimizationPsiManagerService][psi-manager] or [PsiUtils][psi-utils].
   Under the hood it iterates over all kotlin source files and builds a
   `PsiWithBodyDDItem`.
   Psi item is represented by a relative path from projects' root and the path from file's PSI
   element to it.
2. On the second stage, the DD algorithm is run using SameExceptionPropertyTester (described
   in [Compilation and Exceptions][compilation]) with custom lens: on this stage we
   use [FunctionModificationLens][function-lens] to focus on the selected functions
3. The lens is using [PsiTrie][psi-trie] to quickly find all related PSI elements.
4. After it, they are modified using [MinimizationPsiManagerService][psi-manager] and [PsiUtils][psi-utils].

## Declaration-level stage

TODO

### Import Reference Counter

Import reference counter is an optimization that allows automatically removing unused import statements after the
deletion of all referenced elements.
The reason for adding this feature was the lack of proper fast file-level unused import removing for the light context.

Because of that, the IDEA's unused import optimizer has been modified for context storing.
The code could found
in [psi.imports][imports] package.
We left all credits to the original authors from IntelliJ IDEA.

The import optimization is performed in two stages: pre-processing and processing.

On the pre-processing stage, for each import in each file, the reference counter collects the number of references
(see [KtSourceImportRefCounter]).
This information is stored in the DD context.

The processing stage is done on the focusing to the PSI element (see [FunctionDeletingLens][function-deleting-lens]).
For each deleted element, the number of references to the imports is resolved.
Then, the copy (using persistent data structures) is created with the updated reference counts.
However, if some references become null, then the corresponding imports are also removed.

### Overridden Composing

The second optimization is basically a shortcut for delta-debugging.
That optimization is focused only one problem.
Imagine the following class hierarchy:

```kotlin
interface I {
    fun method(): Int
}

class IntProducer(val produced: Int) : I {
    override fun method(): Int = produced
}
```

If on the instance-level stage we would like to delete `IntProducer::method` we will face compilation exception:
`IntProducer does not override method`.
So, to avoid it, we have two options: do not remove `method` or remove `method` from all super/subclasses.

The optimization for each method or property using Kotlin Analysis API (see [KotlinOverriddenElementsGetter]) fetches
its-based or derived methods and properties.
Using this information, the methods and properties are split into equivalence classes based on the hierarchy.
In each equivalence class a representative is chosen.
The representative element (`OverriddenPsiStubDDItem` in [IJDDItem]) is an element that has the shortest path
from the root PSI element.

Then all the elements of the equivalence class are replaced with a representative element with the information about
rest elements.
The information is retrieved on the focusing stage (see [BasePsiLens]).


[compilation]: CompilationAndExceptions.md

[function-lens]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/modification/lenses/FunctionModificationLens.kt

[psi-manager]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/services/MinimizationPsiManagerService.kt

[psi-utils]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/modification/psi/PsiUtils.kt

[psi-trie]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/modification/psi/trie/PsiTrie.kt

[function-deleting-lens]:  ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/modification/lenses/FunctionDeletingLens.kt

[imports]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/modification/psi/imports

[KotlinOverriddenElementsGetter]:  ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/modification/psi/KotlinOverriddenElementsGetter.kt

[IJDDItem]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/modification/item/IJDDItem.kt

[BasePsiLens]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/modification/lenses/BasePsiLens.kt

[KtSourceImportRefCounter]: ../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/modification/psi/KtSourceImportRefCounter.kt