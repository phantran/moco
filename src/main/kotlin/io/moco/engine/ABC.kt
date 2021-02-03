package io.moco.engine


import com.sun.tools.attach.VirtualMachine
import io.moco.engine.io.BytecodeLoader
import io.moco.engine.preprocessing.Codebase
import io.moco.engine.preprocessing.Preprocessor
import io.moco.engine.preprocessing.PreprocessorTracker
import io.moco.engine.test.TestItem
import io.moco.engine.test.TestItemWrapper
import io.moco.engine.test.TestResultAggregator
import io.moco.utils.ClassLoaderUtil
import org.junit.Test
import java.io.File
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors.toList
import java.net.URLClassLoader
import java.util.LinkedList

import java.io.IOException
import java.lang.management.ManagementFactory


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
        val abc3 =
            "/Users/phantran/Study/Passau/WiSe20/SearchBased_Software_Engineering/Exercises/test-suite-minimization-phantran/build/classes/java/"
        val className = klass.name
        val command: MutableList<String> = LinkedList()

        val abcd = "-javaagent:MyJar.jar"

        command.add(javaBin)
        command.add(abcd)

        command.add("-cp")
        command.add("$classpath:$abc:$abc1:$abc2:$abc3:$abc5")
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
//        print(System.getProperty("java.class.path"))


//        val classLoaderUrls: Array<URL> =
//            arrayOf(URL("file:///Users/phantran/Study/Passau/WiSe20/SearchBased_Software_Engineering/Exercises/test-suite-minimization-phantran/build/classes/java/test/"))
////    val classLoaderUrls1: Array<URL> = arrayOf(URL("file:///Users/phantran/Study/Passau/WiSe20/SearchBased_Software_Engineering/Exercises/test-suite-minimization-phantran/build/classes/java/main/"))
////    classLoaderUrls.addAll(classLoaderUrls1)
//        // Create a new URLClassLoader
//
//        // Create a new URLClassLoader
//        val urlClassLoader = URLClassLoader(classLoaderUrls)
//
//        // Load the target class
//
//        // Load the target class
//        val beanClass = urlClassLoader.loadClass("de.uni_passau.fim.se2.testsuite_minimization.DummyTest")

//        val nameOfRunningVM: String = ManagementFactory.getRuntimeMXBean().getName()
//        val pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'))
//        val vm: VirtualMachine = VirtualMachine.attach(pid)
//        vm.loadAgent("MyJar.jar", "")
//        vm.detach()


        val beanClass = ClassName.clsNameToClass(ClassName("org.example.HihiTest"),
            ClassLoaderUtil.contextClsLoader)

        if (beanClass != null) {
            val haha = TestItem(beanClass)
            val hehe = TestItemWrapper(haha, TestResultAggregator(mutableListOf()))
            hehe.call()
        } else {
            println("huhuhu")
        }
    }
}


fun main() {
    val status = JavaProcess.exec(ABC::class.java, null)
    println(status)
}


////private fun prompt(msg: String): String {
////    print("$msg => ")
////    return readLine() ?: ""
////}


////fun main(args: Array<String>) {
//////    val root =
//////        File("/Users/phantran/Study/Passau/WiSe20/SearchBased_Software_Engineering/Exercises/sbse-test-prioritisation-phantran")
//////    val codeRoot =
//////        File("/Users/phantran/Study/Passau/WiSe20/SearchBased_Software_Engineering/Exercises/sbse-test-prioritisation-phantran/build/classes/java/main")
//////    val testRoot =
//////        File("/Users/phantran/Study/Passau/WiSe20/SearchBased_Software_Engineering/Exercises/sbse-test-prioritisation-phantran/build/classes/java/test")
//////
//////    val temp = Codebase(root, testRoot, codeRoot)
//////    val temp1 = Preprocessor(File(""), temp, null)
//////    temp1.preprocessing()
//////    print(PreprocessorTracker.testToCUTTracker)
//////    print(PreprocessorTracker.cutRecord)
////
////
////    val classLoaderUrls: Array<URL> = arrayOf(URL("file:///Users/phantran/Study/Passau/WiSe20/SearchBased_Software_Engineering/Exercises/test-suite-minimization-phantran/build/classes/java/test/"))
//////    val classLoaderUrls1: Array<URL> = arrayOf(URL("file:///Users/phantran/Study/Passau/WiSe20/SearchBased_Software_Engineering/Exercises/test-suite-minimization-phantran/build/classes/java/main/"))
//////    classLoaderUrls.addAll(classLoaderUrls1)
////    // Create a new URLClassLoader
////
////    // Create a new URLClassLoader
////    val urlClassLoader = URLClassLoader(classLoaderUrls)
////
////    // Load the target class
////
////    // Load the target class
////    val beanClass = urlClassLoader.loadClass("de.uni_passau.fim.se2.testsuite_minimization.DummyTest")
////    print(beanClass.name)
////    print(System.getProperty("java.class.path"))
////
////}

