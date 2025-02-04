package org.plan.research.minimization.plugin.context

import org.plan.research.minimization.plugin.modification.psi.KtSourceImportRefCounter

interface WithImportRefCounterContext<T : WithImportRefCounterContext<T>> : IJDDContext {
    val importRefCounter: KtSourceImportRefCounter

    fun copy(importRefCounter: KtSourceImportRefCounter): T
}
