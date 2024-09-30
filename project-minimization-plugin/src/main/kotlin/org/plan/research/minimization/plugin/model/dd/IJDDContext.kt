package org.plan.research.minimization.plugin.model.dd

import org.plan.research.minimization.core.model.DDContext
import org.plan.research.minimization.plugin.model.snapshot.Snapshot

data class IJDDContext(val snapshot: Snapshot) : DDContext
