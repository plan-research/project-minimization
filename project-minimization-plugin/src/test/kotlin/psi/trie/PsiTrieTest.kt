package psi.trie

import LightTestContext
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.*
import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.modification.item.PsiChildrenIndexDDItem
import org.plan.research.minimization.plugin.modification.item.index.IntChildrenIndex
import org.plan.research.minimization.plugin.modification.psi.PsiUtils
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo

class PsiTrieTest : PsiTrieTestBase<PsiChildrenIndexDDItem, IntChildrenIndex>() {
    override suspend fun getAllElements(context: IJDDContext): List<PsiChildrenIndexDDItem> {
        val service = service<MinimizationPsiManagerService>()
        return service.findAllPsiWithBodyItems(context)
    }
    fun testFunctionsWithBody() {
        val psiFile = loadPsiFile("functions.kt", "functions_1.kt")

        doTest(psiFile) { it is KtNamedFunction && it.hasBody() }
    }

    fun testFunctionsWithoutBody() {
        val psiFile = loadPsiFile("functions.kt", "functions_2.kt")
        doTest(psiFile) { it is KtNamedFunction && !it.hasBody() }
    }

    fun testSimpleClassWithBody() {
        val psiFile = loadPsiFile("simple-class.kt", "simple-class_1.kt")
        doTest(psiFile) { it is KtNamedFunction && it.hasBody() }
    }

    fun testSimpleClassWithoutBody() {
        val psiFile = loadPsiFile("simple-class.kt", "simple-class_2.kt")
        doTest(psiFile) { it is KtNamedFunction && !it.hasBody() }
    }

    fun testLambda() {

        val psiFile = loadPsiFile("lambda.kt", "lambda_1.kt")
        doTest(psiFile) { it is KtLambdaExpression }
    }

    private fun doTestComplexClass(id: Int, filter: (PsiElement) -> Boolean) {
        val psiFile = loadPsiFile("complex-class.kt", "complex-class_$id.kt")
        doTest(psiFile, filter)
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

    fun testAllFilesWithEmptySet() {
        val path = Path(testDataPath)
        path.toFile().walk().filter { it.isFile }.forEach {
            val fileName = it.toPath().relativeTo(path)
            val fileNameChanged = path.resolve("${fileName.nameWithoutExtension}_999.kt")
            val psiFile = loadPsiFile(fileName.toString(), fileNameChanged.toString())
            doTest(psiFile) { false }
        }
    }

    private fun doTest(
        psiFile: KtFile,
        filter: (PsiElement) -> Boolean,
    ) = runBlocking {
        val context = LightTestContext(project)
        val selectedPsi = selectElements(context) { readAction { filter(PsiUtils.getPsiElementFromItem(context, it)!!) } }
        super.doTest(psiFile, selectedPsi) { _, it ->
            assertTrue(filter(it))
        }
    }
}