package org.plan.research.minimization.plugin.model

import com.intellij.psi.PsiElement
import org.plan.research.minimization.core.model.DDItem

// TODO: think about make it sealed and create some different implementations, e.g. PSI or VF
data class PsiDDItem(val psi: PsiElement): DDItem