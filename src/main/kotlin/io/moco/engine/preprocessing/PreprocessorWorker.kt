package io.moco.engine.preprocessing

import io.moco.engine.Codebase
import io.moco.engine.MocoAgent
import io.moco.utils.JsonConverter
import java.net.Socket


object PreprocessorWorker {
    @JvmStatic
    fun main(args: Array<String>) {
        var socket: Socket? = null
        var buildRoot = ""  // path to build or target folder of project
        val preprocessFilename = args[5]  // Check moco entry point for index of these arguments
        try {
            socket = Socket("localhost", args[0].toInt())
            val codeRoot = args[1]  // root of the classes under test folder
            val testRoot = args[2]  // root of the test folder
            val excludedClasses = args[3]  // classes to be excluded, regex or string
            buildRoot = args[4]
            val analysedCodeBase = Codebase(codeRoot, testRoot, excludedClasses)
            MocoAgent.addTransformer(PreprocessorTransformer(analysedCodeBase.sourceClassNames))
            Preprocessor(analysedCodeBase).preprocessing()
        } catch (ex: Exception) {
            ex.printStackTrace(System.out)
        } finally {
            JsonConverter(
                "$buildRoot/moco/preprocess/", preprocessFilename
            ).saveObjectToJson(PreprocessorTracker.getPreprocessResults())
            socket?.close()
        }
    }
}