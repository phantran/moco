package io.moco.engine

import io.moco.engine.preprocessing.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Whitelist
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

fun getLinesInRange(jacocoSourceFile: File, target: Any, linesAround: Int): String {
    val document = Jsoup.parse(jacocoSourceFile, null)
    val lines: List<String> = document.html().lines()
    val outputSettings = Document.OutputSettings()
    outputSettings.prettyPrint(false)
    document.outputSettings(outputSettings)
    document.select("br").before("\\n")
    document.select("p").before("\\n")

    val lineIndex: Int = when (target) {
        is Int -> {
            if (target < 0) { return "" }
            val line = document.selectFirst("#L$target") ?: return ""
            lines.indexOfFirst { it == line.toString() }
        }
        is String -> { lines.indexOfFirst { it.contains(target) } }
        else ->  return ""
    }

    if (lineIndex < 0) { return "" }

    var res = ""
    val offset = if (linesAround % 2 != 0) 1 else 0
    for (i in (lineIndex - (linesAround/2))..(lineIndex + (linesAround/2 + offset))) {
        val temp = lines.getOrNull(i)
        if (temp != null) {
            res += Jsoup.clean(temp, "", Whitelist.none(), outputSettings) + System.lineSeparator()

        }
    }
    return res
}

fun main() {
//    val buildRoot =
//        "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/target"
//    val codeRoot =
//        "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/target/classes"
//    val testRoot =
//        "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/target/test-classes"
//    val classpath = System.getProperty("java.class.path").split(File.pathSeparatorChar.toString()).toMutableList()
//    val jvm = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
//
//    Configuration.setConfiguration(
//        buildRoot,
//        codeRoot,
//        testRoot,
//        "",
//        classpath,
//        jvm,
//        "preprocess",
//        "moco",
//        "",
//        "",
//        File("")
//    )
//
//    MocoEntryPoint().execute()

    println(PreprocessingFilterByGit.getChangedClassesSinceLastStoredCommit("745079d8060420580651bdb54a7e3c398a640a90",
        "/Users/phantran/Study/Passau/Thesis/TestGamekins/test", "org.example"))
}


//    val url = "jdbc:h2:file:/Users/phantran/Study/Passau/Thesis"
//    val user = "sa"
//    val passwd = "hahaha"
//
//    val abc = H2Database(url, user, passwd)
//
//    val schema: Map<String, String> = mapOf("id" to "int NOT NULL AUTO_INCREMENT",
//                                            "LastName" to "varchar(255)",
//                                            "FirstName" to "varchar(255)")
//    abc.dropTable("Persons")
//
//    abc.createTable("Persons", schema)
//
//    abc.insert("Persons", mapOf("LastName" to "Phan", "FirstName" to "Tran" ))
//    abc.insert("Persons", mapOf("LastName" to "Huynh", "FirstName" to "Nghi" ))
////
////    val temp1 = abc.fetchOne("Persons", "LastName = 'Huynh' or LastName = 'Tran'", listOf("*"))
////    val temp2 = abc.fetchAll("Persons")
//
////    abc.delete("Persons", "PersonID = 2")
//
//    val temp2 = abc.fetchAll("Persons")
////
////    H2Database.printResults(temp1!!)
//    H2Database.printResults(temp2!!)
//
//
//
//    abc.closeConnection()
//


////////..............
//    val x = "huhu"
//    val query2 = "INSERT INTO Persons (PersonID, LastName, Address, City) VALUES ('3', '" + x + "', 'burger', 'munich'); "
//    val query3 = "select * from Persons;"
//
//
//    try {
//        DriverManager.getConnection(url, user, passwd).use { con ->
//
//            con.createStatement().use { st ->
//                st.execute("Drop table if exists Persons")
//
//                st.execute(query1)
//                st.execute(query2)
//                st.execute(query2)
//
//                st.executeQuery(query3).use {   rs ->
//                    while (rs.next()) {
//
//                        println( rs.getInt(1).toString() + " " +
//                                rs.getString(2) +  " "   +
//                                rs.getString(3) +  " "   +
//                                rs.getString(4) +  " "   + rs.getString(5))
//                    }
//                }
//            }
//        }
//    } catch (ex: SQLException) {
//        println(ex.printStackTrace())
//    }


//
//    val a = String(Files.readAllBytes(Paths.get("/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/target/moco/mutation/moco.json")))
//    println(a)
//    val input =
//        File("/Users/phantran/Study/Passau/Thesis/TestGamekins/test/target/site/jacoco/org.example/Main.java.html")
//    val temp = getLinesInRange(input, 3, 5)
//    println(temp)
//}
