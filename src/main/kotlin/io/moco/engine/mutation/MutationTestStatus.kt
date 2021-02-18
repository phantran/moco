package io.moco.engine.mutation


enum class MutationTestStatus {
    KILLED,
    SURVIVED,
    TIMED_OUT,
    NOT_STARTED,
    STARTED,
    RUN_ERROR;
}