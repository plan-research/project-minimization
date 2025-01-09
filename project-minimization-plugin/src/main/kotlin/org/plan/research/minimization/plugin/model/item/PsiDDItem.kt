package org.plan.research.minimization.plugin.model.item

import org.plan.research.minimization.plugin.model.item.index.PsiChildrenPathIndex
import java.nio.file.Path

sealed interface PsiDDItem<T : PsiChildrenPathIndex> : IJDDItem {
    val localPath: Path
    val childrenPath: List<T>
}