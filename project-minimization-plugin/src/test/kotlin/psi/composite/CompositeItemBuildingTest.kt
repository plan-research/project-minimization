package psi.composite

import AbstractAnalysisKotlinTest
import arrow.core.filterOption
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.psi.search.searches.ReferencesSearch
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.plan.research.minimization.plugin.model.context.impl.DefaultProjectContext
import org.plan.research.minimization.plugin.model.item.PsiStubChildrenCompositionItem
import org.plan.research.minimization.plugin.model.item.index.CompositeIndex
import org.plan.research.minimization.plugin.psi.PsiUtils
import org.plan.research.minimization.plugin.psi.stub.KtBlockExpressionStub
import org.plan.research.minimization.plugin.psi.stub.KtFunctionStub
import org.plan.research.minimization.plugin.psi.stub.KtPropertyStub
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService
import kotlin.io.path.Path
import kotlin.test.assertIs

class CompositeItemBuildingTest: AbstractAnalysisKotlinTest() {
    override fun getTestDataPath() = "src/test/resources/testData/kotlin-psi/composite"
    fun testSimple() = runBlocking {
        val file = myFixture.configureByFile("simple-constructor.kt")
        val context = DefaultProjectContext(project)
        assertIs<KtFile>(file)
        val psiManager = service<MinimizationPsiManagerService>()
        val constructors = readAction {
            psiManager.findPsiInKotlinFiles(context, listOf(KtClass::class.java)).mapNotNull { it.primaryConstructor }
        }
        assertSize(1, constructors)
        val calls = readAction {
            ReferencesSearch.search(constructors.first()).toList().map { it.element }
        }
        assertSize(4, calls)
        val items = readAction { calls.map { PsiUtils.buildCompositeStubItem(context, it) }.filterOption() }
        assertSize(4, items)
        val propertyCall = PsiStubChildrenCompositionItem(
            localPath = Path("simple-constructor.kt"),
            childrenPath = listOf(
                CompositeIndex.StubDeclarationIndex(KtPropertyStub("x", null, "")),
                CompositeIndex.ChildrenNonDeclarationIndex(0),
                CompositeIndex.ChildrenNonDeclarationIndex(0),
            )
        )
        val funReturnCall = PsiStubChildrenCompositionItem(
            localPath = Path("simple-constructor.kt"),
            childrenPath = listOf(
                CompositeIndex.StubDeclarationIndex(KtFunctionStub("f", emptyList(), null, "")),
                CompositeIndex.StubDeclarationIndex(KtBlockExpressionStub),
                CompositeIndex.ChildrenNonDeclarationIndex(0),
                CompositeIndex.ChildrenNonDeclarationIndex(0),
                CompositeIndex.ChildrenNonDeclarationIndex(0)
            )
        )
        val funBodyExpressionCall = PsiStubChildrenCompositionItem(
            localPath = Path("simple-constructor.kt"),
            childrenPath = listOf(
                CompositeIndex.StubDeclarationIndex(KtFunctionStub("g", emptyList(), null, "")),
                CompositeIndex.ChildrenNonDeclarationIndex(1),
                CompositeIndex.ChildrenNonDeclarationIndex(0)
            )
        )
        val compositeFunCall = PsiStubChildrenCompositionItem(
            localPath = Path("simple-constructor.kt"),
            childrenPath = listOf(
                CompositeIndex.StubDeclarationIndex(KtFunctionStub("h", emptyList(), null, "")),
                CompositeIndex.StubDeclarationIndex(KtBlockExpressionStub),
                CompositeIndex.StubDeclarationIndex(KtFunctionStub("g", emptyList(), null, "")),
                CompositeIndex.StubDeclarationIndex(KtBlockExpressionStub),
                CompositeIndex.StubDeclarationIndex(KtFunctionStub("h", emptyList(), null, "")),
                CompositeIndex.StubDeclarationIndex(KtBlockExpressionStub),
                CompositeIndex.ChildrenNonDeclarationIndex(0),
                CompositeIndex.ChildrenNonDeclarationIndex(1),
                CompositeIndex.ChildrenNonDeclarationIndex(0),
                CompositeIndex.ChildrenNonDeclarationIndex(0),
                CompositeIndex.ChildrenNonDeclarationIndex(0),
            )
        )
        assertContainsElements(items, propertyCall, funReturnCall, funBodyExpressionCall, compositeFunCall)
    }
}