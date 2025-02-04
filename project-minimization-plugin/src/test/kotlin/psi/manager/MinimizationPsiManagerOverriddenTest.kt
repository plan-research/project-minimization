package psi.manager

import LightTestContext
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.lexer.KtTokens.OVERRIDE_KEYWORD
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.plan.research.minimization.plugin.modification.item.PsiStubDDItem
import org.plan.research.minimization.plugin.modification.psi.PsiUtils
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService
import kotlin.test.assertIs

class MinimizationPsiManagerOverriddenTest : MinimizationPsiManagerTestBase() {
    fun testInheritance() {
        val service = service<MinimizationPsiManagerService>()
        val context = LightTestContext(project)
        val psiFile = myFixture.configureByFile("inheritance.kt")
        configureModules(project)
        assertIs<KtFile>(psiFile)
        val elements = runBlocking {
            service.findDeletablePsiItems(context)
        }
        assertSize(5, elements)
        val overridden = elements.filterIsInstance<PsiStubDDItem.OverriddenPsiStubDDItem>()
        assertSize(1, overridden)
        val item = overridden.single()
        assertIs<PsiStubDDItem.OverriddenPsiStubDDItem>(item)
        assertSize(1, item.childrenElements)
        runBlocking {
            readAction {
                val psiMain = PsiUtils.getPsiElementFromItem(context, item)
                val psiChildren = item
                    .childrenElements
                    .map { PsiUtils.getPsiElementFromItem(context, it) }

                assertIs<KtNamedFunction>(psiMain)
                assertTrue(psiMain.name == "f")
                assertTrue(psiMain.modifierList?.hasModifier(OVERRIDE_KEYWORD) != true)

                assert(psiChildren.all { it is KtNamedFunction && it.modifierList?.hasModifier(OVERRIDE_KEYWORD) == true })
            }
        }
    }

    fun testComplexInheritance() {
        val service = service<MinimizationPsiManagerService>()
        val context = LightTestContext(project)
        val psiFile = myFixture.configureByFile("complex-inheritance.kt")
        configureModules(project)
        assertIs<KtFile>(psiFile)
        val elements = runBlocking {
            service.findDeletablePsiItems(context)
        }
        assertSize(6, elements)
        val overridden = elements.filterIsInstance<PsiStubDDItem.OverriddenPsiStubDDItem>()
        assertSize(2, overridden)
        runBlocking {
            readAction {
                val (fElement, gElement) = overridden.findByPsi(context) { it is KtNamedFunction && it.name == "f" }!! to
                        overridden.findByPsi(context) { it is KtNamedFunction && it.name == "g" }!!

                val funF = PsiUtils.getPsiElementFromItem(context, fElement)
                val psiChildren = fElement
                    .childrenElements
                    .map { PsiUtils.getPsiElementFromItem(context, it) }
                assertSize(2, psiChildren)
                assertIs<KtNamedFunction>(funF)
                assertTrue(funF.modifierList?.hasModifier(OVERRIDE_KEYWORD) != true)

                assert(psiChildren.all { it is KtNamedFunction && it.modifierList?.hasModifier(OVERRIDE_KEYWORD) == true })
                val funG = PsiUtils.getPsiElementFromItem(context, gElement)
                val psiChildrenG = gElement
                    .childrenElements
                    .map { PsiUtils.getPsiElementFromItem(context, it) }
                assertSize(2, psiChildrenG)
                assertIs<KtNamedFunction>(funG)
                assertTrue(funG.modifierList?.hasModifier(OVERRIDE_KEYWORD) != true)

                assert(psiChildrenG.all { it is KtNamedFunction && it.modifierList?.hasModifier(OVERRIDE_KEYWORD) == true })
            }
        }
    }

    fun testOverriddenPropertiesAndMethods() {
        val service = service<MinimizationPsiManagerService>()
        val context = LightTestContext(project)
        val psiFile = myFixture.configureByFile("inheritance-methods-properties.kt")
        configureModules(project)
        assertIs<KtFile>(psiFile)
        val elements = runBlocking {
            service.findDeletablePsiItems(context)
        }
        assertSize(7, elements)
        runBlocking {
            readAction {
                val methodWithBody = elements.findByPsi(context) { it is KtNamedFunction && it.name == "method" }!!
                val methodWithoutBody =
                    elements.findByPsi(context) { it is KtNamedFunction && it.name == "methodWithoutBody" }!!
                val property = elements.findByPsi(context) { it is KtProperty }!!

                assertSize(1, methodWithBody.childrenElements)
                val psiMethodWithBody = PsiUtils.getPsiElementFromItem(context, methodWithBody)
                assertIs<KtNamedFunction>(psiMethodWithBody)
                assertTrue(psiMethodWithBody.modifierList?.hasModifier(OVERRIDE_KEYWORD) != true)
                val psiChildrenMethod = methodWithBody
                    .childrenElements
                    .map { PsiUtils.getPsiElementFromItem(context, it) }
                assert(psiChildrenMethod.all { it is KtNamedFunction && it.modifierList?.hasModifier(OVERRIDE_KEYWORD) == true })

                assertSize(2, methodWithoutBody.childrenElements)
                val psiMethodWithoutBody = PsiUtils.getPsiElementFromItem(context, methodWithoutBody)
                assertIs<KtNamedFunction>(psiMethodWithoutBody)
                assertTrue(psiMethodWithoutBody.modifierList?.hasModifier(OVERRIDE_KEYWORD) != true)
                val psiChildrenMethodWithoutBody = methodWithoutBody
                    .childrenElements
                    .map { PsiUtils.getPsiElementFromItem(context, it) }
                assert(psiChildrenMethodWithoutBody.all {
                    it is KtNamedFunction && it.modifierList?.hasModifier(
                        OVERRIDE_KEYWORD
                    ) == true
                })

                assertSize(2, property.childrenElements)
                val psiProperty = PsiUtils.getPsiElementFromItem(context, property)
                assertIs<KtProperty>(psiProperty)
                assertTrue(psiProperty.modifierList?.hasModifier(OVERRIDE_KEYWORD) != true)
                val psiChildrenProperty = property
                    .childrenElements
                    .map { PsiUtils.getPsiElementFromItem(context, it) }
                assert(psiChildrenProperty.all {
                    it is KtNamedDeclaration && it.modifierList?.hasModifier(OVERRIDE_KEYWORD) == true
                })
            }
        }
    }
}