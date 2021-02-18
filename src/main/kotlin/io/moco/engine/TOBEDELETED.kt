package io.moco.engine

import io.moco.engine.preprocessing.*
import io.moco.utils.JsonConverter
import java.io.File
import java.util.LinkedList
import java.io.IOException

/**
 * Main
 * This file is only used temporarily for testing and debugging purposes
 */

object JavaProcess {
    @Throws(IOException::class, InterruptedException::class)
    fun exec(klass: Class<*>, args: List<String>?): Int {
        val javaHome = System.getProperty("java.home")
        val javaBin = javaHome +
                File.separator + "bin" +
                File.separator + "java"
        val classpath = System.getProperty("java.class.path")
        val abc =
            "/Users/phantran/Study/Passau/WiSe20/SearchBased_Software_Engineering/Exercises/test-suite-minimization-phantran/build/classes/java/test/"
        val abc1 =
            "/Users/phantran/Study/Passau/WiSe20/SearchBased_Software_Engineering/Exercises/test-suite-minimization-phantran/build/classes/java/main/"
        val abc2 =
            "/Users/phantran/Study/Passau/Thesis/TestGamekins/test/target/classes"
        val abc5 =
            "/Users/phantran/Study/Passau/Thesis/TestGamekins/test/target/test-classes"
        val abc100 =
            "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/target/test-classes"
        val abc3 =
            "/Users/phantran/Study/Passau/WiSe20/SearchBased_Software_Engineering/Exercises/test-suite-minimization-phantran/build/classes/java/"
        val className = klass.name
        val command: MutableList<String> = LinkedList()

        val abcd = "-javaagent:MyJar.jar"
//        val abcd = "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/MyJar.jar"
        command.add(javaBin)
        command.add(abcd)

        command.add("-cp")
        command.add("$classpath:$abc:$abc1:$abc2:$abc3:$abc5:$abc100")
        command.add(className)

        if (args != null) {
            command.addAll(args)
        }
        val builder = ProcessBuilder(command)
        val process = builder.inheritIO().start()

        process.waitFor()
        return process.exitValue()
    }
}

object ABC {
    @JvmStatic
    fun main(args: Array<String>) {

        val root =
            "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/"
        val codeRoot =
            "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/target/classes"
        val testRoot =
            "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/target/test-classes"

        val temp = Codebase(root, testRoot, "")
        MocoAgent.addTransformer(PreprocessorTransformer(temp.sourceClassNames))


        val beanClass = Class.forName("io.moco.TestTest1")
        val beanClass1 = Class.forName("io.moco.TestTest2")


        val junit = org.junit.runner.JUnitCore()
        junit.addListener(org.junit.internal.TextListener(System.out))


        val result: org.junit.runner.Result = junit.run(beanClass)
        junit.run(beanClass1)

//        PreprocessorTracker.registerMappingTestToCUT(beanClass)

        for (failure in result.failures) {
            println("$failure")
        }
        println(
            "passed:" + result.wasSuccessful() +
                    PreprocessorTracker.cutRecord + "\n" +
                    PreprocessorTracker.testToCUTTracker + "\n" +
                    PreprocessorTracker.blockTracker
        )
    }
}

fun main() {
//    val status = JavaProcess.exec(ABC::class.java, null)
    val buildRoot =
        "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/target"
    val codeRoot =
        "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/target/classes"
    val testRoot =
        "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/target/test-classes"
    val classpath = System.getProperty("java.class.path").split(File.pathSeparatorChar.toString()).toMutableList()
    val jvm = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"

    Configuration.setConfiguration(
        buildRoot,
        codeRoot,
        testRoot,
        "",
        classpath,
        jvm,
        "preprocess",
        "moco",
        "",
        "",
    )

    MocoEntryPoint().execute()

    val abc = JsonConverter("$buildRoot/moco/preprocess/", "preprocess").retrieveObjectFromJson()
    print(abc)


}
