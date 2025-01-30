package org.plan.research.minimization.plugin.headless

import com.github.ajalt.clikt.command.SuspendingCliktCommand

class MinimizeProjectApplicationStarter : AbstractApplicationStarterCliktAdapter() {
    override val cliktRunner: SuspendingCliktCommand = MinimizeProjectCliCommand()
}
