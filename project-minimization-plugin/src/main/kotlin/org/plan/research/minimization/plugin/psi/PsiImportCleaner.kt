package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.HeavyIJDDContext
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService

import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import org.jetbrains.kotlin.idea.base.codeInsight.KotlinOptimizeImportsFacility
import org.jetbrains.kotlin.psi.KtFile

class PsiImportCleaner {
    suspend fun cleanImports(context: HeavyIJDDContext, psiFile: KtFile) {
        val facility = KotlinOptimizeImportsFacility.getInstance()

        val optimizedImports = smartReadAction(context.project) {
            val importData = facility.analyzeImports(psiFile) ?: return@smartReadAction null
            if (importData.unusedImports.isEmpty()) {
                return@smartReadAction null
            }
            facility.prepareOptimizedImports(psiFile, importData)
        } ?: return

        PsiUtils.performPsiChangesAndSave(context, psiFile, "Cleaning imports in ${psiFile.name}") {
            facility.replaceImports(psiFile, optimizedImports)
        }
    }

    suspend fun isAnyUnusedImports(context: HeavyIJDDContext, psiFile: KtFile): Boolean {
        val facility = KotlinOptimizeImportsFacility.getInstance()
        return smartReadAction(context.project) {
            facility.analyzeImports(psiFile)?.unusedImports?.isNotEmpty() == true
        }
    }

    suspend fun isAnyUnusedImports(context: HeavyIJDDContext): KtFile? {
        val files = smartReadAction(context.indexProject) {
            service<MinimizationPsiManagerService>().findAllKotlinFilesInIndexProject(context)
        }
        for (file in files) {
            val ktFile = smartReadAction(context.project) {
                PsiUtils.getKtFile(context, file)
            } ?: continue
            if (isAnyUnusedImports(context, ktFile)) {
                return ktFile
            }
        }
        return null
    }

    suspend fun cleanAllImports(context: HeavyIJDDContext) {
        val files = smartReadAction(context.indexProject) {
            service<MinimizationPsiManagerService>().findAllKotlinFilesInIndexProject(context)
        }
        for (file in files) {
            val ktFile = smartReadAction(context.project) {
                PsiUtils.getKtFile(context, file)
            } ?: continue
            cleanImports(context, ktFile)
        }
    }
}
