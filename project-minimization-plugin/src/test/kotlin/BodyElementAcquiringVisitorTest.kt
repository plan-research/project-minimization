import com.intellij.openapi.application.readAction
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.KtFile
import org.plan.research.minimization.plugin.model.PsiWithBodyDDItem
import org.plan.research.minimization.plugin.psi.BodyElementAcquiringKtVisitor
import kotlin.test.assertIs

class BodyElementAcquiringVisitorTest: JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/kotlin-psi"
    }
    override fun runInDispatchThread(): Boolean = false
    fun testFunctions() {
        val visitor = BodyElementAcquiringKtVisitor(project)
        val psiFile = myFixture.configureByFile("functions.kt")
        assertIs<KtFile>(psiFile)
        runBlocking {
            readAction {
                psiFile.accept(visitor)
            }
        }

        assertSize(4, visitor.collectedElements)
        val (first, second, third, fourth) = visitor.collectedElements
        assertIs<PsiWithBodyDDItem.NamedFunctionWithBlock>(first)
        assertIs<PsiWithBodyDDItem.NamedFunctionWithBlock>(second)
        assertIs<PsiWithBodyDDItem.NamedFunctionWithoutBlock>(third)
        assertIs<PsiWithBodyDDItem.NamedFunctionWithoutBlock>(fourth)
        runBlocking {
            readAction {
                assertEquals("a", first.underlyingObject.element!!.name)
                assertEquals("b", second.underlyingObject.element!!.name)
                assertEquals("c", third.underlyingObject.element!!.name)
                assertEquals("d", fourth.underlyingObject.element!!.name)
            }
        }
    }
    fun testLambdas() {
        val visitor = BodyElementAcquiringKtVisitor(project)
        val psiFile = myFixture.configureByFile("lambda.kt")
        assertIs<KtFile>(psiFile)
        runBlocking {
            readAction {
                psiFile.accept(visitor)
            }
        }

        assertSize(3, visitor.collectedElements)
        val (first, second, third) = visitor.collectedElements
        assertIs<PsiWithBodyDDItem.LambdaExpression>(first)
        assertIs<PsiWithBodyDDItem.LambdaExpression>(second)
        assertIs<PsiWithBodyDDItem.NamedFunctionWithBlock>(third)
    }
    fun testLambdaAsDefaultParameterIsNotReplaceable() {
        val visitor = BodyElementAcquiringKtVisitor(project)
        val psiFile = myFixture.configureByFile("lambda-as-default.kt")
        assertIs<KtFile>(psiFile)
        runBlocking {
            readAction {
                psiFile.accept(visitor)
            }
        }

        assertSize(1, visitor.collectedElements)
        val (first) = visitor.collectedElements
        assertIs<PsiWithBodyDDItem.NamedFunctionWithBlock>(first)
    }
    fun testSimpleClass() {
        val visitor = BodyElementAcquiringKtVisitor(project)
        val psiFile = myFixture.configureByFile("simple-class.kt")
        assertIs<KtFile>(psiFile)
        runBlocking {
            readAction {
                psiFile.accept(visitor)
            }
        }

        assertSize(6, visitor.collectedElements)
        val (funA, funSimple, funSimple2, funSimple3, funOverridden) = visitor.collectedElements
        val funComplex = visitor.collectedElements[5]

        assertIs<PsiWithBodyDDItem.NamedFunctionWithoutBlock>(funA)
        runBlocking {
            readAction {
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
    }
}