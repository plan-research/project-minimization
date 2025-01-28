package psi.manager

import LightTestContext
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasBody
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService
import kotlin.test.assertIs

class MinimizationPsiManagerDeletablePsiTest : MinimizationPsiManagerTestBase() {
    fun testFunctionFunction() {
        val service = service<MinimizationPsiManagerService>()
        val context = LightTestContext(project)
        val psiFile = myFixture.configureByFile("function-function.kt")
        assertIs<KtFile>(psiFile)
        val elements = runBlocking {
            service.findDeletablePsiItems(context)
        }
        val psi = runBlocking { readAction { elements.getPsi(context) } }
        assertIs<List<KtNamedFunction>>(psi)
        assertSize(6, psi)
        val doesNotHaveBlockBody = setOf(2, 4, 5)
        psi.forEachIndexed { idx, function ->
            runBlocking {
                readAction {
                    assertEquals("fun${idx + 1}", function.name)
                    assertTrue(idx in doesNotHaveBlockBody || function.hasBlockBody())
                }
            }
        }
    }

    fun testFunctionVariable() {
        val service = service<MinimizationPsiManagerService>()
        val context = LightTestContext(project)
        val psiFile = myFixture.configureByFile("function-variable.kt")
        assertIs<KtFile>(psiFile)
        val elements = runBlocking {
            service.findDeletablePsiItems(context, compressOverridden = false)
        }
        val psi = runBlocking { readAction { elements.getPsi(context) } }
        assertSize(4, psi)
        // TODO: Enable if class constructor fetching is done
//        assertSize(6, psi)
//        val (fn, clazz, classVar1, classVar2, var2) = psi
//        val var1 = psi[5]
        val (fn, clazz, var2, var1) = psi
        runBlocking {
            readAction {
                assertIs<KtNamedFunction>(fn)
                assertEquals("fn", fn.name)
                assertTrue(fn.hasBlockBody())
                assertIs<KtClass>(clazz)
                assertEquals("DataClass", clazz.name)
                assertTrue(clazz.isData())
                assertIs<KtProperty>(var1)
                assertEquals("z", var1.name)
                assertIs<KtProperty>(var2)
                assertEquals("x", var2.name)

                // TODO: Enable if class constructor fetching is done
//                assertIs<KtParameter>(classVar1)
//                assertEquals("x", classVar1.name)
//
//                assertIs<KtParameter>(classVar2)
//                assertEquals("y", classVar2.name)
            }
        }
    }
    fun testClassClass() {
        val service = service<MinimizationPsiManagerService>()
        val context = LightTestContext(project)
        val psiFile = myFixture.configureByFile("class-class.kt")
        assertIs<KtFile>(psiFile)
        val elements = runBlocking {
            service.findDeletablePsiItems(context)
        }
        val psi = runBlocking { readAction { elements.getPsi(context) } }
        assertSize(11, psi)
        val (clazz, fn, clazz2, object1, fn2) = psi.subList(0, 5)
        val (val5, object2, var4, var3, var1) = psi.subList(5, 10)
        val (var2) = psi.subList(10, 11)
        runBlocking {
            readAction {
                assertIs<KtClass>(clazz)
                assertEquals("C", clazz.name)

                assertIs<KtNamedFunction>(fn)
                assertEquals("f", fn.name)
                assertTrue(fn.hasBlockBody())

                assertIs<KtClass>(clazz2)
                assertEquals("Aboba", clazz2.name)
                assertFalse(clazz2.hasBody())

                assertIs<KtProperty>(var1)
                assertEquals("x", var1.name)

                assertIs<KtProperty>(var2)
                assertEquals("y", var2.name)

                assertIs<KtObjectDeclaration>(object1)
                assertTrue(object1.isCompanion())
                assertNotNull(object1.body)

                assertIs<KtObjectDeclaration>(object2)
                assertFalse(object2.isCompanion())
                assertFalse(object2.hasBody())

                assertIs<KtProperty>(var3)
                assertEquals("wow", var3.name)
                assertTrue(var3.isPrivate())
//                assertTrue(var3.isConstant())
                assertFalse(var3.isVar)

                assertIs<KtProperty>(var4)
                assertEquals("hmm", var4.name)

                assertIs<KtNamedFunction>(fn2)
                assertEquals("test", fn2.name)
                assertTrue(fn2.hasBlockBody())

                assertIs<KtProperty>(val5)
                assertEquals("x", val5.name)
                assertTrue(val5.isVar)
            }
        }
    }
}