package snapshot

import HeavyTestContext
import LightTestContext
import TestWithContext
import TestWithHeavyContext
import TestWithLightContext
import org.plan.research.minimization.plugin.context.IJDDContextBase
import org.plan.research.minimization.plugin.context.snapshot.impl.ProjectCloningSnapshotManager

abstract class ProjectCloningSnapshotManagerTest<C : IJDDContextBase<C>> : ProjectCloningSnapshotTest<C, ProjectCloningSnapshotManager>() {
    override fun createSnapshotManager(): ProjectCloningSnapshotManager {
        return ProjectCloningSnapshotManager(myFixture.project)
    }
}

class ProjectCloningSnapshotHeavyManagerTest :
    ProjectCloningSnapshotManagerTest<HeavyTestContext>(),
    TestWithContext<HeavyTestContext> by TestWithHeavyContext()


class ProjectCloningSnapshotLightManagerTest :
    ProjectCloningSnapshotManagerTest<LightTestContext>(),
    TestWithContext<LightTestContext> by TestWithLightContext()
