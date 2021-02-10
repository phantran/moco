package io.moco.engine.test

//import mu.KotlinLogging
import io.moco.engine.preprocessing.PreprocessorTracker
import org.junit.internal.builders.AllDefaultPossibilitiesBuilder
import org.junit.internal.runners.ErrorReportingRunner
import org.junit.runner.Runner
import org.junit.runner.notification.RunListener
import java.lang.Exception
import org.junit.runner.notification.RunNotifier
//import org.junit.runners.BlockJUnit4ClassRunner


class TestItem(
    val cls: Class<*>,
) {
    val desc: Description = Description(this.cls.name, this.cls.name)
    fun execute(tra: TestResultAggregator) {
        val runner: Runner = createRunner(cls)
        if (runner is ErrorReportingRunner) {
            println("Error while running test of $cls")
//            logger.warn { "Error while running test of $cls" }
        }
        try {
            val runNotifier = RunNotifier()
            val listener: RunListener = CustomRunListener(this.desc, tra)
            runNotifier.addFirstListener(listener)
            runner.run(runNotifier)
        } catch (e: Exception) {
//            logger.error(e) { "Error while running test of $cls with desc $desc" }
            throw RuntimeException(e)
        }
    }

    companion object {
//        private val logger = KotlinLogging.logger {}
        fun createRunner(cls: Class<*>): Runner {
            val builder = AllDefaultPossibilitiesBuilder(true)
            try {
                return builder.runnerForClass(cls)
            } catch (ex: Throwable) {
//                logger.error(ex) { "Error while creating runner for $cls"}
                throw RuntimeException(ex)
            }
        }
    }

    override fun toString(): String {
        return ("TestItem [cls=" + cls + "description" + desc + "]")
    }
}