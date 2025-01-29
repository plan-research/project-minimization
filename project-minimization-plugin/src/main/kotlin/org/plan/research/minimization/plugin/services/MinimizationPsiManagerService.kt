package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.graph.InstanceLevelGraph
import org.plan.research.minimization.plugin.model.graph.PsiIJEdge
import org.plan.research.minimization.plugin.model.item.PsiChildrenIndexDDItem
import org.plan.research.minimization.plugin.model.item.PsiStubChildrenCompositionItem
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem
import org.plan.research.minimization.plugin.model.item.PsiStubDDItem.CallablePsiStubDDItem
import org.plan.research.minimization.plugin.psi.KotlinElementLookup
import org.plan.research.minimization.plugin.psi.KotlinOverriddenElementsGetter
import org.plan.research.minimization.plugin.psi.PsiDSU
import org.plan.research.minimization.plugin.psi.PsiUtils
import org.plan.research.minimization.plugin.psi.usages.MethodUserSearcher

import arrow.core.compareTo
import arrow.core.filterOption
import arrow.core.raise.option
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.concurrency.annotations.RequiresReadLock
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jgrapht.graph.DirectedPseudograph
import org.jetbrains.kotlin.psi.psiUtil.containingClass

import kotlin.collections.plus
import kotlin.collections.singleOrNull
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

private typealias NodesAndEdges = Pair<List<PsiStubDDItem>, List<PsiIJEdge>>

/**
 * Service that provides functions to get a list of all the psi elements that could be modified
 */
@Service(Service.Level.APP)
class MinimizationPsiManagerService {
    private val logger = KotlinLogging.logger {}

    private val KtElement.selfOrConstructorIfFunctionOrClass: KtFunction?
        get() = when (this) {
            is KtClass -> primaryConstructor
            is KtFunction -> this
            else -> null
        }

    suspend fun findAllPsiWithBodyItems(context: IJDDContext): List<PsiChildrenIndexDDItem> =
        smartReadAction(context.indexProject) {
            findPsiInKotlinFiles(context, PsiChildrenIndexDDItem.BODY_REPLACEABLE_PSI_JAVA_CLASSES)
                .filter { PsiChildrenIndexDDItem.hasBodyIfAvailable(it) != false }
                .mapNotNull { PsiUtils.buildReplaceablePsiItem(context, it) }
        }

    /**
     * Finds all deletable PSI items in Kotlin files within the given context.
     * The deletable elements are the elements that
     *  * are one of the [PsiStubDDItem.DELETABLE_PSI_JAVA_CLASSES] classes
     *  * on the path from [org.jetbrains.kotlin.psi.KtFile] to that element there are only serializable elements.
     *  That means that they could be represented via [org.plan.research.minimization.plugin.psi.stub.KtStub]
     *
     * @param context The context for the minimization process containing the current project and relevant properties.
     * @param compressOverridden If set to true, then all overridden elements will be compressed to one element
     * @param withFunctionParameters
     * @return A list of deletable PSI items found in the Kotlin files of the project.
     */
    suspend fun findDeletablePsiItems(
        context: IJDDContext,
        compressOverridden: Boolean = true,
        withFunctionParameters: Boolean = false,
    ): List<PsiStubDDItem> =
        if (compressOverridden) {
            findDeletablePsiItemsCompressed(context)
        } else {
            findDeletablePsiItemsWithoutCompression(context, withFunctionParameters).map { it.item }
        }

    private suspend fun findDeletablePsiItemsWithoutCompression(
        context: IJDDContext,
        withFunctionParameters: Boolean,
    ): List<IntermediatePsiItemInfo> {
        val coreElements = smartReadAction(context.indexProject) {
            findPsiInKotlinFiles(context, PsiStubDDItem.DELETABLE_PSI_JAVA_CLASSES)
                .mapNotNull { psiElement ->
                    PsiUtils.buildDeletablePsiItem(context, psiElement).getOrNull()
                        ?.let { IntermediatePsiItemInfo(psiElement, it) }
                }
        }
        logger.debug { "Got ${coreElements.size} core elements" }
        val functionParameters = takeIf { withFunctionParameters }
            ?.getFunctionsProperties(
                context,
                coreElements.mapNotNull { readAction { it.psiElement.selfOrConstructorIfFunctionOrClass } },
            )
            .orEmpty()
        logger.debug { "Got ${functionParameters.size} function parameters" }
        return coreElements + functionParameters
    }

    private suspend fun findDeletablePsiItemsCompressed(
        context: IJDDContext,
    ): List<PsiStubDDItem> {
        val nonStructuredItems = findDeletablePsiItemsWithoutCompression(context, false)

        val dsu =
            PsiDSU<KtElement, PsiStubDDItem>(nonStructuredItems.map(IntermediatePsiItemInfo::asPair)) { lhs, rhs ->
                lhs.childrenPath.size
                    .compareTo(rhs.childrenPath.size)
                    .takeIf { it != 0 }
                    ?: lhs.childrenPath.compareTo(rhs.childrenPath)
            }
        return smartReadAction(context.indexProject) {
            dsu.transformItems(context, nonStructuredItems)
        }
    }

    /**
     * Builds an instance-level graph representing the deletable PSI (Program Structure Interface) elements
     * and their relationships within the given context.
     *
     * @param context The minimization context containing information about the current project and relevant properties.
     * @param withFunctionParameters If set to `true` then the constructor and function parameters will be included in the graph
     * @return An instance of [InstanceLevelGraph] containing the vertices (deletable PSI items) and edges (connections between them).
     */
    suspend fun buildDeletablePsiGraph(context: IJDDContext, withFunctionParameters: Boolean): InstanceLevelGraph {
        val nodes = findDeletablePsiItemsWithoutCompression(context, withFunctionParameters)

        val psiCache = nodes.associate { it.psiElement to it.item }
        val (fileHierarchyNodes, fileHierarchyEdges) = buildFileHierarchy(context)
        val vertices = (nodes.map(IntermediatePsiItemInfo::item) + fileHierarchyNodes).distinct()
        val edges = buildInstanceLevelGraphEdges(nodes, context, psiCache) + fileHierarchyEdges

        val builder = DirectedPseudograph.createBuilder<PsiStubDDItem, PsiIJEdge>(PsiIJEdge::class.java)

        vertices.forEach { builder.addVertex(it) }
        edges.forEach { builder.addEdge(it.from, it.to, it) }

        return builder.build()
    }

    @Suppress("TOO_LONG_FUNCTION")
    private suspend fun buildInstanceLevelGraphEdges(
        nodes: List<IntermediatePsiItemInfo>,
        context: IJDDContext,
        psiCache: Map<KtElement, PsiStubDDItem>,
    ): List<PsiIJEdge> {
        // TODO: remake using dsl
        val filesNodes = nodes
            .map { (_, item) -> PsiStubDDItem.NonOverriddenPsiStubDDItem(item.localPath, emptyList()) }
        val fileEdges =
            nodes.zip(filesNodes).map { (from, fileNode) -> PsiIJEdge.PSITreeEdge(from.item, fileNode) }
        val psiEdges =
            nodes.mapNotNull { (_, from) ->
                readAction {
                    PsiUtils
                        .findAllDeletableParentElements(context, from)
                        ?.let { PsiIJEdge.PSITreeEdge(from, it) }
                }
            }
        val overloadEdges =
            nodes.flatMap { (element, from) ->
                smartReadAction(context.indexProject) {
                    KotlinOverriddenElementsGetter.getOverriddenElements(element).process(context, psiCache)
                        .map { PsiIJEdge.Overload(from, it) }
                }
            }
        val usageEdges =
            nodes.flatMap { (element, from) ->
                smartReadAction(context.indexProject) {
                    PsiUtils.collectUsages(element).process(context, psiCache)
                        .map { PsiIJEdge.UsageInPSIElement(from, it) }
                }
            }
        val obligatoryOverride =
            nodes.flatMap { (element, from) ->
                smartReadAction(context.indexProject) {
                    KotlinElementLookup.lookupObligatoryOverrides(element).process(context, psiCache)
                        .map { PsiIJEdge.ObligatoryOverride(from, it) }
                }
            }
        val expectActual = nodes.flatMap { (element, from) ->
            smartReadAction(context.indexProject) {
                KotlinElementLookup.lookupExpected(element).process(context, psiCache)
                    .map { PsiIJEdge.UsageInPSIElement(from, it) }
            }
        }
        return psiEdges + overloadEdges + usageEdges + obligatoryOverride + fileEdges + expectActual
    }

    private suspend fun buildFileHierarchy(context: IJDDContext): NodesAndEdges {
        val kotlinFiles = smartReadAction(context.indexProject) { findAllKotlinFilesInIndexProject(context) }
        val projectRoot = context.indexProjectDir
        val sourceRoots = smartReadAction(context.indexProject) {
            service<RootsManagerService>().findPossibleRoots(context).map {
                projectRoot.findFileByRelativePath(it.pathString)
            }
        }
        val edges = buildList {
            for (file in kotlinFiles) {
                var currentFile = file
                while (currentFile !in sourceRoots) {
                    val parent = currentFile.parent ?: break
                    readAction {
                        PsiIJEdge.FileTreeEdge.create(context, currentFile, parent).onSome(::add)
                    }
                    currentFile = parent
                }
            }
        }
        return edges.flatMap { listOf(it.from, it.to) } to edges
    }

    /**
     * Finds all kotlin files inside the given context that are in the source roots
     *
     * @param context
     */
    @RequiresReadLock
    fun findAllKotlinFilesInIndexProject(context: IJDDContext): List<VirtualFile> {
        val roots = service<RootsManagerService>().findPossibleRoots(context)
        logger.debug {
            "Found ${roots.size} roots: $roots"
        }
        val rootFiles = roots.mapNotNull {
            context.indexProjectDir.findFileByRelativePath(it.pathString)
        }
        return myVirtualFileTraverse(rootFiles)
    }

    @RequiresReadLock
    private fun myVirtualFileTraverse(roots: List<VirtualFile>): List<VirtualFile> = buildSet<VirtualFile> {
        roots.forEach { root ->
            VfsUtilCore.visitChildrenRecursively(root, object : VirtualFileVisitor<Unit>() {
                override fun visitFileEx(file: VirtualFile): Result {
                    if (file.isDirectory) {
                        return CONTINUE
                    }
                    if (file.extension == KotlinFileType.EXTENSION) {
                        add(file)
                    }
                    return CONTINUE
                }
            })
        }
    }.toList()

    @RequiresReadLock
    fun <T : PsiElement> findPsiInKotlinFiles(
        context: IJDDContext,
        classes: List<Class<out T>>,
    ): List<T> {
        val kotlinFiles = findAllKotlinFilesInIndexProject(context)
        logger.debug {
            "Found ${kotlinFiles.size} kotlin files"
        }
        if (kotlinFiles.isEmpty()) {
            logger.warn { "Found 0 kotlin files!" }
        }
        return extractAllPsi(context, kotlinFiles, classes)
    }

    @RequiresReadLock
    private fun <T : PsiElement> extractAllPsi(
        context: IJDDContext,
        files: Collection<VirtualFile>,
        classes: List<Class<out T>>,
    ): List<T> =
        files.flatMap { kotlinFile ->
            val relativePath = kotlinFile.toNioPath().relativeTo(context.indexProjectDir.toNioPath())
            val fileInCurrentProject = context.projectDir.findFileByRelativePath(relativePath.pathString)
                ?: return@flatMap emptyList()

            val ktFileInCurrentProject = PsiUtils.getKtFile(context, fileInCurrentProject)
                ?: return@flatMap emptyList()

            classes.flatMap { clazz ->
                PsiTreeUtil.collectElementsOfType(ktFileInCurrentProject, clazz)
                    .also {
                        logger.trace {
                            "Found ${it.size} ${clazz.simpleName} elements in $relativePath"
                        }
                    }
            }
        }

    private fun PsiDSU<KtElement, PsiStubDDItem>.transformItems(
        context: IJDDContext,
        items: List<IntermediatePsiItemInfo>,
    ): List<PsiStubDDItem> {
        items.forEach { (element) ->
            KotlinOverriddenElementsGetter
                .getOverriddenElements(element, includeClass = false)
                .filter { it.isFromContext(context) }
                .forEach { union(element, it) }
        }
        val classes = classes
        return items
            .asSequence()
            .filter { (psiElement, item) -> representativeElementOf(psiElement) === item }
            .map { (_, item) ->
                val clazz = classes[item]!!
                clazz.singleOrNull() ?: PsiStubDDItem.OverriddenPsiStubDDItem(
                    item.localPath,
                    item.childrenPath,
                    clazz.filter { it != item },
                )
            }
            .toList()
    }

    private suspend fun getFunctionsProperties(
        context: IJDDContext,
        classes: List<KtFunction>,
    ): List<IntermediatePsiItemInfo> {
        data class ClassInfo(
            val klassPsi: KtFunction,
            val traces: List<PsiStubChildrenCompositionItem>,
        )

        val classInfo = classes
            // We shouldn't delete any parameters from operator fun
            .filterNot { it is KtNamedFunction && readAction { it.hasModifier(KtTokens.OPERATOR_KEYWORD) } }
            // We can't delete any parameter from a value class
            .filterNot { it is KtPrimaryConstructor && readAction { it.containingClass()?.isValue() == true } }
            .map { funPsi ->
                val traces = buildTraceFor(funPsi, context)
                ClassInfo(funPsi, traces)
            }
        logger.debug { "Built ${classInfo.size} traces" }

        return classInfo.flatMap { (funPsi, traces) ->
            readAction {
                funPsi
                    .valueParameters
            }
                .map { property ->
                    option {
                        val stubItem = readAction { PsiUtils.buildDeletablePsiItem(context, property).bind() }
                        IntermediatePsiItemInfo(property, CallablePsiStubDDItem.create(stubItem, traces))
                    }
                }.filterOption()
        }
    }

    // TODO: Add the search scope parameter.
    // Since we have some bug in find files, I don't want to add it now and just use project scope
    private suspend fun buildTraceFor(
        item: KtCallableDeclaration,
        context: IJDDContext,
    ): List<PsiStubChildrenCompositionItem> {
        val methodUsageSearcher = smartReadAction(context.indexProject) {
            MethodUserSearcher(
                item,
                context,
            )
        }
        return buildList {
            smartReadAction(context.indexProject) {
                methodUsageSearcher.buildTaskList { usageInfo ->
                    usageInfo.element?.let {
                        add(
                            PsiUtils.buildCompositeStubItem(
                                context = context,
                                element = it.parent,
                            ),
                        )
                    }
                    true
                }
            }
            methodUsageSearcher.executeTasks()
        }.filterOption()
    }

    private fun PsiElement.isFromContext(context: IJDDContext): Boolean =
        containingFile?.virtualFile?.let {
            ProjectFileIndex.getInstance(context.indexProject).isInProject(it)
        } == true

    private fun List<PsiElement>.process(
        context: IJDDContext,
        psiCache: Map<KtElement, PsiStubDDItem>,
    ) =
        filter { it.isFromContext(context) }
            .mapNotNull(psiCache::get)

    private data class IntermediatePsiItemInfo(
        val psiElement: KtElement,
        val item: PsiStubDDItem,
    ) {
        fun asPair() = psiElement to item
    }
}
