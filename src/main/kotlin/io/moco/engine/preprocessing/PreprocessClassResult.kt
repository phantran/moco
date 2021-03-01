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

package io.moco.engine.preprocessing

/**
 * Preprocess result of a class under test
 * An instant of this class contains information that is collected during preprocessing of codebase of a specific class
 * Collected information are mapping of test classes and class under test and the information of blocks
 * Block information is needed in mutation process since finally block are not considered for mutation
 * @property classUnderTestName
 * @property testClasses
 */
data class PreprocessClassResult(
    val classUnderTestName: String,
    val testClasses: MutableSet<String>,
    val coveredLines: MutableSet<Int>?,
)