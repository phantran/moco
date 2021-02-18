package io.moco.engine.mutation

import com.fasterxml.jackson.annotation.JsonProperty

data class MutationStorage(
    // IMPORTANT: Do not change json property name
    @JsonProperty("entries")
    var entries: MutableMap<String, MutableList<Map<String, Any>>>
)