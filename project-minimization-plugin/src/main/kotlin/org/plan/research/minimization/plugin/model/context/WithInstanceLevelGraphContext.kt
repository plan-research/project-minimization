package org.plan.research.minimization.plugin.model.context

import org.plan.research.minimization.plugin.psi.graph.CondensedInstanceLevelGraph

interface WithInstanceLevelGraphContext<T : WithInstanceLevelGraphContext<T>> : IJDDContext {
    val graph: CondensedInstanceLevelGraph

    fun copy(graph: CondensedInstanceLevelGraph): T
}
