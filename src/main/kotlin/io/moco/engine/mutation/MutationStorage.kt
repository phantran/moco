package io.moco.engine.mutation

data class MutationStorage(
    var entry: MutableMap<String, MutableList<Pair<Mutation, String>>>
)