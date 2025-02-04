package org.plan.research.minimization.plugin.context

import org.plan.research.minimization.plugin.modification.psi.CallTraceParameterCache

interface WithCallTraceParameterCacheContext<T : WithCallTraceParameterCacheContext<T>> : IJDDContext {
    val callTraceParameterCache: CallTraceParameterCache

    fun copy(callTraceParameterCache: CallTraceParameterCache): T
}
