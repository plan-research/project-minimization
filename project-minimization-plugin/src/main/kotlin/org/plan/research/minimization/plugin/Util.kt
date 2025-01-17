package org.plan.research.minimization.plugin

import org.plan.research.minimization.core.algorithm.dd.ReversedDDAlgorithm
import org.plan.research.minimization.core.algorithm.dd.impl.DDMin
import org.plan.research.minimization.core.algorithm.dd.impl.ProbabilisticDD
import org.plan.research.minimization.core.algorithm.dd.impl.asReversed
import org.plan.research.minimization.core.algorithm.dd.withZeroTesting
import org.plan.research.minimization.plugin.execution.DumbCompiler
import org.plan.research.minimization.plugin.execution.comparable.SimpleExceptionComparator
import org.plan.research.minimization.plugin.execution.comparable.StacktraceExceptionComparator
import org.plan.research.minimization.plugin.execution.gradle.GradleBuildExceptionProvider
import org.plan.research.minimization.plugin.execution.transformer.PathRelativizationTransformation
import org.plan.research.minimization.plugin.logging.withLog
import org.plan.research.minimization.plugin.model.BuildExceptionProvider
import org.plan.research.minimization.plugin.model.context.IJDDContext
import org.plan.research.minimization.plugin.model.exception.CompilationException
import org.plan.research.minimization.plugin.model.exception.ExceptionTransformation
import org.plan.research.minimization.plugin.model.snapshot.SnapshotManager
import org.plan.research.minimization.plugin.model.state.*
import org.plan.research.minimization.plugin.snapshot.ProjectCloningSnapshotManager

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import kotlin.io.path.Path
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object PathSerializer : KSerializer<Path> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Path", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Path) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Path {
        val pathString = decoder.decodeString()
        return Path(pathString)
    }
}

fun SnapshotStrategy.getSnapshotManager(project: Project): SnapshotManager =
    when (this) {
        SnapshotStrategy.PROJECT_CLONING -> ProjectCloningSnapshotManager(project)
    }

fun DDStrategy.getDDAlgorithm(isZeroTesting: Boolean = false): ReversedDDAlgorithm =
    when (this) {
        DDStrategy.DD_MIN -> DDMin()
        DDStrategy.PROBABILISTIC_DD -> ProbabilisticDD()
    }
        .withLog()
        .let { if (isZeroTesting) it.withZeroTesting() else it }
        .asReversed()

fun CompilationStrategy.getCompilationStrategy(): BuildExceptionProvider =
    when (this) {
        CompilationStrategy.GRADLE_IDEA -> GradleBuildExceptionProvider()
        CompilationStrategy.DUMB -> DumbCompiler
    }

fun VirtualFile.getAllNestedElements(): List<VirtualFile> = buildList {
    VfsUtilCore.iterateChildrenRecursively(
        this@getAllNestedElements,
        null,
    ) {
        add(it)
        true
    }
}

fun List<VirtualFile>.getAllParents(root: VirtualFile): List<VirtualFile> = buildSet {
    @Suppress("AVOID_NESTED_FUNCTIONS")
    fun traverseParents(vertex: VirtualFile?) {
        if (vertex == null || contains(vertex) || VfsUtil.isAncestor(vertex, root, false)) {
            return
        }
        add(vertex)
        traverseParents(vertex.parent)
    }
    this@getAllParents.forEach(::traverseParents)
}.toList()

fun ExceptionComparingStrategy.getExceptionComparator() = when (this) {
    ExceptionComparingStrategy.SIMPLE -> SimpleExceptionComparator()
    ExceptionComparingStrategy.STACKTRACE -> StacktraceExceptionComparator(SimpleExceptionComparator())
}

fun TransformationDescriptor.getExceptionTransformations() = when (this) {
    TransformationDescriptor.PATH_RELATIVIZATION -> PathRelativizationTransformation()
}

suspend fun CompilationException.apply(transformations: List<ExceptionTransformation>, context: IJDDContext) =
    transformations.fold(this) { acc, it -> acc.apply(it, context) }

fun getCurrentTimeString(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
