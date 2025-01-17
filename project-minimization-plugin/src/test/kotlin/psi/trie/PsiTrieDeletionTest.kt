package psi.trie

import LightTestContext
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiElement
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.idea.util.isComma
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem
import org.plan.research.minimization.plugin.psi.PsiUtils
import org.plan.research.minimization.plugin.psi.stub.KtStub
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

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

    fun testValueParameters() {
        return // Deprecated
        val psiFile = loadPsiFile("function-variable.kt", "function-variable-2.kt")
        doTest(
            psiFile,
            "fun-var-2.kt"
        ) { runBlocking { readAction { (it is KtParameter && it.name == "x") } } }
    }

    fun testTypeAlias() {
        val psiFile = loadPsiFile("type-alias.kt", "type-alias-1.kt")
        doTest(
            psiFile,
            "type-alias_1.kt"
        ) { runBlocking { readAction { it is KtTypeAlias } } }
    }

    fun testTypeAlias2() {
        val psiFile = loadPsiFile("type-alias.kt", "type-alias-2.kt")
        doTest(
            psiFile,
            "type-alias_2.kt"
        ) {
            runBlocking {
                readAction {
                    it is KtTypeAlias &&
                            (it.children.find { it is KtTypeParameterList } as? KtTypeParameterList)
                                ?.parameters
                                ?.isNotEmpty() != true
                }
            }
        }

    }
    fun testTypeAlias3() {
        val psiFile = loadPsiFile("type-alias.kt", "type-alias-3.kt")
        val names = setOf(
            "B", "D", "F", "G2"
        )
        doTest(
            psiFile,
            "type-alias_3.kt"
        ) {
            runBlocking {
                readAction { it is KtTypeAlias && it.name !in names }
            }
        }
    }
    fun testTypeAlias4() {
        val psiFile = loadPsiFile("type-alias.kt", "type-alias-4.kt")
        val names = setOf(
            "B", "D", "F", "G2"
        )
        doTest(
            psiFile,
            "type-alias_4.kt"
        ) {
            runBlocking {
                readAction { it is KtTypeAlias && it.name in names }
            }
        }
    }

    private fun doTest(
        psiFile: KtFile,
        expectedFile: String,
        filter: (PsiElement) -> Boolean,
    ) = runBlocking {
        val context = LightTestContext(project)
        val selectedPsi = readAction { runBlocking { selectElements(context) { filter(it.psi(context)!!) } } }
        super.doTest(psiFile, selectedPsi) { _, it ->
            var nextSibling = it.nextSibling
            it.delete()
            if (nextSibling?.isComma == true) {
                nextSibling.delete()
            }
        }
        val path = psiFile.containingFile.virtualFile.toNioPath().relativeTo(project.guessProjectDir()!!.toNioPath())
        myFixture.checkResultByFile(path.pathString, "deletion-results/$expectedFile", true)
    }

    private fun PsiStubDDItem.psi(context: IJDDContext) =
        PsiUtils.getPsiElementFromItem(context, this)

    override suspend fun getAllElements(context: IJDDContext): List<PsiStubDDItem> {
        val service = service<MinimizationPsiManagerService>()
        return service.findDeletablePsiItems(context)
    }
}