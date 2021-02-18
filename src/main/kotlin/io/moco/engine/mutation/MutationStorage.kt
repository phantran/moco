package io.moco.engine.mutation

data class MutationStorage(
    var entries: MutableMap<String, MutableList<Map<String, Any>>>
)