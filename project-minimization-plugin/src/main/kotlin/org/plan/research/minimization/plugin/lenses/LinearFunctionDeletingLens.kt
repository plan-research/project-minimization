package org.plan.research.minimization.plugin.lenses

import org.plan.research.minimization.plugin.model.context.WithImportRefCounterContext

class LinearFunctionDeletingLens<C : WithImportRefCounterContext<C>> : FunctionDeletingLens<C>()
