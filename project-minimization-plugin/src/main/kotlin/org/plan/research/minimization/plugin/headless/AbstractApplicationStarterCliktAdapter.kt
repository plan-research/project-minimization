package org.plan.research.minimization.plugin.headless

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.command.main
import com.intellij.openapi.application.ModernApplicationStarter

abstract class AbstractApplicationStarterCliktAdapter : ModernApplicationStarter() {
    abstract val cliktRunner: SuspendingCliktCommand
    final override suspend fun start(args: List<String>) = cliktRunner.main(args.drop(1))
}
