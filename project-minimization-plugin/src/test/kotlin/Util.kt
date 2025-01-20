import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.core.model.MonadF
import org.plan.research.minimization.plugin.getAllParents
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.context.IJDDContextBase
import org.plan.research.minimization.plugin.model.monad.*
import org.plan.research.minimization.plugin.model.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.services.SnapshotManagerService
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.relativeTo

data class PathContent(val path: Path, val content: String?)

infix fun Path.to(content: String?) = PathContent(this, content)

fun VirtualFile.getPathContentPair(projectPath: Path): PathContent =
    toNioPath().relativeTo(projectPath) to
            this.takeIf { it.isFile }?.contentsToByteArray()?.toString(Charsets.UTF_8)

fun Project.getAllFiles(): Set<PathContent> {
    return buildSet {
        val index = ProjectRootManager.getInstance(this@getAllFiles).fileIndex
        index.iterateContent {
            add(it.getPathContentPair(guessProjectDir()!!.toNioPath()))
        }
    }
}

fun VirtualFile.getAllFiles(projectPath: Path): Set<PathContent> = buildSet {
    add(getPathContentPair(projectPath))
    if (isFile)
        return@buildSet
    children.forEach {
        addAll(it.getAllFiles(projectPath))
    }
}

fun List<VirtualFile>.getAllFiles(projectDir: VirtualFile): Set<PathContent> =
    flatMap { it.getAllFiles(projectDir.toNioPath()) }.toSet() + getAllParents(projectDir).map {
        it.getPathContentPair(projectDir.toNioPath())
    }.toSet()

private val gradleFolders = listOf("build", "gradle", ".gradle", ".kotlin", "gradlew", "gradlew.bat")

fun Set<PathContent>.filterGradleAndBuildFiles(): Set<PathContent> =
    filterNot { content ->
        content.path.invariantSeparatorsPathString.split("/").let {
            gradleFolders.any { folder -> folder in it }
        }
    }.toSet()

fun generateAllPermutations(possibleIndices: Set<Int>): Sequence<List<Int>> {
    if (possibleIndices.isEmpty()) {
        return sequenceOf(emptyList())
    }
    return sequence {
        for (element in possibleIndices) {
            val next = possibleIndices - element
            yieldAll(generateAllPermutations(next).map { listOf(element) + it })
        }
    }
}

inline fun <C : IJDDContext> C.runMonad(block: context(IJDDContextMonad<C>) () -> Unit): C {
    val monad = IJDDContextMonad(this)
    block(monad)
    return monad.context
}

inline fun <C : IJDDContextBase<C>> C.runSnapshotMonad(
    snapshotManager: SnapshotManager = originalProject.service<SnapshotManagerService>(),
    crossinline block: MonadF<SnapshotMonad<C>, Unit>,
): C = runBlocking {
    runSnapshotMonadAsync(snapshotManager, block)
}

suspend inline fun <C : IJDDContextBase<C>> C.runSnapshotMonadAsync(
    snapshotManager: SnapshotManager = originalProject.service<SnapshotManagerService>(),
    block: MonadF<SnapshotMonad<C>, Unit>,
): C {
    val monad = snapshotManager.createMonad(this)
    block(monad)
    return monad.context
}

inline fun <C : IJDDContextBase<C>> C.runMonadWithEmptyProgress(
    crossinline block: SnapshotWithProgressMonadF<C, Unit>,
): C = runSnapshotMonad { withEmptyProgress(block) }
