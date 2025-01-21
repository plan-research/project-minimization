package psi.lookup

import AbstractAnalysisKotlinTest
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.waitForSmartMode
import com.intellij.psi.PsiElement
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.KtElement

abstract class AbstractLookupTestBase : AbstractAnalysisKotlinTest() {
    override fun getTestDataPath() = TEST_DATA_PATH
    abstract fun lookupFunction(element: PsiElement): List<PsiElement>
    fun doTest(
        element: KtElement,
        filterFunction: (List<Pair<PsiElement, PsiElement>>) -> List<Pair<PsiElement, PsiElement>> = { it },
        checkFunction: (List<Pair<PsiElement, PsiElement>>) -> Unit
    ) {
        configureModules(myFixture.project)
        runBlocking {
            myFixture.project.waitForSmartMode()
            val collectedElements = readAction {
                buildList {
                    element.acceptChildren(object : RecursiveKtVisitor() {
                        override fun visitKtElement(element: KtElement) {
                            super.visitKtElement(element)
                            lookupFunction(element).forEach { add(element to it) }
                        }
                    })
                }.let { filterFunction(it) }
            }
            readAction {
                checkFunction(collectedElements)
            }
        }
    }

    companion object {
        protected const val TEST_DATA_PATH = "src/test/resources/testData/lookup"
    }
}