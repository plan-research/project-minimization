package org.plan.research.minimization.plugin.psi

import org.plan.research.minimization.plugin.model.HeavyIJDDContext
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService

import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.ProgressReporter
import com.intellij.platform.util.progress.reportProgress
import org.jetbrains.kotlin.idea.imports.KotlinFirImportOptimizer
import org.jetbrains.kotlin.psi.KtFile

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class PsiImportCleaner {
    private val processorPermits = Runtime.getRuntime().availableProcessors() * 2
    private val importOptimizer = KotlinFirImportOptimizer()

    private suspend fun cleanImports(context: HeavyIJDDContext, psiFile: KtFile) {
        val replaceAction = smartReadAction(context.indexProject) { importOptimizer.processFile(psiFile) }

        PsiUtils.performPsiChangesAndSave(context, psiFile, "Cleaning imports in ${psiFile.name}") {
            replaceAction.run()
        }
    }

    private suspend fun processFiles(
        context: HeavyIJDDContext,
        reporter: ProgressReporter,
        files: List<VirtualFile>,
    ) = coroutineScope {
        val semaphore = Semaphore(processorPermits)
        for (file in files) {
            launch {
                semaphore.withPermit {
                    reporter.itemStep {
                        val ktFile = smartReadAction(context.project) {
                            PsiUtils.getKtFile(context, file)
                        }

                        ktFile?.let { cleanImports(context, it) }
                    }
                }
            }
        }
    }

    suspend fun cleanAllImports(context: HeavyIJDDContext) {
        val files = smartReadAction(context.indexProject) {
            service<MinimizationPsiManagerService>().findAllKotlinFilesInIndexProject(context)
        }

        withBackgroundProgress(context.indexProject, "Cleaning imports in all files") {
            reportProgress(files.size) { reporter ->
                processFiles(context, reporter, files)
            }
        }
    }
}
