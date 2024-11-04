import com.intellij.psi.PsiElement
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.*
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo

class PsiTrieTest : PsiTrieTestBase() {
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
        val selectedPsi = selectElements { filter(it.psi!!) }
        super.doTest(psiFile, selectedPsi) {
            assertTrue(filter(it))
        }
    }
}