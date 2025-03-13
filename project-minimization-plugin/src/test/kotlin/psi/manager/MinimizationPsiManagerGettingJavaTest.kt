package psi.manager

import LightTestContext
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.impl.source.PsiMethodImpl
import com.intellij.psi.impl.source.tree.java.PsiLambdaExpressionImpl
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import getPsi
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.*
import org.plan.research.minimization.plugin.modification.psi.PsiUtils
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService
import kotlin.test.assertIs

class MinimizationPsiManagerGettingJavaTest : JavaCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return "src/test/resources/testData/java-psi"
    }

    override fun runInDispatchThread(): Boolean = false

    private fun testPsi(inPath: String, test: (List<PsiElement?>) -> Any?) {
        val service = service<MinimizationPsiManagerService>()
        val psiFile = myFixture.configureByFile(inPath)
        assertIs<PsiJavaFile>(psiFile)
        val context = LightTestContext(project)
        runBlocking {
            val ddItems = service.findAllPsiWithBodyItems(context)
            readAction {
                test(ddItems.getPsi(context))
            }
        }
    }

    fun testSimpleClass() {
        testPsi("simple-class.java") { psiElems ->
            assertSize(3, psiElems)

            psiElems.forEachIndexed { idx, it ->
                assertIs<PsiMethodImpl>(it)
                assertEquals("method${idx + 1}", it.name)
                assertNotNull(it.body)
                assertIs<PsiCodeBlock>(it.body)
            }
        }
    }

    fun testLambdas() {
        testPsi("lambdas.java") { psiElems ->
            assertSize(4, psiElems)

            psiElems.forEachIndexed { idx, it ->
                assertIs<PsiLambdaExpressionImpl>(it)
                assertNotNull(it.body)
                if (idx % 2 == 0) {
                    assertIs<PsiExpression>(it.body)
                } else {
                    assertIs<PsiCodeBlock>(it.body)
                }
            }
        }
    }

    fun testClassInClass() {
        testPsi("class-class.java") { psiElems ->
            assertSize(3, psiElems)

            psiElems.forEachIndexed { idx, it ->
                assertIs<PsiMethodImpl>(it)
                assertEquals(if (idx < 2) "method${idx + 1}" else "test", it.name)
                assertNotNull(it.body)
                assertIs<PsiCodeBlock>(it.body)
            }
        }
    }
}