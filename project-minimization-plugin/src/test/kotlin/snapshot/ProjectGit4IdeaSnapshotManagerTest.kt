package snapshot

import HeavyTestContext
import LightTestContext
import TestWithContext
import TestWithHeavyContext
import TestWithLightContext
import org.plan.research.minimization.plugin.model.context.IJDDContextBase
import org.plan.research.minimization.plugin.snapshot.ProjectGit4IdeaSnapshotManager

abstract class ProjectGit4IdeaSnapshotManagerTest<C : IJDDContextBase<C>> : ProjectCloningSnapshotTest<C, ProjectGit4IdeaSnapshotManager>() {
    override fun createSnapshotManager(): ProjectGit4IdeaSnapshotManager {
        return ProjectGit4IdeaSnapshotManager()
    }
}

class ProjectGit4IdeaSnapshotHeavyManagerTest :
    ProjectGit4IdeaSnapshotManagerTest<HeavyTestContext>(),
    TestWithContext<HeavyTestContext> by TestWithHeavyContext()


class ProjectGit4IdeaSnapshotLightManagerTest :
    ProjectGit4IdeaSnapshotManagerTest<LightTestContext>(),
    TestWithContext<LightTestContext> by TestWithLightContext()