import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.plan.research.minimization.plugin.model.HeavyIJDDContext
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext

interface TestWithContext<C : IJDDContext> {
    fun createContext(project: Project): C
    fun deleteContext(context: C)
}

class TestWithLightContext : TestWithContext<LightIJDDContext> {
    override fun createContext(project: Project): LightIJDDContext = LightIJDDContext(project)
    override fun deleteContext(context: LightIJDDContext) {}
}

class TestWithHeavyContext : TestWithContext<HeavyIJDDContext> {
    override fun createContext(project: Project): HeavyIJDDContext =
        HeavyIJDDContext(project)

    override fun deleteContext(context: HeavyIJDDContext) {
        runBlocking(Dispatchers.EDT) {
            ProjectManagerEx.getInstanceEx().forceCloseProjectAsync(context.project)
        }
    }
}
