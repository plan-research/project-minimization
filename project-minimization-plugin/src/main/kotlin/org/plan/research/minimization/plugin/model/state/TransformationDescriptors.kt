package org.plan.research.minimization.plugin.model.state

/**
 * A descriptor for [ExceptionTransformer][org.plan.research.minimization.plugin.model.exception.ExceptionTransformation].
 * Since some of them could require runtime information (such as the project and its services),
 * we can't store it in the settings
 *
 */
enum class TransformationDescriptors {
    PATH_RELATIVIZATION
}
