import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.plan.research.minimization.plugin.getAllParents
import java.nio.file.Path
import kotlin.io.path.relativeTo

data class PathContent(val path: Path, val content: String?)

infix fun Path.to(content: String?) = PathContent(this, content)

fun VirtualFile.getPathContentPair(project: Project): PathContent =
    toNioPath().relativeTo(project.guessProjectDir()!!.toNioPath()) to
            this.takeIf { it.isFile }?.contentsToByteArray()?.toString(Charsets.UTF_8)

fun Project.getAllFiles(): Set<PathContent> {
    return buildSet {
        val index = ProjectRootManager.getInstance(this@getAllFiles).fileIndex
        index.iterateContent {
            add(it.getPathContentPair(this@getAllFiles))
        }
    }
}

fun VirtualFile.getAllFiles(project: Project): Set<PathContent> = buildSet {
    add(getPathContentPair(project))
    if (isFile)
        return@buildSet
    children.forEach {
        addAll(it.getAllFiles(project))
    }
}

fun List<VirtualFile>.getAllFiles(project: Project): Set<PathContent> =
    flatMap { it.getAllFiles(project) }.toSet() + getAllParents(project.guessProjectDir()!!).map {
        it.getPathContentPair(
            project
        )
    }.toSet()