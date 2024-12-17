# Snapshot Managers

Snapshot Managers are responsible for managing the creation and lifecycle of project snapshots during transactions.

All classes implementing this functionality are located in:`project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/snapshot`.

## [ProjectCloningSnapshotManager](../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/snapshot/ProjectCloningSnapshotManager.kt)

`transaction` method executes a transaction within the provided context, typically involving project cloning and rollback upon failures.

Before a transaction begins, the manager copies the current minimization stage—either the initial project or a partially minimized project from the previous stage—to the snapshot directory.

- **On successful completion of the transaction**, the clone is retained and serves as the latest minimization stage for subsequent processes.
- **On failure**, the clone is discarded, and the minimization process continues with the previous instance.

The copying of the project is managed by
[ProjectCloningService.kt](../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/services/ProjectCloningService.kt)

### Transaction guarantees that:
- Cloned project is closed if a transaction fails.
- If a transaction is successful, the project of the [context] is closed.

## [ProjectGitSnapshotManager](../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/snapshot/ProjectGitSnapshotManager.kt)

`transaction` executes a transaction within the provided context. 

Snapshots are managed with Git operations.

- **On successful transaction**, `git commit` will be executed.
- **On failure**, `git reset --HARD` will be executed.

Git operations within the manager are executed by 
[GitWrapperService.kt](../project-minimization-plugin/src/main/kotlin/org/plan/research/minimization/plugin/services/GitWrapperService.kt)