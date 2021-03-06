/*
 * Copyright (c) 2021. Tran Phan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package io.moco.engine.tracker

import io.moco.engine.ClassInfo
import io.moco.engine.mutation.Mutation
import io.moco.engine.mutation.MutationID


class MutatedClassTracker(val targetMutation: Mutation? = null, val coveredLines: Set<Int>? = null) {
    private var mutations: MutableList<Mutation> = mutableListOf()
    private var generatedTargetMutation: Mutation? = null
    private var clsInfo: ClassInfo? = null
    private var fileName: String? = null

    fun addMutation(mutation: Mutation) {
        mutations.add(mutation)
    }

    fun mutationAlreadyCollected(id: MutationID): Boolean {
        return mutations.any { it.mutationID == id }
    }

    fun getCollectedMutations(): List<Mutation> {
        return mutations
    }

    fun setGeneratedTargetMutation(mutation: Mutation) {
        generatedTargetMutation = mutation
    }

    fun getGeneratedTargetMutation(): Mutation? {
        return generatedTargetMutation
    }

    fun setClsInfo(info: ClassInfo) {
        clsInfo = info
    }

    fun getClsInfo(): ClassInfo? {
        return clsInfo
    }

    fun setFileName(name: String) {
        fileName = name
    }

    fun getFileName(): String? {
        return fileName
    }

}
