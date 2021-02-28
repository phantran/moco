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

package io.moco.engine

import io.moco.utils.MoCoLogger


data class Configuration(
    val buildRoot: String,
    val codeRoot: String,
    val testRoot: String,
    val excludedSourceClasses : String,
    val excludedSourceFolders : String,
    val excludedTestClasses : String,
    val excludedTestFolders : String,
    val classPath: List<String>,
    val jvm: String,
    val preprocessResultFileName: String,
    val mutationResultsFileName: String,
    val excludedMutationOperatorNames: String,
    val baseDir: String,
    val compileSourceRoots: List<String>?,
    val artifactId: String,
    val gitMode: Boolean,
    val testTimeOut: String,
    val mutationPerClass: Int,
    val debugEnabled: Boolean
) {
    companion object {
        var currentConfig: Configuration? = null
    }

    fun getPreprocessProcessArgs(): MutableList<String> {
        return mutableListOf(buildRoot, codeRoot, testRoot, excludedSourceClasses, excludedSourceFolders,
            excludedTestClasses, excludedTestFolders, preprocessResultFileName,
            testTimeOut, debugEnabled.toString()
        )
    }
}