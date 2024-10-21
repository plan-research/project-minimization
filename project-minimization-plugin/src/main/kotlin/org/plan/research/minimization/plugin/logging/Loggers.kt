package org.plan.research.minimization.plugin.logging

import mu.KotlinLogging

object Loggers {
    val statLogger = KotlinLogging.logger("STATISTICS")
    val workingLogger = KotlinLogging.logger("WORKING")
    val generalLogger = KotlinLogging.logger("GENERAL")
}
