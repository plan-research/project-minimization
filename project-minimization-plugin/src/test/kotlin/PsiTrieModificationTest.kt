import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.plan.research.minimization.plugin.services.MinimizationPsiManager
import kotlin.test.assertIs

class PsiTrieModificationTest : PsiTrieTestBase() {
    fun testFunctionsWithBody() {
        val psiFile = loadPsiFile("functions.kt", "functions-1.kt")

        doTest(psiFile, "functions_1.kt") { it is KtNamedFunction && runBlocking { readAction { it.hasBlockBody() } } }
    }

    fun testFunctionsWithoutBody() {
        val psiFile = loadPsiFile("functions.kt", "functions-2.kt")
        doTest(psiFile, "functions_2.kt") { it is KtNamedFunction && !runBlocking { readAction { it.hasBlockBody() } } }
    }

    fun testSimpleClassWithBody() {
        val psiFile = loadPsiFile("simple-class.kt", "simple-class-1.kt")
        doTest(
            psiFile,
            "simple-class_1.kt"
        ) { it is KtNamedFunction && runBlocking { readAction { it.hasBlockBody() } } }
    }

    fun testSimpleClassWithoutBody() {
        val psiFile = loadPsiFile("simple-class.kt", "simple-class-2.kt")
        doTest(
            psiFile,
            "simple-class_2.kt"
        ) { it is KtNamedFunction && !runBlocking { readAction { it.hasBlockBody() } } }
    }

    fun testLambda() {

        val psiFile = loadPsiFile("lambda.kt", "lambda-1.kt")
        doTest(psiFile, "lambda_1.kt") { it is KtLambdaExpression }
    }

    private fun doTestComplexClass(id: Int, filter: (PsiElement) -> Boolean) {
        val psiFile = loadPsiFile("complex-class.kt", "complex-class-$id.kt")
        doTest(psiFile, "complex-class_$id.kt", filter)
    }

    fun testComplexClassMethods() {
        doTestComplexClass(1) {
            it is KtNamedFunction || it is KtLambdaExpression
        }
    }

    fun testComplexClassInitBlocks() {
        doTestComplexClass(2) {
            it is KtClassInitializer
        }
    }

    fun testComplexClassPropertyAccessors() {
        doTestComplexClass(3) {
            it is KtPropertyAccessor
        }
    }

    private fun doTest(
        psiFile: KtFile,
        expectedFile: String,
        filter: (PsiElement) -> Boolean,
    ) = runBlocking {
        val selectedPsi = selectElements { filter(it.psi!!) }
        val psiModificationManager = project.service<MinimizationPsiManager>()
        super.doTest(psiFile, selectedPsi, psiModificationManager::replaceBody)
        val expectedFile = myFixture.configureByFile("modification-results/$expectedFile")
        assertIs<KtFile>(expectedFile)
        readAction {
            kotlin.test.assertEquals(
                expectedFile.text,
                psiFile.text
            )
        }
    }
}