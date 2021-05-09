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

package io.github.phantran.dev.main

import io.github.phantran.engine.Codebase
import io.github.phantran.engine.Configuration
import io.github.phantran.engine.MoCoAgent
import io.github.phantran.engine.MoCoEntryPoint
import io.github.phantran.engine.operator.Operator
import io.github.phantran.engine.preprocessing.PreprocessorTracker
import io.github.phantran.engine.preprocessing.PreprocessorTransformer
import io.github.phantran.persistence.H2Database
import io.github.phantran.utils.JavaInfo
import io.github.phantran.utils.MoCoLogger
import org.apache.maven.model.Dependency
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.apache.maven.model.io.xpp3.MavenXpp3Writer
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Whitelist
import org.objectweb.asm.*
import org.objectweb.asm.util.CheckClassAdapter
import org.objectweb.asm.util.TraceClassVisitor
import java.io.*
import java.util.*


class DefaultClassVisitor : ClassVisitor(JavaInfo.ASM_VERSION) {
    class DefaultAnnotationVisitor internal constructor() : AnnotationVisitor(JavaInfo.ASM_VERSION) {
        override fun visit(arg0: String?, arg1: Any) {}
        override fun visitAnnotation(
            arg0: String, arg1: String
        ): AnnotationVisitor {
            return DefaultAnnotationVisitor()
        }

        override fun visitArray(arg0: String): AnnotationVisitor {
            return DefaultAnnotationVisitor()
        }

        override fun visitEnd() {}
        override fun visitEnum(
            arg0: String, arg1: String,
            arg2: String
        ) {
        }
    }

    class DefaultMethodVisitor internal constructor() : MethodVisitor(JavaInfo.ASM_VERSION) {
        override fun visitAnnotation(
            arg0: String,
            arg1: Boolean
        ): AnnotationVisitor {
            return DefaultAnnotationVisitor()
        }

        override fun visitAnnotationDefault(): AnnotationVisitor {
            return DefaultAnnotationVisitor()
        }

        override fun visitAttribute(arg0: Attribute) {}
        override fun visitCode() {}
        override fun visitEnd() {}
        override fun visitFieldInsn(
            arg0: Int, arg1: String,
            arg2: String, arg3: String
        ) {
        }

        override fun visitFrame(
            arg0: Int, arg1: Int, arg2: Array<Any>,
            arg3: Int, arg4: Array<Any>
        ) {
        }

        override fun visitIincInsn(arg0: Int, arg1: Int) {}
        override fun visitInsn(arg0: Int) {}
        override fun visitIntInsn(arg0: Int, arg1: Int) {}
        override fun visitJumpInsn(arg0: Int, arg1: Label) {}
        override fun visitLabel(arg0: Label) {}
        override fun visitLdcInsn(arg0: Any) {}
        override fun visitLineNumber(arg0: Int, arg1: Label) {}
        override fun visitLocalVariable(
            arg0: String, arg1: String,
            arg2: String?, arg3: Label, arg4: Label, arg5: Int
        ) {
        }

        override fun visitLookupSwitchInsn(
            arg0: Label, arg1: IntArray,
            arg2: Array<Label>
        ) {
        }

        override fun visitMaxs(arg0: Int, arg1: Int) {}
        override fun visitMethodInsn(
            arg0: Int, arg1: String,
            arg2: String, arg3: String
        ) {
        }

        override fun visitMultiANewArrayInsn(arg0: String, arg1: Int) {}
        override fun visitParameterAnnotation(
            arg0: Int,
            arg1: String, arg2: Boolean
        ): AnnotationVisitor {
            return DefaultAnnotationVisitor()
        }

        override fun visitTableSwitchInsn(
            arg0: Int, arg1: Int,
            arg2: Label, vararg labels: Label
        ) {
        }

        override fun visitTryCatchBlock(
            arg0: Label, arg1: Label,
            arg2: Label, arg3: String?
        ) {
        }

        override fun visitTypeInsn(arg0: Int, arg1: String) {}
        override fun visitVarInsn(arg0: Int, arg1: Int) {}
    }

    override fun visit(
        arg0: Int, arg1: Int, arg2: String,
        arg3: String?, arg4: String, arg5: Array<String>
    ) {
    }

    override fun visitAnnotation(arg0: String, arg1: Boolean): AnnotationVisitor {
        return DefaultAnnotationVisitor()
    }

    override fun visitAttribute(arg0: Attribute) {}
    override fun visitEnd() {}
    override fun visitField(
        arg0: Int, arg1: String,
        arg2: String, arg3: String?, arg4: Any?
    ): FieldVisitor {
        return object : FieldVisitor(JavaInfo.ASM_VERSION) {
            override fun visitAnnotation(
                arg0: String,
                arg1: Boolean
            ): AnnotationVisitor {
                return DefaultAnnotationVisitor()
            }

            override fun visitAttribute(arg0: Attribute) {}
            override fun visitEnd() {}
        }
    }

    override fun visitInnerClass(
        arg0: String, arg1: String,
        arg2: String, arg3: Int
    ) {
    }

    override fun visitMethod(
        arg0: Int, arg1: String,
        arg2: String, arg3: String?, arg4: Array<String>?
    ): MethodVisitor {
        return DefaultMethodVisitor()
    }

    override fun visitOuterClass(
        arg0: String, arg1: String,
        arg2: String
    ) {
    }

    override fun visitSource(arg0: String, arg1: String?) {}
}

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

object DEV {
    @JvmStatic
    fun main(args: Array<String>) {

        val root =
            "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/"
        val testRoot =
            "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/target/test-classes"

        val temp = Codebase(root, testRoot, root, testRoot, listOf(), listOf(), listOf(), listOf())
        MoCoAgent.addTransformer(PreprocessorTransformer(temp.sourceClassNames))

        val beanClass = Class.forName("io.github.phantran.TestTest1")
        val beanClass1 = Class.forName("io.github.phantran.TestTest2")


        val junit = org.junit.runner.JUnitCore()
        junit.addListener(org.junit.internal.TextListener(System.out))


        val result: org.junit.runner.Result = junit.run(beanClass)
        junit.run(beanClass1)

        for (failure in result.failures) {
            println("$failure")
        }
        println(
            "passed:" + result.wasSuccessful() +
                    PreprocessorTracker.cutRecord + "\n" +
                    PreprocessorTracker.cutToLineTestMap + "\n" +
                    PreprocessorTracker.cutToTests + "\n" +
                    PreprocessorTracker.lineTracker
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
            if (target < 0) {
                return ""
            }
            val line = document.selectFirst("#L$target") ?: return ""
            lines.indexOfFirst { it == line.toString() }
        }
        is String -> {
            lines.indexOfFirst { it.contains(target) }
        }
        else -> return ""
    }

    if (lineIndex < 0) {
        return ""
    }

    var res = ""
    val offset = if (linesAround % 2 != 0) 1 else 0
    for (i in (lineIndex - (linesAround / 2))..(lineIndex + (linesAround / 2 + offset))) {
        val temp = lines.getOrNull(i)
        if (temp != null) {
            res += Jsoup.clean(temp, "", Whitelist.none(), outputSettings) + System.lineSeparator()

        }
    }
    return res
}

private fun getPrevHash(repo: Repository, commit: RevCommit): RevCommit? {
    val walk = RevWalk(repo)

    walk.markStart(commit)
    for ((count, rev) in walk.withIndex()) {
        if (count == 1) {
            return rev
        }
    }
    walk.dispose()

    return null
}

private fun getCanonicalTreeParser(git: Git, commitId: ObjectId): AbstractTreeIterator {
    val walk = RevWalk(git.repository)
    val commit = walk.parseCommit(commitId)
    val treeId = commit.tree.id
    val reader = git.repository.newObjectReader()
    return CanonicalTreeParser(null, reader, treeId)
}

private fun getDiffOfCommit(git: Git, repo: Repository, newCommit: RevCommit): String {
    val oldCommit = getPrevHash(repo, newCommit)
    val oldTreeIterator = if (oldCommit == null) EmptyTreeIterator() else getCanonicalTreeParser(git, oldCommit)
    val newTreeIterator = getCanonicalTreeParser(git, newCommit)
    val outputStream: OutputStream = ByteArrayOutputStream()
    val formatter = DiffFormatter(outputStream)

    formatter.setRepository(git.repository)
    formatter.format(oldTreeIterator, newTreeIterator)
    return outputStream.toString()
}


fun main() {
    val buildRoot =
        "/Users/phantran/Study/Passau/Thesis/Evaluation/admin/target"
    val codeRoot =
        "/Users/phantran/Study/Passau/Thesis/Evaluation/admin/target/classes"
    val testRoot =
        "/Users/phantran/Study/Passau/Thesis/Evaluation/admin/target/test-classes"

    val baseDir = "/Users/phantran/Study/Passau/Thesis/Evaluation/admin"

//    val buildRoot =
//        "/Users/phantran/Study/Passau/Thesis/Evaluation/user10/target"
//    val codeRoot =
//        "/Users/phantran/Study/Passau/Thesis/Evaluation/user10/target/classes"
//    val testRoot =
//        "/Users/phantran/Study/Passau/Thesis/Evaluation/user10/target/test-classes"
//
//    val baseDir = "/Users/phantran/Study/Passau/Thesis/Evaluation/user10"

    val codeRootDir = ""
    var codeTarget = codeRoot
    if (codeRootDir.isNotEmpty()) {
        val temp = "$codeTarget${File.separator}$codeRootDir"
        if (File(temp).exists()) {
            codeTarget = temp
        } else {
            return
        }
    }

    val testRootDir = ""
    var testTarget = testRoot
    if (testRootDir.isNotEmpty()) {
        val temp = "$testTarget${File.separator}$testRootDir"
        if (File(temp).exists()) {
            testTarget = temp
        } else {
            return
        }
    }


    val classpath =
        System.getProperty("java.class.path") + File.pathSeparator + codeRoot + File.pathSeparator + testRoot
    val jvm = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
    val excluded = ""
    val fOpNames = Operator.supportedOperatorNames.filter { !excluded.contains(it) }
    val configuration = Configuration(
        baseDir,
        System.currentTimeMillis().toString(),
        buildRoot,
        codeRoot,
        testRoot,
        codeTarget,
        testTarget,
        "$buildRoot${File.separator}moco",
            "",
        "",
        "",
        "",
        classpath,
        jvm,
        "preprocess",
        "mutation",
        excluded,
        fOpNames,
        baseDir,
        listOf(),
        "com.example",
        "jenkins-test",
        false,
        "500",
        5,
        false,
        debugEnabled = true,
        verbose = true,
        1,
        enableMetrics = true,
        mocoPluginVersion = "1.0-SNAPSHOT"
    )
    Configuration.currentConfig = configuration
    MoCoLogger.useKotlinLog()
    MoCoLogger.debugEnabled = Configuration.currentConfig!!.debugEnabled
    H2Database.initPool(
        url = "jdbc:h2:file:${Configuration.currentConfig?.mocoBuildPath}" +
                "${File.separator}/persistence/moco;mode=MySQL",
        user = "moco",
        password = "moco",
    )
    val logger = MoCoLogger()
    logger.info("-----------------------------------------------------------------------")
    logger.info("                               M O C O")
    logger.info("-----------------------------------------------------------------------")
    logger.info("START")
    H2Database().initDBTablesIfNotExists()
    MoCoEntryPoint(configuration).execute()
    H2Database.shutDownDB()


//    com/example/CalculatorTest,com/example/RationalTest,com/example/ComplexTest,com/example/NestedLoopTest," +
//    "com/example/SimpleIntegerTest,com/example/TestClass

//    val s = File.separator
//    val root = "/Users/phantran/.m2/repository"
//    val res: MutableSet<String> = mutableSetOf()
//    val reader = MavenXpp3Reader()
//    val model: Model = reader.read(FileReader("pom.xml"))
//    val depNames: List<Pair<String,String>> = listOf(
//        Pair("junit-platform-launcher", "1.7.1"), Pair("junit-jupiter", "5.8.0-M1"),
//        Pair("junit-jupiter-engine", "5.8.0-M1"), Pair("junit-jupiter-params", "5.8.0-M1"),
//        Pair("junit-platform-runner", "1.7.1"), Pair("junit", "4.13.2"), Pair("testng", "6.9.10")
//    )
//    println(model.dependencies)
//    model.dependencies.map {
//        if (depNames.any {it1 -> it1.first == it.artifactId && it1.second == it.version }) {
//            val depPath = root + s + it.groupId.replace(".", s) + s +
//                    it.artifactId + s + it.version + s + it.artifactId + "-" + it.version + ".jar"
//            val depFile = File(depPath)
//            if (depFile.exists()) {
//                res.add(depPath)
//            }
//        }
//    }
//    println(res)

//    "org/example/AnstrengendTest,org/example/FeatureTest," +
//            "org/example/SimpleExampleTest," +
//            "org/example/TestSuite,org/example/TestTest1,org/example/HihiTest," +
//            "org/example/Junit3Example,org/example/Junit5Example,org/example/TestNGExample,org/example/MainTest"


//    "io/moco/dev/AnstrengendTest,io/moco/dev/FeatureTest," +
//            "io/moco/dev/HeheTest,io/moco/dev/MainTest,io/moco/dev/SimpleExampleTest," +
//            "io/moco/dev/TestSuite,io/moco/dev/HihiTest,",
//
//                "AOD, BLD, POUOI, PRUOI, AOR, BLR, ROR",
//
//    "io/moco/dev/AnstrengendTest,io/moco/dev/FeatureTest," +
//            "io/moco/dev/HeheTest,io/moco/dev/MainTest,io/moco/dev/SimpleExampleTest," +
//            "io/moco/dev/TestSuite,io/moco/dev/TestTest1,io/moco/dev/HihiTest,"

//    val cr = ClassReader("SimpleExample")
//    val cw = ClassWriter(ClassWriter.COMPUTE_MAXS)
//    val temp = DefaultClassVisitor()
//    val cv = TraceClassVisitor(temp, PrintWriter(System.out))
//    val ca = CheckClassAdapter(cv)
//    try {
//        cr.accept(ca, ClassReader.EXPAND_FRAMES)
//    } catch (e: Exception) {
//        println(e.printStackTrace().toString())
//    }
}


//    "mutationDetails" : {
//        "methodInfo" : {
//        "className" : "org/example/Hihi",
//        "methodName" : "addRiders",
//        "methodDescription" : "(I)V"
//    },
//        "instructionIndices" : [ 9 ],
//        "mutationOperatorName" : "ROR",
//        "mutatorID" : "IF_ICMPGT-IF_ICMPLT-50",
//        "fileName" : "Hihi.java",
//        "loc" : 50,
//        "mutationDescription" : "replace less than or equal operator with greater than or equal operator",
//        "instructionsOrder" : [ "9" ],
//        "additionalInfo" : { }
//    },
//    "result" : "survived",
//    "uniqueID" : 1769542436

//    var details = MutationDetails(
//        mapOf("a" to "a"),
//        listOf(14),
//        "AOR",
//        "IF_ICMPGT-IF_ICMPLT-50",
//        "Feature.java", 51,
//        "delete second operand after arithmetic operator integer addition",
//        listOf("14"), mapOf("varName" to "numEntering"))
//    var mutation = MutationInfo(details, "survived", -1547277781)
//    var line ="        if (numRiders + numEntering <= capacity) {"
//    var temp = MutationPresentation.createMutatedLine(line, mutation, 27)
//    println(temp)
//
//    val outputSettings = Document.OutputSettings()
//    outputSettings.prettyPrint(false)
//    val temp1 = Jsoup.clean(line, "", Whitelist.none(), outputSettings)
//    println(temp1)
//    println(Parser.unescapeEntities(temp1, true));

//    details = MutationDetails(
//        mapOf("a" to "a"),
//        listOf(10),
//        "AOD",
//        "IMUL-F-27",
//        "Feature.java", 27,
//        "delete second operand after arithmetic operator integer addition",
//        listOf("10"), mapOf())
//    mutation = MutationInfo(details, "survived", -1547277781)
//    line = "        this.value = intValue * value2++;"
//    temp = MutationPresentation.createMutatedLine(line, mutation, 27)
//    println(temp)
//
//
//    details = MutationDetails(
//        mapOf("a" to "a"),
//        listOf(10),
//        "AOD",
//        "IDIV-S-27",
//        "Feature.java", 27,
//        "delete second operand after arithmetic operator integer addition",
//        listOf("10"), mapOf())
//    mutation = MutationInfo(details, "survived", -1547277781)
//    line = "        this.value = intValue / value2++;"
//    temp = MutationPresentation.createMutatedLine(line, mutation, 27)
//    println(temp)
//
//    details = MutationDetails(
//        mapOf("a" to "a"),
//        listOf(10),
//        "AOD",
//        "IADD-S-27",
//        "Feature.java", 27,
//        "delete second operand after arithmetic operator integer addition",
//        listOf("10"), mapOf())
//    mutation = MutationInfo(details, "survived", -1547277781)
//    line = "        this.value = (this.intValfsdfue + (++this.value2)) - 5 * aasdsabc;"
//    temp = MutationPresentation.createMutatedLine(line, mutation, 27)
//    println(temp)
//
//    details = MutationDetails(
//        mapOf("a" to "a"),
//        listOf(10),
//        "AOD",
//        "ISUB-F-27",
//        "Feature.java", 27,
//        "delete second operand after arithmetic operator integer addition",
//        listOf("10"), mapOf())
//    mutation = MutationInfo(details, "survived", -1547277781)
//    line = "        this.value = (--intValue - value2++);"
//    temp = MutationPresentation.createMutatedLine(line, mutation, 27)
//    println(temp)
//
//    details = MutationDetails(
//        mapOf("a" to "a"),
//        listOf(10),
//        "AOD",
//        "IREM-F-27",
//        "Feature.java", 27,
//        "delete second operand after arithmetic operator integer addition",
//        listOf("10"), mapOf())
//    mutation = MutationInfo(details, "survived", -1547277781)
//    line = "        this.value = (value2-- % Math.asd++);"
//    temp = MutationPresentation.createMutatedLine(line, mutation, 27)
//    println(temp)


//    val abcdf = "int f = -b - a + k-- - (+l);"

//    val abcde = "f > f % && | +/6 ! (++asd.value++ & == !="
//    val haha = "(?<!&)&(?!&)"
//    val reg = "((\\(?)([a-zA-Z_\$][a-zA-Z\\d_.\$]*)((\\+{2}|-{2})?)(\\)?))|((\\(?)((\\+{2}|-{2})?)([a-zA-Z_\$][a-zA-Z\\d_.\$]*)(\\)?))"
//    val reg1 = Regex("\\w\\+\\+(?!\\+) ")
//
////    val x = Regex(reg1)
//    val z = reg1.findAll("d++ ").iterator()
//    var count = 0
//    while(z.hasNext()) {
//        count++
//        val tata = z.next()
//        println(tata.range)
//        println(tata.value)
//    }


//    DriverManager.getConnection(
//        "jdbc:h2:file:${Configuration.currentConfig?.mocoBuildPath}${File.separator}/persistence/persistence;mode=MySQL",
//        "moco",
//        "moco"
//    ).use { con ->
//        con.createStatement().use { st ->
//            st.execute("Drop table if exists ProjectTestHistory")
//        }
//    }
//}


//        val abc = H2Database()
//
//        abc.dropTable("ProjectMeta")
//
//        val schema: Map<String, String> = mapOf(
//            "id" to "int NOT NULL AUTO_INCREMENT",
//            "meta_key" to "varchar(255) PRIMARY KEY",
//            "meta_value" to "varchar(255)"
//        )
//        abc.createTable("ProjectMeta", schema)
//
//    val time = measureTimeMillis {
//        abc.insert("ProjectMeta", mapOf("meta_key" to "latestStoredBranchName", "meta_value" to "1283701298710927"))
//        abc.insert("ProjectMeta", mapOf("meta_key" to "storedHeadCommit", "meta_value" to "kahsudgkasd"))
//        val xyz = ProjectMeta()
//        println(xyz)
//    }
//    abc.insertOrUpdateIfExist("ProjectMeta", mapOf("meta_key" to "storedHeadCommit", "meta_value" to "abcdef"))
//    abc.insertOrUpdateIfExist("ProjectMeta", mapOf("meta_key" to "storedHeadCommit", "meta_value" to "abcdef"))
//
//    val res = abc.fetch("ProjectMeta")
//    H2Database.printResults(res!!)


//    val url = "jdbc:h2:file:/Users/phantran/Study/Passau/Thesis"
//    val user = "sa"
//    val passwd = "hahaha"
//

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
