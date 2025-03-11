package org.plan.research.minimization.plugin.services

import org.plan.research.minimization.plugin.context.IJDDContext
import org.plan.research.minimization.plugin.modification.graph.InstanceLevelGraph
import org.plan.research.minimization.plugin.modification.graph.PsiIJEdge
import org.plan.research.minimization.plugin.modification.item.PsiChildrenIndexDDItem
import org.plan.research.minimization.plugin.modification.item.PsiStubChildrenCompositionItem
import org.plan.research.minimization.plugin.modification.item.PsiStubDDItem
import org.plan.research.minimization.plugin.modification.item.PsiStubDDItem.CallablePsiStubDDItem
import org.plan.research.minimization.plugin.modification.psi.KotlinElementLookup
import org.plan.research.minimization.plugin.modification.psi.KotlinOverriddenElementsGetter
import org.plan.research.minimization.plugin.modification.psi.PsiUtils
import org.plan.research.minimization.plugin.modification.psi.usages.MethodUserSearcher

import arrow.core.filterOption
import arrow.core.raise.option
import com.intellij.ide.highlighter.JavaFileType
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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jgrapht.graph.DirectedPseudograph

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
            val javaPsi = findPsiInJavaFiles(context, PsiChildrenIndexDDItem.JAVA_BODY_REPLACEABLE_PSI_JAVA_CLASSES)
            val kotlinPsi = findPsiInKotlinFiles(context, PsiChildrenIndexDDItem.BODY_REPLACEABLE_PSI_JAVA_CLASSES)

            val result = (kotlinPsi + javaPsi)
                .filter { PsiChildrenIndexDDItem.isCompatible(it) }
                .mapNotNull { PsiUtils.buildReplaceablePsiItem(context, it) }

            result
        }

    /**
     * Finds all deletable PSI items in Kotlin files within the given context.
     * The deletable elements are the elements that
     *  * are one of the [PsiStubDDItem.DELETABLE_PSI_JAVA_CLASSES] classes
     *  * on the path from [org.jetbrains.kotlin.psi.KtFile] to that element there are only serializable elements.
     *  That means that they could be represented via [org.plan.research.minimization.plugin.modification.psi.stub.KtStub]
     *
     * @param context The context for the minimization process containing the current project and relevant properties.
     * @param withFunctionParameters
     * @return A list of deletable PSI items found in the Kotlin files of the project.
     */
    suspend fun findDeletablePsiItems(
        context: IJDDContext,
        withFunctionParameters: Boolean = false,
    ): List<PsiStubDDItem> =
        findDeletablePsiItemsWithoutCompression(context, withFunctionParameters).map { it.item }

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

    /**
     * Builds an instance-level adapters representing the deletable PSI (Program Structure Interface) elements
     * and their relationships within the given context.
     *
     * @param context The minimization context containing information about the current project and relevant properties.
     * @param withFunctionParameters If set to `true` then the constructor and function parameters will be included in the adapters
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
    fun findAllKotlinFilesInIndexProject(context: IJDDContext): List<VirtualFile> =
        findAllSourceFilesInIndexProject(context, setOf(KotlinFileType.EXTENSION))

    /**
     * Finds all source files within the index project that match any of the provided file extensions.
     *
     * @param context The context representing the minimization process.
     * @param exts A set of file extensions to filter the source files (e.g., "kt", "java").
     * @return A list of source files from the index project that match the specified file extensions.
     */
    @RequiresReadLock
    fun findAllSourceFilesInIndexProject(context: IJDDContext, exts: Set<String>): List<VirtualFile> {
        val roots = service<RootsManagerService>().findPossibleRoots(context)
        logger.debug {
            "Found ${roots.size} roots: $roots"
        }
        val rootFiles = roots.mapNotNull {
            context.indexProjectDir.findFileByRelativePath(it.pathString)
        }
        return myVirtualFileTraverse(rootFiles, exts)
    }

    @RequiresReadLock
    private fun myVirtualFileTraverse(roots: List<VirtualFile>, exts: Set<String>): List<VirtualFile> =
        buildSet<VirtualFile> {
            roots.forEach { root ->
                VfsUtilCore.visitChildrenRecursively(root, object : VirtualFileVisitor<Unit>() {
                    override fun visitFileEx(file: VirtualFile): Result {
                        if (file.isDirectory) {
                            return CONTINUE
                        }
                        if (exts.contains(file.extension)) {
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
        return extractPsi(context, kotlinFiles, classes)
    }

    @RequiresReadLock
    fun <T : PsiElement> findPsiInJavaFiles(
        context: IJDDContext,
        classes: List<Class<out T>>,
    ): List<T> {
        val javaFiles = findAllSourceFilesInIndexProject(context, setOf(JavaFileType.DEFAULT_EXTENSION))
        logger.debug {
            "Found ${javaFiles.size} java files"
        }

        return extractPsi(context, javaFiles, classes)
    }

    @RequiresReadLock
    private fun <T : PsiElement> extractPsi(
        context: IJDDContext,
        files: Collection<VirtualFile>,
        classes: List<Class<out T>>,
    ): List<T> =
        files.flatMap { file ->
            val relativePath = file.toNioPath().relativeTo(context.indexProjectDir.toNioPath())
            val fileInCurrentProject = context.projectDir.findFileByRelativePath(relativePath.pathString)
                ?: return@flatMap emptyList()

            // TODO: Is it important to check specific file type here? e.g. KtFile
            val psiFileInCurrentProject = PsiUtils.getPsiFile(context, fileInCurrentProject)
                ?: return@flatMap emptyList()

            classes.flatMap { clazz ->
                PsiTreeUtil.collectElementsOfType(psiFileInCurrentProject, clazz)
                    .also {
                        logger.trace {
                            "Found ${it.size} ${clazz.simpleName} elements in $relativePath"
                        }
                    }
            }
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
