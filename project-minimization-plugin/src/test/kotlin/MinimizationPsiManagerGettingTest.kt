import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.*
import org.plan.research.minimization.plugin.model.PsiDDItem
import org.plan.research.minimization.plugin.services.MinimizationPsiManager
import kotlin.test.assertIs

class MinimizationPsiManagerGettingTest : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/kotlin-psi"
    }

    override fun runInDispatchThread(): Boolean = false
    fun testFunctions() {
        val service = project.service<MinimizationPsiManager>()
        val psiFile = myFixture.configureByFile("functions.kt")
        assertIs<KtFile>(psiFile)
        val elements = runBlocking {
            service.findAllPsiWithBodyItems()
        }

        assertSize(4, elements)
        val (first, second, third, fourth) = elements.getPsi(service)
        assertIs<KtNamedFunction>(first)
        assertIs<KtNamedFunction>(second)
        assertIs<KtNamedFunction>(third)
        assertIs<KtNamedFunction>(fourth)
        runBlocking {
            readAction {
                assertEquals("a", first.name)
                assertTrue(first.hasBlockBody())
                assertEquals("b", second.name)
                assertTrue(second.hasBlockBody())

                assertEquals("c", third.name)
                assertFalse(third.hasBlockBody())
                assertEquals("d", fourth.name)
                assertFalse(fourth.hasBlockBody())
            }
        }
    }

    fun testLambdas() {
        val service = project.service<MinimizationPsiManager>()
        val psiFile = myFixture.configureByFile("lambda.kt")
        assertIs<KtFile>(psiFile)
        val elements = runBlocking {
            service.findAllPsiWithBodyItems()
        }

        assertSize(3, elements)
        val (first, second, third) = elements.getPsi(service)
        assertIs<KtLambdaExpression>(first)
        assertIs<KtLambdaExpression>(second)
        assertIs<KtNamedFunction>(third)
        runBlocking {
            readAction {
                assertTrue(third.hasBlockBody())
            }
        }
    }

    fun testLambdaAsDefaultParameterIsNotReplaceable() {
        val service = project.service<MinimizationPsiManager>()
        val psiFile = myFixture.configureByFile("lambda-as-default.kt")
        assertIs<KtFile>(psiFile)
        val elements = runBlocking {
            service.findAllPsiWithBodyItems()
        }

        assertSize(1, elements)
        val first = runBlocking { service.getPsiElementFromItem(elements[0]) }
        assertIs<KtNamedFunction>(first)
        runBlocking {
            readAction {
                assertTrue(first.hasBlockBody())
            }
        }
    }

    fun testSimpleClass() {
        val service = project.service<MinimizationPsiManager>()
        val psiFile = myFixture.configureByFile("simple-class.kt")
        assertIs<KtFile>(psiFile)
        val elements = runBlocking {
            service.findAllPsiWithBodyItems()
        }
        val mappedElements = elements.getPsi(service)
        assertSize(6, mappedElements)
        val (funA, funSimple, funSimple2, funSimple3, funOverridden) = mappedElements
        val funComplex = mappedElements[5]

        runBlocking {
            readAction {
                assertIs<KtNamedFunction>(funA)
                assertEquals("overridden", funA.name)
                assertFalse(funA.hasBlockBody())

                assertIs<KtNamedFunction>(funSimple)
                assertEquals("simpleMethod", funSimple.name)
                assertTrue(funSimple.hasBlockBody())

                assertIs<KtNamedFunction>(funSimple2)
                assertEquals("simpleMethod2", funSimple2.name)
                assertTrue(funSimple2.hasBlockBody())

                assertIs<KtNamedFunction>(funSimple3)
                assertEquals("simpleMethod3", funSimple3.name)
                assertFalse(funSimple3.hasBlockBody())

                assertIs<KtNamedFunction>(funOverridden)
                assertEquals("overridden", funOverridden.name)
                assertTrue(funOverridden.hasBlockBody())

                assertIs<KtNamedFunction>(funComplex)
                assertEquals("complexMethod", funComplex.name)
                assertTrue(funComplex.hasBlockBody())
            }
        }
    }

    fun testComplexClass() {
        val service = project.service<MinimizationPsiManager>()
        val psiFile = myFixture.configureByFile("complex-class.kt")
        assertIs<KtFile>(psiFile)
        val elements = runBlocking {
            service.findAllPsiWithBodyItems()
        }
        val mappedElements = elements.getPsi(service)
        assertSize(8, elements)
        val (first, second, third, fourth, fifth) = mappedElements
        val sixth = mappedElements[5]
        val seventh = mappedElements[6]
        val eighth = mappedElements[7]
        runBlocking {
            readAction {
                assertIs<KtNamedFunction>(first)
                assertTrue(first.hasBlockBody())
                assertEquals("method", first.name)

                assertIs<KtNamedFunction>(second)
                assertFalse(second.hasBlockBody())
                assertEquals("method2", second.name)

                assertIs<KtLambdaExpression>(third)

                assertIs<KtClassInitializer>(fourth)

                assertIs<KtPropertyAccessor>(fifth)
                assertTrue(fifth.isGetter)
                assertTrue(fifth.hasBlockBody())
                assertIs<KtPropertyAccessor>(sixth)
                assertTrue(sixth.isGetter)
                assertFalse(sixth.hasBlockBody())

                assertIs<KtPropertyAccessor>(seventh)
                assertTrue(seventh.isSetter)

                assertIs<KtClassInitializer>(eighth)
            }
        }
    }

    private fun compare(a: List<Int>, b: List<Int>): Int {
        val maxIndex = a.size.coerceAtLeast(b.size)
        for (i in 0 until maxIndex) {
            val aValue = a.getOrNull(i) ?: return -1
            val bValue = b.getOrNull(i) ?: return 1
            if (aValue != bValue) {
                return aValue - bValue
            }
        }
        return 0
    }
    private fun List<PsiDDItem>.getPsi(service: MinimizationPsiManager) = runBlocking {
        sortedWith { a, b -> compare(a.childrenPath, b.childrenPath) }
            .map { service.getPsiElementFromItem(it) }
    }
}