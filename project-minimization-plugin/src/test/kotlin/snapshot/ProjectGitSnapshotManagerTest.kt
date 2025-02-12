package snapshot

import HeavyTestContext
import LightTestContext
import TestWithContext
import TestWithHeavyContext
import TestWithLightContext
import org.plan.research.minimization.plugin.model.context.IJDDContextBase
import org.plan.research.minimization.plugin.snapshot.ProjectGitSnapshotManager

abstract class ProjectGitSnapshotManagerTest<C : IJDDContextBase<C>> : ProjectCloningSnapshotTest<C, ProjectGitSnapshotManager>() {
    override fun createSnapshotManager(): ProjectGitSnapshotManager {
        return ProjectGitSnapshotManager()
    }
}

class ProjectGitSnapshotHeavyManagerTest :
    ProjectGitSnapshotManagerTest<HeavyTestContext>(),
    TestWithContext<HeavyTestContext> by TestWithHeavyContext()


class ProjectGitSnapshotLightManagerTest :
    ProjectGitSnapshotManagerTest<LightTestContext>(),
    TestWithContext<LightTestContext> by TestWithLightContext()