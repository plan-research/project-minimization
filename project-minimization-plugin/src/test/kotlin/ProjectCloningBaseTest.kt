import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import org.plan.research.minimization.plugin.getAllParents
import java.nio.file.Path
import kotlin.io.path.relativeTo

abstract class ProjectCloningBaseTest : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/projectCloning"
    }

    protected fun VirtualFile.getPathContentPair(project: Project): Pair<Path, String?> =
        toNioPath().relativeTo(project.guessProjectDir()!!.toNioPath()) to
                this.takeIf { it.isFile }?.contentsToByteArray()?.toString(Charsets.UTF_8)

    protected fun Project.getAllFiles(): Set<Pair<Path, String?>> {
        return buildSet {
            val index = ProjectRootManager.getInstance(this@getAllFiles).fileIndex
            index.iterateContent {
                add(it.getPathContentPair(this@getAllFiles))
            }
        }
    }

    protected fun VirtualFile.getAllFiles(project: Project): Set<Pair<Path, String?>> = buildSet {
        add(getPathContentPair(project))
        if (isFile)
            return@buildSet
        children.forEach {
            addAll(it.getAllFiles(project))
        }
    }

    protected fun List<VirtualFile>.getAllFiles(project: Project): Set<Pair<Path, String?>> =
        flatMap { it.getAllFiles(project) }.toSet() + getAllParents(project.guessProjectDir()!!).map {
            it.getPathContentPair(
                project
            )
        }.toSet()
}