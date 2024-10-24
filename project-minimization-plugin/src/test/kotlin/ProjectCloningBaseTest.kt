import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import org.plan.research.minimization.plugin.services.ProjectCloningService

abstract class ProjectCloningBaseTest : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/projectCloning"
    }

    override fun runInDispatchThread(): Boolean = false

    override fun setUp() {
        super.setUp()
        project.service<ProjectCloningService>().isTest = true
    }
}