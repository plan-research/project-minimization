package org.plan.research.minimization.plugin.model.context

import org.plan.research.minimization.plugin.psi.CallTraceParameterCache

interface WithCallTraceParameterCacheContext<T : WithCallTraceParameterCacheContext<T>> : IJDDContext {
    val callTraceParameterCache: CallTraceParameterCache

    fun copy(importRefCounter: CallTraceParameterCache): T
}
