package io.moco.engine.test

data class TestResult(
    val desc: Description,
    val error: Throwable?,
    val state: TestState
) {
    enum class TestState { RUNNING, FINISHED, NOT_STARTED, TIMEOUT }
}