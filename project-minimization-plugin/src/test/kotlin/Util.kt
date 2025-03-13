import arrow.core.compareTo
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.core.model.withEmptyProgress
import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.context.IJDDContextBase
import org.plan.research.minimization.plugin.context.IJDDContextMonad
import org.plan.research.minimization.plugin.context.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.context.snapshot.SnapshotMonad
import org.plan.research.minimization.plugin.modification.item.PsiDDItem
import org.plan.research.minimization.plugin.modification.item.index.PsiChildrenPathIndex
import org.plan.research.minimization.plugin.modification.psi.PsiUtils
import org.plan.research.minimization.plugin.services.SnapshotManagerService
import org.plan.research.minimization.plugin.util.SnapshotWithProgressMonadFAsync
import java.nio.file.Path
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

fun List<VirtualFile>.getAllParents(root: VirtualFile): List<VirtualFile> = buildSet {
    @Suppress("AVOID_NESTED_FUNCTIONS")
    fun traverseParents(vertex: VirtualFile?) {
        if (vertex == null || contains(vertex) || VfsUtil.isAncestor(vertex, root, false)) {
            return
        }
        add(vertex)
        traverseParents(vertex.parent)
    }
    this@getAllParents.forEach(::traverseParents)
}.toList()

fun VirtualFile.getAllNestedElements(): List<VirtualFile> = buildList {
    VfsUtilCore.iterateChildrenRecursively(
        this@getAllNestedElements,
        null,
    ) {
        add(it)
        true
    }
}

fun List<VirtualFile>.getAllFiles(projectDir: VirtualFile): Set<PathContent> =
    flatMap { it.getAllFiles(projectDir.toNioPath()) }.toSet() + getAllParents(projectDir).map {
        it.getPathContentPair(projectDir.toNioPath())
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

fun <C : IJDDContextBase<C>> C.runSnapshotMonad(
    snapshotManager: SnapshotManager = originalProject.service<SnapshotManagerService>(),
    block: suspend context(SnapshotMonad<C>) () -> Unit,
): C = runBlocking {
    runSnapshotMonadAsync(snapshotManager, block)
}

suspend fun <C : IJDDContextBase<C>> C.runSnapshotMonadAsync(
    snapshotManager: SnapshotManager = originalProject.service<SnapshotManagerService>(),
    block: suspend context(SnapshotMonad<C>) () -> Unit,
): C {
    val monad = snapshotManager.createMonad(this)
    block(monad)
    return monad.context
}

fun <C : IJDDContextBase<C>> C.runMonadWithEmptyProgress(
    block: SnapshotWithProgressMonadFAsync<C, Unit>,
): C = runSnapshotMonad { withEmptyProgress(block) }


@RequiresReadLock
fun <ITEM, T> List<ITEM>.getPsi(
    context: IJDDContext
) where T : Comparable<T>, T : PsiChildrenPathIndex, ITEM : PsiDDItem<T> =
    sortedWith { a, b -> a.childrenPath.compareTo(b.childrenPath) }
        .map { PsiUtils.getPsiElementFromItem(context, it) }

@RequiresReadLock
fun <ITEM, T> List<ITEM>.findByPsi(
    context: IJDDContext,
    filter: (PsiElement) -> Boolean
) where T : Comparable<T>, T : PsiChildrenPathIndex, ITEM : PsiDDItem<T> =
    find { filter(PsiUtils.getPsiElementFromItem(context, it)!!) }

@RequiresReadLock
fun <ITEM, T> List<ITEM>.findLastByPsi(
    context: IJDDContext,
    filter: (PsiElement) -> Boolean
) where T : Comparable<T>, T : PsiChildrenPathIndex, ITEM : PsiDDItem<T> =
    findLast { filter(PsiUtils.getPsiElementFromItem(context, it)!!) }

@RequiresReadLock
fun <ITEM, T> List<ITEM>.filterByPsi(
    context: IJDDContext,
    filter: (PsiElement) -> Boolean
) where T : Comparable<T>, T : PsiChildrenPathIndex, ITEM : PsiDDItem<T> =
    filter { filter(PsiUtils.getPsiElementFromItem(context, it)!!) }
