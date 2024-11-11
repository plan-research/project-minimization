package org.plan.research.minimization.core.model

interface SlicingGraphNode {
    suspend fun getOutwardEdges(): Collection<SlicingGraphNode>
}