package org.plan.research.minimization.plugin.prototype.slicing

import org.plan.research.minimization.core.algorithm.slicing.SlicingError
import org.plan.research.minimization.plugin.errors.CompilationPropertyCheckerError
import org.plan.research.minimization.plugin.errors.SnapshotError
import org.plan.research.minimization.plugin.execution.IdeaCompilationException
import org.plan.research.minimization.plugin.model.exception.CompilationException

sealed interface SlicingServiceError {
    data class InitialCompilationFailed(val cause: CompilationPropertyCheckerError): SlicingServiceError
    data class InvalidInitialCompilationExceptionType(val actualFqn: String): SlicingServiceError
    data class SlicingFailed(val cause: SlicingError): SlicingServiceError
    data class TransactionFailed<T>(val cause: SnapshotError<T>): SlicingServiceError
    data class SlicingInvalid(val oldException: IdeaCompilationException, val newException: CompilationException?) : SlicingServiceError
}
