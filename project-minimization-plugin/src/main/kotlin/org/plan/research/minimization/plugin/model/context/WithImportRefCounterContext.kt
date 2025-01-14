package org.plan.research.minimization.plugin.model.context

import org.plan.research.minimization.plugin.psi.KtSourceImportRefCounter

interface WithImportRefCounterContext<T: WithImportRefCounterContext<T>> {
    val importRefCounter: KtSourceImportRefCounter

    fun copy(importRefCounter: KtSourceImportRefCounter): T
}
