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

import java.io.File

/**
 * Codebase
 * This class represents target classes to be mutated. Two important attributes of an instance of this class are
 * sourceClassNames and testClassesNames. These two attributes are used to determine which tests to be run on which
 * classes in the preprocessing phase.
 * @constructor
 *
 * @param codeTarget
 * @param testTarget
 * @param codeRoot
 * @param testRoot
 * @param excludedSourceClasses
 * @param excludedSourceFolders
 * @param excludedTestClasses
 * @param excludedTestFolders
 */
class Codebase(
    val codeRoot: String,
    val testRoot: String,
    private val codeTarget: String,
    private val testTarget: String,
    excludedSourceClasses: List<String>,
    excludedSourceFolders: List<String>,
    excludedTestClasses: List<String>,
    excludedTestFolders: List<String>,
) {

    private val codeRoots: List<File> = codeTarget.split(",").map { File(it) }
    private val testRoots: List<File> = testTarget.split(",").map { File(it) }
    val sourceClassNames: MutableSet<ClassName> =
        getAllClassNames(codeRoots, excludedSourceClasses, excludedSourceFolders, false)
    val testClassesNames: MutableSet<ClassName> =
        getAllClassNames(testRoots, excludedTestClasses, excludedTestFolders, true)

    private var curRoot: File = File("")

    /**
     * Get all class names
     * Depends on the argument of roots parameter, This function will collect all classes in the corresponding roots
     * Here we collect all classes in the compiled source classes folder when roots argument is codeRoots and
     * all classes in the compiled test classes folder when roots argument is testRoots
     * @param roots
     * @param filter
     * @param folderFilter
     * @return
     */
    private fun getAllClassNames(
        roots: List<File>, filter: List<String>,
        folderFilter: List<String>, forTest: Boolean
    ): MutableSet<ClassName> {
        curRoot = if (forTest) File(testRoot)
        else File(codeRoot)

        val res: MutableSet<ClassName> = mutableSetOf()
        for (root: File in roots) {

            res.addAll(getClassNames(root, filter, folderFilter))
        }
        return res
    }

    /**
     * Get class names
     * Recursively collect all class names in folders and files of given root
     * @param curRoot
     * @param filter
     * @param folderFilter
     * @return
     */
    private fun getClassNames(curRoot: File, filter: List<String>, folderFilter: List<String>): MutableSet<ClassName> {
        val clsNames: MutableSet<ClassName> = mutableSetOf()
        val listOfFiles = curRoot.listFiles()
        if (listOfFiles != null) {
            for (file in listOfFiles) {
                if (file.isDirectory) {
                    // Recursively call this function to collect all class names
                    clsNames.addAll(getClassNames(file, filter, folderFilter))
                } else if (file.name.endsWith(".class")) {
                    val clsName = fileToClassName(file, filter, folderFilter)
                    if (clsName != null) {
                        clsNames.add(clsName)
                    }
                }
            }
        }
        return clsNames
    }

    /**
     * File to class name
     *
     * Source and test filters that we specify in mojo configuration are used to filter class names here in this
     * method. If Git change mode is ON, it will also be considered in this method.
     *
     * @param file
     * @param filter
     * @param folderFilter
     * @return
     */
    private fun fileToClassName(file: File, filter: List<String>, folderFilter: List<String>): ClassName? {
        if (!(folderFilter.size == 1 && folderFilter[0] == "") &&
            folderFilter.any { file.absolutePath.contains(it) }
        ) {
            return null
        }

        val res = file.absolutePath.substring(
            curRoot.absolutePath.length + 1,
            file.absolutePath.length - ".class".length
        )

        // Remove inner class in compiled class names at the moment
        val indexOfDollarSign = res.indexOf("$", 0)
        // Return class name
        var clsName: String = res
        if (indexOfDollarSign > 0) {
            clsName = res.substring(0, indexOfDollarSign)
        }

        if (filter.contains(clsName)) {
            return null
        }
        return ClassName(clsName)
    }

    override fun toString(): String {
        return "Code roots: ${this.codeRoots}  " +
                "Test roots: ${this.testRoots}  " +
                "Number of Classes: ${this.sourceClassNames.size}  " +
                "Number of Tests  ${this.testClassesNames.size}"
    }

}