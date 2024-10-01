package org.plan.research.minimization.plugin.model.snapshot

import arrow.core.raise.Raise

class TransactionBody<T>(
    raise: Raise<T>,
    translator: TransactionTranslator
) : Raise<T> by raise, TransactionTranslator by translator
