package io.moco

import org.apache.maven.plugin.testing.MojoRule
import org.apache.maven.plugin.testing.WithoutMojo
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.lang.Exception

class MocoTest {
//    @Rule
//    var rule: MojoRule = object : MojoRule() {
//        @Throws(Throwable::class)
//        override fun before() {
//        }
//
//        override fun after() {}
//    }
//
//    /**
//     * @throws Exception if any
//     */
//    @Test
//    @Throws(Exception::class)
//    fun testSomething() {
//        val pom = File("target/test-classes/project-to-test/")
//        Assert.assertNotNull(pom)
//        Assert.assertTrue(pom.exists())
//        val myMojo: Moco = rule.lookupConfiguredMojo(pom, "touch") as Moco
//        Assert.assertNotNull(myMojo)
//        myMojo.execute()
//        val outputDirectory = rule.getVariableValueFromObject(myMojo, "outputDirectory") as File
//        Assert.assertNotNull(outputDirectory)
//        Assert.assertTrue(outputDirectory.exists())
//        val touch = File(outputDirectory, "touch.txt")
//        Assert.assertTrue(touch.exists())
//    }

    /** Do not need the MojoRule.  */
    @WithoutMojo
    @Test
    fun testSomethingWhichDoesNotNeedTheMojoAndProbablyShouldBeExtractedIntoANewClassOfItsOwn() {
        Assert.assertTrue(true)
    }
}