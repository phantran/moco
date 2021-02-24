package io.moco.engine

import java.io.File

class Codebase(
    codePath: String,
    testPath: String,
    excludedSourceClasses: List<String>,
    excludedSourceFolders: List<String>,
    excludedTestClasses: List<String>,
    excludedTestFolders: List<String>,
    filterByGitCommit: List<String>?
) {

    private val codeRoots: List<File> = codePath.split(",").map { File(it) }
    private val testRoots: List<File> = testPath.split(",").map { File(it) }
    val sourceClassNames: MutableList<ClassName> =
        getAllClassNames(codeRoots, excludedSourceClasses, excludedSourceFolders)
    val testClassesNames: MutableList<ClassName> =
        getAllClassNames(testRoots, excludedTestClasses, excludedTestFolders, filterByGitCommit)

    private var curRoot: File = File("")

    private fun getAllClassNames(
        roots: List<File>, filter: List<String>,
        folderFilter: List<String>, filterByGitCommit: List<String>? = null
    ): MutableList<ClassName> {
        val res: MutableList<ClassName> = mutableListOf()
        for (root: File in roots) {
            curRoot = root
            res.addAll(getClassNames(root, filter, folderFilter, filterByGitCommit))
        }
        return res
    }

    private fun getClassNames(
        curRoot: File, filter: List<String>,
        folderFilter: List<String>, filterByGitCommit: List<String>? = null
    ): MutableList<ClassName> {
        val clsNames: MutableList<ClassName> = mutableListOf()
        val listOfFiles = curRoot.listFiles()
        if (listOfFiles != null) {
            for (file in listOfFiles) {
                if (file.isDirectory) {
                    // Recursively call this function to collect all class names
                    clsNames.addAll(getClassNames(file, filter, folderFilter, filterByGitCommit))
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

    private fun fileToClassName(
        file: File, filter: List<String>,
        folderFilter: List<String>, filterByGitCommit: List<String>? = null
    ): ClassName? {
        if (folderFilter.any { file.absolutePath.contains(it) }) {
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
        if (filterByGitCommit != null && !filterByGitCommit.contains(clsName))  {
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