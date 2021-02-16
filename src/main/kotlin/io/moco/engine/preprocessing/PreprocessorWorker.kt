package io.moco.engine.preprocessing

import io.moco.engine.Codebase
import io.moco.engine.MocoAgent
import java.net.Socket


object PreprocessorWorker {
    @JvmStatic
    fun main(args: Array<String>) {
        var socket: Socket? = null
        var buildRoot = ""  // path to build or target folder of project
        try {
            socket = Socket("localhost", args[0].toInt())
            val codeRoot = args[1]  // root of the classes under test folder
            val testRoot = args[2]  // root of the test folder
            val excludedClasses = args[3]  // classes to be excluded, regex or string
            buildRoot  = args[4]
            val analysedCodeBase = Codebase(codeRoot, testRoot, excludedClasses)
            MocoAgent.addTransformer(PreprocessorTransformer(analysedCodeBase.sourceClassNames))
            val worker = Preprocessor(analysedCodeBase)
            worker.preprocessing()
        } catch (ex: Exception) {
            ex.printStackTrace(System.out)
        } finally {
            PreprocessConverter(buildRoot).savePreprocessResult(PreprocessorTracker.getPreprocessResults())
            println("------------------Complete preprocessing step------------------")
            socket?.close()
        }
    }
}