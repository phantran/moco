package io.moco.engine.mutation

import java.io.Serializable

data class MutationTestResult(
    val numberOfTestsRun: Int,
    val mutationTestStatus: MutationTestStatus
) : Serializable {
}