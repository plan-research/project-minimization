package psi.trie

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.LightIJDDContext
import org.plan.research.minimization.plugin.model.PsiStubDDItem
import org.plan.research.minimization.plugin.psi.PsiUtils
import org.plan.research.minimization.plugin.psi.stub.KtStub
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService
import kotlin.test.assertIs

class PsiTrieDeletionTest : PsiTrieTestBase<PsiStubDDItem, KtStub>() {
    fun testFunctions() {
        val psiFile = loadPsiFile("functions.kt", "functions-del-1.kt")
        doTest(
            psiFile,
            "functions-1.kt",
        ) {
            runBlocking {
                readAction {
                    it is KtNamedFunction && (it.name == "doNotParseIt" || it.name == "d")
                }
            }
        }
    }

    fun testLambdas() {
        val psiFile = loadPsiFile("lambda.kt", "lambda-del-1.kt")
        doTest(
            psiFile,
            "lambda-1.kt",
        ) {
            runBlocking {
                readAction {
                    it is KtProperty && it.name == "y"
                }
            }
        }
    }

    fun testFunctionFunctions() {
        val psiFile = loadPsiFile("function-function.kt", "function-function-1.kt")
        doTest(
            psiFile,
            "fun-fun-1.kt"
        ) {
            runBlocking {
                readAction {
                    it is KtNamedFunction && (it.name == "fun2" || it.name == "fun6")
                }
            }
        }
    }

    fun testClassClass() {
        val psiFile = loadPsiFile("class-class.kt", "class-class-1.kt")
        doTest(
            psiFile,
            "cl-cl.kt"
        ) {
            runBlocking {
                readAction {
                    (it is KtNamedFunction && it.name == "test") ||
                            (it is KtClass && it.name == "Aboba") ||
                            (it is KtProperty && it.name == "wow")
                }
            }
        }
    }

    fun testFunctionVariable() {
        val psiFile = loadPsiFile("function-variable.kt", "function-variable-1.kt")
        doTest(
            psiFile,
            "fun-var-1.kt"
        ) {
            runBlocking {
                readAction {
                    (it is KtProperty && it.name == "x") ||
                            (it is KtClass && it.name == "DataClass")
                }
            }
        }
    }

    private fun doTest(
        psiFile: KtFile,
        expectedFile: String,
        filter: (PsiElement) -> Boolean,
    ) = runBlocking {
        val context = LightIJDDContext(project)
        val selectedPsi = readAction { runBlocking { selectElements(context) { filter(it.psi(context)!!) } } }
        super.doTest(psiFile, selectedPsi) { _, it -> it.delete() }
        val expectedFile = myFixture.configureByFile("deletion-results/$expectedFile")
        assertIs<KtFile>(expectedFile)
        readAction {
            kotlin.test.assertEquals(
                expectedFile.text,
                psiFile.text
            )
        }
    }

    private fun PsiStubDDItem.psi(context: IJDDContext) =
        PsiUtils.getPsiElementFromItem(context, this)

     override suspend fun getAllElements(context: IJDDContext): List<PsiStubDDItem> {
        val service = service<MinimizationPsiManagerService>()
        return service.findDeletablePsiItems(context)
    }
}