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


data class Configuration(
    val rootProjectBaseDir: String,
    val mavenSession: String,
    val buildRoot: String,
    val codeRoot: String,
    val testRoot: String,
    val mocoBuildPath: String,
    val excludedSourceClasses : String,
    val excludedSourceFolders : String,
    val excludedTestClasses : String,
    val excludedTestFolders : String,
    val classPath: String,
    val jvm: String,
    val preprocessResultsFolder: String,
    val mutationResultsFolder: String,
    val excludedMuOpNames: String,
    val fOpNames: List<String>,
    val baseDir: String,
    val compileSourceRoots: List<String>?,
    val groupId: String,
    val artifactId: String,
    val gitMode: Boolean,
    val preprocessTestTimeout: String,
    val mutationPerClass: Int,
    val limitMutantsByType: Boolean,
    val debugEnabled: Boolean,
    val verbose: Boolean,
    val numberOfThreads: Int,
    val noLogAtAll: Boolean = false,
    val enableMetrics: Boolean = false,
    val useForCICD: Boolean = true,
    val mocoPluginVersion: String?
) {
    companion object {
        var currentConfig: Configuration? = null
    }

    fun getPreprocessProcessArgs(): MutableList<String> {
        return mutableListOf(mocoBuildPath, codeRoot, testRoot, excludedSourceClasses, excludedSourceFolders,
            excludedTestClasses, excludedTestFolders, preprocessResultsFolder,
            preprocessTestTimeout, debugEnabled.toString(), verbose.toString(), noLogAtAll.toString()
        )
    }
}