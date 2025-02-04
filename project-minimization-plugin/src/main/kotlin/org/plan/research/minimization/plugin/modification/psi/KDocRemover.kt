package org.plan.research.minimization.plugin.modification.psi

import org.plan.research.minimization.plugin.context.HeavyIJDDContext
import org.plan.research.minimization.plugin.services.MinimizationPsiManagerService

import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.ProgressReporter
import com.intellij.platform.util.progress.reportProgress
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtFile

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

// FIXME: Make a post processor interface / abstract class
class KDocRemover {
    private val processorPermits = Runtime.getRuntime().availableProcessors() * 2
    private val psiManager = service<MinimizationPsiManagerService>()

    suspend fun removeKDocs(from: HeavyIJDDContext<*>) {
        val files = smartReadAction(from.indexProject) {
            psiManager.findAllKotlinFilesInIndexProject(from)
        }
        withBackgroundProgress(from.indexProject, "Removing KDocs from files") {
            reportProgress(files.size) { reporter ->
                from.processFiles(reporter, files)
            }
        }
    }
    private suspend fun HeavyIJDDContext<*>.processFiles(progressReporter: ProgressReporter, files: List<VirtualFile>) = coroutineScope {
        val semaphore = Semaphore(processorPermits)
        for (file in files) {
            launch {
                semaphore.withPermit {
                    progressReporter.itemStep {
                        val ktFile = smartReadAction(project) {
                            PsiUtils.getKtFile(this@processFiles, file)
                        }

                        ktFile?.let { removeKDocs(it) }
                    }
                }
            }
        }
    }
    private suspend fun HeavyIJDDContext<*>.removeKDocs(file: KtFile) {
        val kDocs = smartReadAction(indexProject) {
            PsiTreeUtil.collectElementsOfType(file, KDoc::class.java)
        }
        PsiUtils.performPsiChangesAndSave(this, file, "Removing KDoc from file") {
            kDocs.forEach { it.delete() }
        }
    }
}
