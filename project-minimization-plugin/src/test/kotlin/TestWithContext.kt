import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.plugin.model.context.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.context.IJDDContextBase
import org.plan.research.minimization.plugin.model.context.IJDDContextCloner
import org.plan.research.minimization.plugin.model.context.LightIJDDContext

interface TestWithContext<C : IJDDContextBase<C>> {
    fun createContext(project: Project): C
    fun deleteContext(context: C)
}

class LightTestContext(
    projectDir: VirtualFile,
    indexProject: Project,
    originalProject: Project,
) : LightIJDDContext<LightTestContext>(projectDir, indexProject, originalProject) {
    constructor(project: Project) : this(project.guessProjectDir()!!, project, project)

    override fun copy(projectDir: VirtualFile): LightTestContext =
        LightTestContext(projectDir, indexProject, originalProject)

    override suspend fun clone(cloner: IJDDContextCloner): LightTestContext? =
        cloner.cloneLight(this)
}

class TestWithLightContext : TestWithContext<LightTestContext> {
    override fun createContext(project: Project): LightTestContext = LightTestContext(project)
    override fun deleteContext(context: LightTestContext) {}
}

class HeavyTestContext(
    project: Project,
    originalProject: Project = project,
) : HeavyIJDDContext<HeavyTestContext>(project, originalProject) {
    override fun copy(project: Project): HeavyTestContext =
        HeavyTestContext(project, originalProject)

    override suspend fun clone(cloner: IJDDContextCloner): HeavyTestContext? =
        cloner.cloneHeavy(this)
}

class TestWithHeavyContext : TestWithContext<HeavyTestContext> {
    override fun createContext(project: Project): HeavyTestContext =
        HeavyTestContext(project)

    override fun deleteContext(context: HeavyTestContext) {
        runBlocking(Dispatchers.EDT) {
            ProjectManagerEx.getInstanceEx().forceCloseProjectAsync(context.project)
        }
    }
}
