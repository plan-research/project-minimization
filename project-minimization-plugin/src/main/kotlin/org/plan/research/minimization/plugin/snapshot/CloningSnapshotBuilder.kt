package org.plan.research.minimization.plugin.snapshot

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.plan.research.minimization.plugin.errors.SnapshotBuildingError
import org.plan.research.minimization.plugin.model.snapshot.SnapshotBuilder
import org.plan.research.minimization.plugin.model.snapshot.SnapshotDecision
import org.plan.research.minimization.plugin.services.ProjectCloningService

class CloningSnapshotBuilder(private val rootProject: Project) : SnapshotBuilder<CloneSnapshot> {
    private val projectCloningService = rootProject.service<ProjectCloningService>()
    override suspend fun makeTransaction(
        currentSnapshot: CloneSnapshot,
        modifier: suspend (Project) -> SnapshotDecision
    ): Either<SnapshotBuildingError, CloneSnapshot> = either {
        val newProject = projectCloningService.clone(currentSnapshot.project)
        ensureNotNull(newProject) { SnapshotBuildingError.CopyingFailed }
        val newSnapshot = CloneSnapshot(newProject, currentSnapshot)

        when (modifier(newProject)) {
            SnapshotDecision.Commit -> {
                currentSnapshot.rollback().bind()
                newSnapshot
            }

            SnapshotDecision.Rollback -> {
                newSnapshot.rollback().bind()
                raise(SnapshotBuildingError.Aborted)
            }
        }
    }

    override fun initialSnapshot() = CloneSnapshot(rootProject, null).right()
}