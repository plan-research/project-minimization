package org.plan.research.minimization.plugin.settings.data

/**
 * A descriptor for [ExceptionTransformer][org.plan.research.minimization.plugin.compilation.transformer.ExceptionTransformer].
 * Since some of them could require runtime information (such as the project and its services),
 * we can't store it in the settings
 *
 */
enum class TransformationDescriptor {
    PATH_RELATIVIZATION
}
