import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase

class ModifyingBodyKtVisitorTest: JavaCodeInsightFixtureTestCase()  {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/kotlin-psi"
    }
    override fun runInDispatchThread(): Boolean = false
    fun testFunctions() {
        val psiFile = myFixture.configureByFile("functions.kt")
    }
}