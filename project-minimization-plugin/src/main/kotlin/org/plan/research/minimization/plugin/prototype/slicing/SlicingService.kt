package org.plan.research.minimization.plugin.prototype.slicing

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.plan.research.minimization.core.algorithm.slicing.impl.SlicingImpl
import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.execution.IdeaCompilationException
import org.plan.research.minimization.plugin.execution.exception.positionOrNull
import org.plan.research.minimization.plugin.execution.transformer.PathRelativizationTransformation
import org.plan.research.minimization.plugin.getCompilationStrategy
import org.plan.research.minimization.plugin.getExceptionComparator
import org.plan.research.minimization.plugin.lenses.FileDeletingItemLens
import org.plan.research.minimization.plugin.logging.statLogger
import org.plan.research.minimization.plugin.model.BuildExceptionProvider
import org.plan.research.minimization.plugin.model.IJDDContext
import org.plan.research.minimization.plugin.model.ProjectFileDDItem
import org.plan.research.minimization.plugin.model.exception.ExceptionComparator
import org.plan.research.minimization.plugin.model.exception.ExceptionTransformation
import org.plan.research.minimization.plugin.model.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.prototype.slicing.SlicingServiceError.*
import org.plan.research.minimization.plugin.services.RootsManagerService
import org.plan.research.minimization.plugin.services.SnapshotManagerService
import org.plan.research.minimization.plugin.settings.MinimizationPluginSettings
import kotlin.io.path.Path
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText

@Service(Service.Level.APP)
class SlicingService(val cs: CoroutineScope) {
    private val logger = KotlinLogging.logger {}
    fun dumpGraph(context: IJDDContext, file: PsiFile, onComplete: suspend (PsiSlicingNode) -> Unit) = cs.launch {
        val graphConstructor = context.indexProject.service<PsiGraphConstructor>()
        val fileNode = readAction { graphConstructor.initializeGraphNode(context, file) }
            ?: return@launch
        onComplete(fileNode)
    }

    suspend fun sliceProject(context: IJDDContext) = either {
        logger.info { "Starting slicing project ${context.originalProject.name}" }
        val slicingContext = createSlicingContext(context)
        val initialCompilationResult = getInitialException(slicingContext, context).bind()
        val rootsFiles = getRootFiles(initialCompilationResult, context).bind()
        val initialFiles = gettAllFiles(context)
        val initialItems = initialFiles.map {
            ProjectFileDDItem(
                it.virtualFile.toNioPath().relativeTo(context.indexProjectDir.toNioPath())
            )
        }

        val nodes =
            readAction { rootsFiles.mapNotNull { slicingContext.psiGraphConstructor.initializeGraphNode(context, it) } }
        nodes.logDotGraph()
        val slicedGraph = SlicingImpl<PsiSlicingNode>().slice(nodes).getOrElse { raise(SlicingFailed(it)) }
        val slicedPaths = slicedGraph
            .nodes
            .map { node -> node.underlyingItem.localPath } +
                initialFiles.filter { it.fileType != KotlinFileType.INSTANCE }
                    .mapNotNull { it.virtualFile.toNioPathOrNull()?.relativeTo(context.indexProjectDir.toNioPath()) }

        val associatedPsiElements = slicedPaths
            .distinct()
            .map { ProjectFileDDItem(it) }
        logger.info { "There are ${initialFiles.size} kotlin files in the source roots. However, slicing left only ${associatedPsiElements.size} nodes" }
        statLogger.info { "There are ${initialFiles.size} kotlin files in the source roots. However, slicing left only ${associatedPsiElements.size} nodes" }
        val lens = FileDeletingItemLens()
        val transformer = PathRelativizationTransformation()

        slicingContext.snapshotManager.transaction(context.copy(currentLevel = initialItems)) { clonedContext ->
            lens.focusOn(associatedPsiElements, clonedContext)
            val slicedCompilationException = slicingContext.buildExceptionProvider.checkCompilation(clonedContext)
                .getOrElse { raise(SlicingInvalid(initialCompilationResult, null)) }
            if (slicingContext.exceptionComparator.areEquals(
                    initialCompilationResult.apply(transformer, context),
                    slicedCompilationException.apply(transformer, clonedContext)
                )
            ) {
                clonedContext
            } else {
                raise(SlicingInvalid(initialCompilationResult, slicedCompilationException))
            }
        }.getOrElse {
            when (it) {
                is SnapshotError.TransactionCreationFailed, is SnapshotError.TransactionFailed -> raise(
                    TransactionFailed(it)
                )

                is SnapshotError.Aborted -> raise(it.reason)
            }
        }
    }.onLeft {
        logger.error { "Slicing has failed with an error: $it" }
        statLogger.info { "Slicing has failed" }
    }.onRight {
        logger.info { "Slicing has been successfully completed. The result project is in ${it.projectDir.path}" }
        statLogger.info { "Slicing has been successfully completed. The result project is in ${it.projectDir.path}" }
    }


    private suspend fun getInitialException(slicingContext: SlicingContext, ideaContext: IJDDContext) = either {
        val compilationResult = slicingContext
            .buildExceptionProvider
            .checkCompilation(ideaContext)
            .getOrElse { raise(InitialCompilationFailed(it)) }
        ensure(compilationResult is IdeaCompilationException) {
            raise(
                InvalidInitialCompilationExceptionType(
                    compilationResult::class.qualifiedName ?: "null"
                )
            )
        }
        compilationResult
    }

    private suspend fun getRootFiles(
        compilationResult: IdeaCompilationException,
        ideaContext: IJDDContext
    ): Either<SlicingServiceError, List<PsiFile>> = either {

        val errorsPositions = compilationResult
            .kotlincExceptions
            .mapNotNull { it.positionOrNull?.filePath?.relativeTo(ideaContext.projectDir.toNioPath()) }
        logger.debug { "Got ${errorsPositions.size} different positions: $errorsPositions" }

        readAction {
            errorsPositions.mapNotNull {
                ideaContext.indexProjectDir.findFileByRelativePath(it.toString())
                    ?.toPsiFile(ideaContext.indexProject)
            }.distinct()
        }.also {
            logger.debug {
                "Got ${it.size} different files ${
                    it.map {
                        it.virtualFile.toNioPath().relativeTo(ideaContext.indexProjectDir.toNioPath())
                    }
                }"
            }
        }
    }

    private suspend fun gettAllFiles(ideaContext: IJDDContext): List<PsiFile> {
        val rootManager = service<RootsManagerService>()
        val roots = rootManager.findPossibleRoots(ideaContext)
            .mapNotNull { ideaContext.indexProjectDir.findFileByRelativePath(it.toString()) }

        val files = smartReadAction(ideaContext.indexProject) {
            buildSet {
                roots.forEach { root ->
                    add(root)
                    VfsUtilCore.visitChildrenRecursively(root, object : VirtualFileVisitor<Any>() {
                        override fun visitFileEx(file: VirtualFile): Result {
                            add(file)
                            return CONTINUE
                        }
                    })
                }
            }
        }
        return readAction { files.mapNotNull { it.toPsiFile(ideaContext.indexProject) } }
    }

    private fun createSlicingContext(ideaContext: IJDDContext): SlicingContext {
        val settings = ideaContext.originalProject.service<MinimizationPluginSettings>().state
        return SlicingContext(
            buildExceptionProvider = settings.currentCompilationStrategy.getCompilationStrategy(),
            snapshotManager = ideaContext.originalProject.service<SnapshotManagerService>(),
            exceptionComparator = settings.exceptionComparingStrategy.getExceptionComparator(),
            psiGraphConstructor = ideaContext.indexProject.service<PsiGraphConstructor>(),
        )
    }

    private suspend fun List<PsiSlicingNode>.logDotGraph() {
        if (!logger.isDebugEnabled) return

        val dotContent = this.toDot()

        val path = System.getProperty("idea.log.path").takeIf { !it.isNullOrBlank() } ?: return
        withContext(Dispatchers.IO) {
            Path(path).resolve("latest-slicing-graph.dot").writeText(dotContent)
        }
    }

    private data class SlicingContext(
        val buildExceptionProvider: BuildExceptionProvider,
        val snapshotManager: SnapshotManager,
        val exceptionComparator: ExceptionComparator,
        val psiGraphConstructor: PsiGraphConstructor,
    )
}