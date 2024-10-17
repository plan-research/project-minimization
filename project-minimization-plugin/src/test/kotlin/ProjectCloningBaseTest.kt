import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase

abstract class ProjectCloningBaseTest : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/projectCloning"
    }

    override fun runInDispatchThread(): Boolean = false
}