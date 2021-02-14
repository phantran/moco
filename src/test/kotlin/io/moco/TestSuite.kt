package io.moco

import io.moco.engine.DummyForTesting
import io.moco.engine.MethodName
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses


@RunWith(Suite::class)
@SuiteClasses(TestTest1::class, TestTest2::class)
class TestSuite  {
    @Test
    fun aasdasdas() {
        val t = DummyForTesting()
        val x = MethodName("abc")
        t.dummy()
        val a = 1
        val b = 2
        Assert.assertEquals(b.toLong(), (a + 1).toLong())
    }
}