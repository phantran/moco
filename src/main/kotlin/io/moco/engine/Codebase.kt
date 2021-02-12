package io.moco.engine

import java.io.File

class Codebase(
    //TODO: implement filtering by using excludedClasses
    codePath: String,
    testPath: String,
    val excludedClasses: String="",  // comma separated classes names to be excluded
) {
    private val codeRoots: List<File> = codePath.split(",").map { File(it) }
    private val testRoots: List<File> = testPath.split(",").map { File(it) }
    val sourceClassNames: MutableList<ClassName> = getAllClassNames(codeRoots)
    val testClassesNames: MutableList<ClassName> = getAllClassNames(testRoots)
    private var curRoot: File = File("")

    private fun getAllClassNames(roots: List<File>): MutableList<ClassName> {
        val res: MutableList<ClassName> = mutableListOf()
        for (root: File in roots) {
            curRoot = root
            res.addAll(getClassNames(root))
        }
        return res
    }

    private fun getClassNames(curRoot: File): MutableList<ClassName> {
        val clsNames: MutableList<ClassName> = mutableListOf()
        val listOfFiles = curRoot.listFiles()
        if (listOfFiles != null) {
            for (file in listOfFiles) {
                if (file.isDirectory) {
                    // Recursively call this function to collect all class names
                    clsNames.addAll(getClassNames(file))
                } else if (file.name.endsWith(".class")) {
                    clsNames.add(fileToClassName(file))
                }
            }
        }
        return clsNames
    }

    private fun fileToClassName(file: File): ClassName {
        val res = file.absolutePath.substring(
            curRoot.absolutePath.length + 1,
            file.absolutePath.length - ".class".length
        )
        // Remove inner class in compiled class names at the moment
        val indexOfDollarSign = res.indexOf("$", 0)
        // Return class name
        return if (indexOfDollarSign > 0) {
            ClassName(res.substring(0, indexOfDollarSign))
        } else {
            ClassName(res)
        }
    }
}