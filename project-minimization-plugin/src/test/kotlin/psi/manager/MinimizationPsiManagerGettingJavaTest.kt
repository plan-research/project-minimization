package psi.manager

import LightTestContext
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.impl.source.PsiMethodImpl
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

    fun testSimpleClass() {
        val service = service<MinimizationPsiManagerService>()
        val psiFile = myFixture.configureByFile("simple-class.java")
        assertIs<PsiJavaFile>(psiFile)
        val context = LightTestContext(project)
        val ddItems = runBlocking {
            service.findAllPsiWithBodyItems(context)
        }

        runBlocking {
            readAction {
                val psiElements = ddItems.getPsi(context)
                assertSize(3, psiElements)

                psiElements.forEachIndexed { idx, it ->
                    assertIs<PsiMethodImpl>(it)
                    assertEquals("method${idx + 1}", it.name)
                    assertNotNull(it.body)
                    assertIs<PsiCodeBlock>(it.body)
                }
            }
        }
    }
}