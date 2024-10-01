package org.plan.research.minimization.plugin.model

enum class CompilationStrategy {
    DUMB, // Return the same exception every time. Used for testing
    GRADLE_IDEA // Use IJ to run a gradle build task and get an error
}