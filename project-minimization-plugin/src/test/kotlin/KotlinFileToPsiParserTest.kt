import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem
import org.plan.research.minimization.plugin.psi.TopLevelFunctionModifierManager
import kotlin.test.assertIs

class KotlinFileToPsiParserTest: JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/kotlin-psi"
    }
    fun testFunctions() {
        myFixture.configureByFile("functions.kt")
        val manager = TopLevelFunctionModifierManager(project)
        val elements = manager.psiElementsWithBody
        assertSize(3, elements)
        val (first, second, third) = elements
        assertIs<PsiWithBodyDDItem.NamedFunctionWithBlock>(first)
        assertIs<PsiWithBodyDDItem.NamedFunctionWithBlock>(second)
        assertIs<PsiWithBodyDDItem.NamedFunctionWithoutBlock>(third)
        assertEquals("a", first.underlyingObject.element!!.name)
        assertEquals("b", second.underlyingObject.element!!.name)
        assertEquals("d", third.underlyingObject.element!!.name)
    }
    fun testLambdas() {
        myFixture.configureByFile("lambda.kt")
        val manager = TopLevelFunctionModifierManager(project)
        val elements = manager.psiElementsWithBody
        assertSize(3, elements)
        val (first, second, third) = elements
        assertIs<PsiWithBodyDDItem.LambdaExpression>(first)
        assertIs<PsiWithBodyDDItem.LambdaExpression>(second)
        assertIs<PsiWithBodyDDItem.NamedFunctionWithBlock>(third)
    }
    fun testLambdaAsDefaultParameterIsNotReplaceable() {
        myFixture.configureByFile("lambda-as-default.kt")
        val manager = TopLevelFunctionModifierManager(project)
        val elements = manager.psiElementsWithBody
        assertSize(1, elements)
        val (first) = elements
        assertIs<PsiWithBodyDDItem.NamedFunctionWithBlock>(first)
    }
    fun testSimpleClass() {
        myFixture.configureByFile("simple-class.kt")
        val manager = TopLevelFunctionModifierManager(project)
        val elements = manager.psiElementsWithBody
        assertSize(6, elements)
        val (funA, funSimple, funSimple2, funSimple3, funOverridden) = elements
        val funComplex = elements[5]

        assertIs<PsiWithBodyDDItem.NamedFunctionWithoutBlock>(funA)
        assertEquals("overridden", funA.underlyingObject.element!!.name)

        assertIs<PsiWithBodyDDItem.NamedFunctionWithBlock>(funSimple)
        assertEquals("simpleMethod", funSimple.underlyingObject.element!!.name)

        assertIs<PsiWithBodyDDItem.NamedFunctionWithBlock>(funSimple2)
        assertEquals("simpleMethod2", funSimple2.underlyingObject.element!!.name)

        assertIs<PsiWithBodyDDItem.NamedFunctionWithoutBlock>(funSimple3)
        assertEquals("simpleMethod3", funSimple3.underlyingObject.element!!.name)

        assertIs<PsiWithBodyDDItem.NamedFunctionWithBlock>(funOverridden)
        assertEquals("overridden", funOverridden.underlyingObject.element!!.name)

        assertIs<PsiWithBodyDDItem.NamedFunctionWithBlock>(funComplex)
        assertEquals("complexMethod", funComplex.underlyingObject.element!!.name)
    }
}