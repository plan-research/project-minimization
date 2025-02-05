package snapshot

import HeavyTestContext
import LightTestContext
import TestWithContext
import TestWithHeavyContext
import TestWithLightContext
import org.plan.research.minimization.plugin.model.context.IJDDContextBase
import org.plan.research.minimization.plugin.snapshot.ProjectLocalHistorySnapshotManager

abstract class ProjectLocalHistorySnapshotManagerTest<C : IJDDContextBase<C>> : ProjectCloningSnapshotTest<C, ProjectLocalHistorySnapshotManager>() {
    override fun createSnapshotManager(): ProjectLocalHistorySnapshotManager {
        return ProjectLocalHistorySnapshotManager()
    }
}

class ProjectLocalHistorySnapshotHeavyManagerTest :
    ProjectLocalHistorySnapshotManagerTest<HeavyTestContext>(),
    TestWithContext<HeavyTestContext> by TestWithHeavyContext()


class ProjectLocalHistorySnapshotLightManagerTest :
    ProjectLocalHistorySnapshotManagerTest<LightTestContext>(),
    TestWithContext<LightTestContext> by TestWithLightContext()