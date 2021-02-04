package io.moco.engine.preprocessing

import io.moco.engine.ClassName
import java.io.File

class Codebase(
    val root: File,
    val testRoot: File,
    val codeRoot: File,
) {
    val sourceClassNames: MutableList<ClassName?> = getClassNames(codeRoot, true)
    val testClassesNames: MutableList<ClassName?> = getClassNames(testRoot, false)

    private fun getClassNames(curRoot: File, forSource: Boolean): MutableList<ClassName?> {
        val clsNames: MutableList<ClassName?> = mutableListOf()
        val listOfFiles = curRoot.listFiles()
        if (listOfFiles != null) {
            for (file in listOfFiles) {
                if (file.isDirectory) {
                    // Recursively call this function to collect all class names
                    clsNames.addAll(getClassNames(file, forSource))
                } else if (file.name.endsWith(".class")) {
                    clsNames.add(fileToClassName(file, forSource))
                }
            }
        }
        return clsNames
    }

    private fun fileToClassName(file: File, forSource: Boolean): ClassName {
        val root = if (forSource) this.codeRoot else this.testRoot
        val res = file.absolutePath.substring(
            root.absolutePath.length + 1,
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