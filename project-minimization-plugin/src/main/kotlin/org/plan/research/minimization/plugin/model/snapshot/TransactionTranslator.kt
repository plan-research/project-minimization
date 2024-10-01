package org.plan.research.minimization.plugin.model.snapshot

import com.intellij.openapi.vfs.VirtualFile

interface TransactionTranslator {
    fun VirtualFile.translate(): VirtualFile
}