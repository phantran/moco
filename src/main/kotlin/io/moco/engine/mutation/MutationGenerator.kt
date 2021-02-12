package io.moco.engine.mutation

import io.moco.engine.ClassName
import io.moco.engine.Codebase

class MutationGenerator(private val codeBase: Codebase, private val mutationFinder: MutationFinder) {
    fun codeBaseMutationAnalyze(): Map<ClassName, List<Mutant>> {
        return this.codeBase.sourceClassNames.associateWith { mutationFinder.findPossibleMutationsOfClass(it) }
    }
}