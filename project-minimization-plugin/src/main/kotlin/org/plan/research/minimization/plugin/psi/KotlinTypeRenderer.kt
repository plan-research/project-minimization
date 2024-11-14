package org.plan.research.minimization.plugin.psi

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KaAnnotationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KaAnnotationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers.KaAnnotationQualifierRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KaClassTypeQualifierRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KaDefinitelyNotNullTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KaErrorTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KaFlexibleTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.idea.base.facet.platform.platform
import org.jetbrains.kotlin.idea.base.projectStructure.compositeAnalysis.findAnalyzerServices
import org.jetbrains.kotlin.idea.base.projectStructure.languageVersionSettings
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.resolve.ImportPath

@OptIn(KaExperimentalApi::class)
object KotlinTypeRenderer {
    private object SimpleKaDefinitelyNotNullTypeRenderer : KaDefinitelyNotNullTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaDefinitelyNotNullType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                typeRenderer.renderType(analysisSession, type.original, printer)
            }
        }
    }

    private object SimpleKaFlexibleTypeRenderer : KaFlexibleTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaFlexibleType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            typeRenderer.renderType(analysisSession, type.lowerBound, printer)
        }
    }

    private object SimpleKaErrorTypeRenderer : KaErrorTypeRenderer {
        @OptIn(KaNonPublicApi::class)
        override fun renderType(
            analysisSession: KaSession,
            type: KaErrorType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            type.presentableText?.let {
                printer.append(it)
                return
            }
            printer.append("Nothing")  // FIXME
        }
    }
    fun KaSession.renderType(ktFile: KtFile, returnType: KaType): String {
        val importChecker = ImportChecker(
            ktFile.packageFqName,
            ktFile.importDirectives,
            getDefaultImports(ktFile),
        )

        val renderer = KaTypeRendererForSource.WITH_QUALIFIED_NAMES.with {
            errorTypeRenderer = SimpleKaErrorTypeRenderer
            flexibleTypeRenderer = SimpleKaFlexibleTypeRenderer
            definitelyNotNullTypeRenderer = SimpleKaDefinitelyNotNullTypeRenderer
            classIdRenderer = ImportSensitiveKaClassRenderer(importChecker)
            annotationsRenderer = KaAnnotationRendererForSource.WITH_QUALIFIED_NAMES.with {
                annotationsQualifiedNameRenderer = ImportSensitiveKaAnnotationRenderer(importChecker)
            }
        }

        val printer = PrettyPrinter()
        renderer.renderType(this, returnType, printer)
        return printer.toString()
    }

    private fun getDefaultImports(file: KtFile): List<ImportPath> {
        val languageVersionSettings = file.languageVersionSettings
        val analyzerServices = file.platform.findAnalyzerServices(file.project)
        return analyzerServices.getDefaultImports(languageVersionSettings, includeLowPriorityImports = true)
    }

    private class ImportChecker(
        val packageFqName: FqName,
        val importDirectives: List<KtImportDirective>,
        val defaultImports: List<ImportPath>,
    ) {
        fun isImported(classId: ClassId?): Boolean {
            classId ?: return false

            val fullFqName = classId.asSingleFqName()
            val shortName = classId.shortClassName

            // Check if the type is in the same package as the file
            if (fullFqName.parent() == packageFqName) {
                return true
            }

            // Check if the type is default imported
            val isDefaultImported = defaultImports.any { import ->
                val importedFqName = import.fqName
                when {
                    importedFqName == fullFqName -> true
                    import.alias == shortName -> true
                    import.isAllUnder && importedFqName == fullFqName.parent() -> true
                    else -> false
                }
            }
            if (isDefaultImported) {
                return true
            }

            // Check if the type is explicitly or implicitly imported in the file
            val isImported = importDirectives.any { importDirective ->
                val importedFqName = importDirective.importedFqName
                when {
                    // Explicit import
                    importedFqName == fullFqName -> true
                    // Import with alias
                    importDirective.aliasName == shortName.asString() -> true
                    // Wildcard import (e.g., import com.example.*)
                    importDirective.isAllUnder && importedFqName == fullFqName.parent() -> true
                    else -> false
                }
            }

            return isImported
        }
    }

    private class ImportSensitiveKaClassRenderer(
        val importChecker: ImportChecker,
    ) : KaClassTypeQualifierRenderer {
        override fun renderClassTypeQualifier(
            analysisSession: KaSession,
            type: KaType,
            qualifiers: List<KaClassTypeQualifier>,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            val renderer = if (importChecker.isImported(type.symbol?.classId)) {
                KaClassTypeQualifierRenderer.WITH_SHORT_NAMES_WITH_NESTED_CLASSIFIERS
            } else {
                KaClassTypeQualifierRenderer.WITH_QUALIFIED_NAMES
            }
            renderer.renderClassTypeQualifier(analysisSession, type, qualifiers, typeRenderer, printer)
        }
    }

    private class ImportSensitiveKaAnnotationRenderer(
        val importChecker: ImportChecker,
    ) : KaAnnotationQualifierRenderer {
        override fun renderQualifier(
            analysisSession: KaSession,
            annotation: KaAnnotation,
            owner: KaAnnotated,
            annotationRenderer: KaAnnotationRenderer,
            printer: PrettyPrinter,
        ) {
            val renderer = if (importChecker.isImported(annotation.classId)) {
                KaAnnotationQualifierRenderer.WITH_SHORT_NAMES
            } else {
                KaAnnotationQualifierRenderer.WITH_QUALIFIED_NAMES
            }
            renderer.renderQualifier(analysisSession, annotation, owner, annotationRenderer, printer)
        }
    }
}
