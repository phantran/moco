//package io.moco.engine.mutation
//
//import io.moco.utils.DataStreamUtils
//import java.io.DataInputStream
//import java.io.IOException
//import java.lang.Exception
//import java.lang.management.MemoryNotificationInfo
//import java.net.Socket
//import java.util.ArrayList
//import java.util.logging.Level
//import java.util.logging.Logger
//import java.util.stream.Collectors
//import javax.management.Notification
//import javax.management.NotificationListener
//import javax.management.openmbean.CompositeData
//
//
//class MutationTestMinion(
//    private val dis: DataInputStream,
//) {
//
//    fun run() {
//        try {
//            val givenArgs: ResultsReceiverThread.WorkerArguments = DataStreamUtils.readObject(dis)
//
//                val loader: ClassLoader = IsolationUtils.getContextClassLoader()
//            val byteSource: ClassByteArraySource = CachingByteArraySource(
//                ClassloaderByteArraySource(
//                    loader
//                ), CACHE_SIZE
//            )
//            val hotswap: F3<ClassName, ClassLoader, ByteArray, Boolean> = HotSwap(
//                byteSource
//            )
//            val engine: MutationEngine = createEngine(paramsFromParent.engine, paramsFromParent.engineArgs)
//            val worker = MutationTestWorker(
//                hotswap,
//                engine.createMutator(byteSource), loader, paramsFromParent.fullMutationMatrix
//            )
//            val tests: List<TestUnit> = findTestsForTestClasses(
//                loader,
//                paramsFromParent.testClasses, createTestPlugin(paramsFromParent.pitConfig)
//            )
//            worker.run(
//                paramsFromParent.mutations, reporter,
//                TimeOutDecoratedTestSource(
//                    paramsFromParent.timeoutStrategy,
//                    tests, reporter
//                )
//            )
//            reporter.done(ExitCode.OK)
//        } catch (ex: Throwable) {
//            ex.printStackTrace(System.out)
//            LOG.log(Level.WARNING, "Error during mutation test", ex)
//            reporter.done(ExitCode.UNKNOWN_ERROR)
//        }
//    }
//
//
//    companion object {
//
//        private const val CACHE_SIZE = 12
//        @JvmStatic
//        fun main(args: Array<String>) {
//            enablePowerMockSupport()
//            val port = Integer.valueOf(args[0])
//            var s: Socket? = null
//            try {
//                s = Socket("localhost", port)
//                val dis = SafeDataInputStream(
//                    s.getInputStream()
//                )
//                val reporter: Reporter = DefaultReporter(s.getOutputStream())
//                addMemoryWatchDog(reporter)
//                val plugins = ClientPluginServices(IsolationUtils.getContextClassLoader())
//                val factory = MinionSettings(plugins)
//                val instance = MutationTestMinion(factory, dis, reporter)
//                instance.run()
//            } catch (ex: Throwable) {
//                ex.printStackTrace(System.out)
//                LOG.log(Level.WARNING, "Error during mutation test", ex)
//            } finally {
//                if (s != null) {
//                    safelyCloseSocket(s)
//                }
//            }
//        }
//
//        private fun findTestsForTestClasses(
//            loader: ClassLoader, testClasses: Collection<ClassName>,
//            pitConfig: Configuration
//        ): List<TestUnit> {
//            val tcs: Collection<Class<*>> = testClasses.stream().flatMap(ClassName.nameToClass(loader))
//                .collect<Collection<Class<*>>, Any>(Collectors.toList<Any>())
//            val finder = FindTestUnits(pitConfig)
//            return finder.findTestUnitsForAllSuppliedClasses(tcs)
//        }
//
//        private fun enablePowerMockSupport() {
//            // Bwahahahahahahaha
//            HotSwapAgent.addTransformer(
//                BendJavassistToMyWillTransformer(
//                    Prelude
//                        .or(Glob("javassist/*")), JavassistInputStreamInterceptorAdapater.inputStreamAdapterSupplier(
//                        JavassistInterceptor::class.java
//                    )
//                )
//            )
//        }
//
//        private fun safelyCloseSocket(s: Socket?) {
//            if (s != null) {
//                try {
//                    s.close()
//                } catch (e: IOException) {
//                    LOG.log(Level.WARNING, "Couldn't close socket", e)
//                }
//            }
//        }
//    }
//}
//
////class MutationTestWorker(
////    hotswap: F3<ClassName?, ClassLoader?, ByteArray?, Boolean?>,
////    mutater: Mutater, private val loader: ClassLoader, fullMutationMatrix: Boolean
////) {
////    private val mutater: Mutater
////    private val hotswap: F3<ClassName, ClassLoader, ByteArray, Boolean>
////    private val fullMutationMatrix: Boolean
////    @Throws(IOException::class)
////    protected fun run(
////        range: Collection<MutationDetails>, r: Reporter,
////        testSource: TimeOutDecoratedTestSource
////    ) {
////        for (mutation: Mutation in range) {
////            if (DEBUG) {
////                LOG.fine("Running mutation $mutation")
////            }
////            val t0 = System.currentTimeMillis()
////            processMutation(r, testSource, mutation)
////            if (DEBUG) {
////                LOG.fine(
////                    "processed mutation in " + (System.currentTimeMillis() - t0)
////                            + " ms."
////                )
////            }
////        }
////    }
////
////    @Throws(IOException::class)
////    private fun processMutation(
////        r: Reporter,
////        testSource: TimeOutDecoratedTestSource,
////        mutationDetails: MutationDetails
////    ) {
////        val mutationId: MutationIdentifier = mutationDetails.getId()
////        val mutatedClass: Mutant = mutater.getMutation(mutationId)
////
////        // For the benefit of mocking frameworks such as PowerMock
////        // mess with the internals of Javassist so our mutated class
////        // bytes are returned
////        JavassistInterceptor.setMutant(mutatedClass)
////        if (DEBUG) {
////            LOG.fine("mutating method " + mutatedClass.getDetails().getMethod())
////        }
////        val relevantTests: List<TestUnit> = testSource
////            .translateTests(mutationDetails.getTestsInOrder())
////        r.describe(mutationId)
////        val mutationDetected: MutationStatusTestPair = handleMutation(
////            mutationDetails, mutatedClass, relevantTests
////        )
////        r.report(mutationId, mutationDetected)
////        if (DEBUG) {
////            LOG.fine("Mutation $mutationId detected = $mutationDetected")
////        }
////    }
////
////    private fun handleMutation(
////        mutationId: MutationDetails, mutatedClass: Mutant,
////        relevantTests: List<TestUnit>?
////    ): MutationStatusTestPair {
////        val mutationDetected: MutationStatusTestPair
////        if ((relevantTests == null) || relevantTests.isEmpty()) {
////            LOG.info(
////                ("No test coverage for mutation  " + mutationId + " in "
////                        + mutatedClass.getDetails().getMethod())
////            )
////            mutationDetected = MutationStatusTestPair.notAnalysed(0, DetectionStatus.RUN_ERROR)
////        } else {
////            mutationDetected = handleCoveredMutation(
////                mutationId, mutatedClass,
////                relevantTests
////            )
////        }
////        return mutationDetected
////    }
////
////    private fun handleCoveredMutation(
////        mutationId: MutationDetails, mutatedClass: Mutant,
////        relevantTests: List<TestUnit>
////    ): MutationStatusTestPair {
////        val mutationDetected: MutationStatusTestPair
////        if (DEBUG) {
////            LOG.fine(
////                ("" + relevantTests.size + " relevant test for "
////                        + mutatedClass.getDetails().getMethod())
////            )
////        }
////        val c: Container = createNewContainer()
////        val t0 = System.currentTimeMillis()
////        if (hotswap.apply(
////                mutationId.getClassName(), loader,
////                mutatedClass.getBytes()
////            )
////        ) {
////            if (DEBUG) {
////                LOG.fine(
////                    ("replaced class with mutant in "
////                            + (System.currentTimeMillis() - t0) + " ms")
////                )
////            }
////            mutationDetected = doTestsDetectMutation(c, relevantTests)
////        } else {
////            LOG.warning("Mutation $mutationId was not viable ")
////            mutationDetected = MutationStatusTestPair.notAnalysed(
////                0,
////                DetectionStatus.NON_VIABLE
////            )
////        }
////        return mutationDetected
////    }
////
////    override fun toString(): String {
////        return ("MutationTestWorker [mutater=" + mutater + ", loader="
////                + loader + ", hotswap=" + hotswap + "]")
////    }
////
////    private fun doTestsDetectMutation(
////        c: Container,
////        tests: List<TestUnit>
////    ): MutationStatusTestPair {
////        try {
////            val listener = CheckTestHasFailedResultListener(fullMutationMatrix)
////            val pit = Pitest(listener)
////            if (fullMutationMatrix) {
////                pit.run(c, tests)
////            } else {
////                pit.run(c, createEarlyExitTestGroup(tests))
////            }
////            return createStatusTestPair(listener)
////        } catch (ex: Exception) {
////            throw translateCheckedException(ex)
////        }
////    }
////
////    private fun createStatusTestPair(
////        listener: CheckTestHasFailedResultListener
////    ): MutationStatusTestPair {
////        val failingTests: List<String> = listener.getFailingTests().stream()
////            .map { description -> description.getQualifiedName() }.collect(Collectors.toList())
////        val succeedingTests: List<String> = listener.getSucceedingTests().stream()
////            .map { description -> description.getQualifiedName() }.collect(Collectors.toList())
////        return MutationStatusTestPair(
////            listener.getNumberOfTestsRun(),
////            listener.status(), failingTests, succeedingTests
////        )
////    }
////
////    private fun createEarlyExitTestGroup(tests: List<TestUnit>): List<TestUnit> {
////        return listOf<TestUnit>(MultipleTestGroup(tests))
////    }
////
////    companion object {
////        private val LOG: Logger = Log
////            .getLogger()
////
////        // micro optimise debug logging
////        private val DEBUG = LOG
////            .isLoggable(Level.FINE)
////
////        private fun createNewContainer(): Container {
////            return object : UnContainer() {
////                fun execute(group: TestUnit): List<TestResult> {
////                    val results: List<TestResult> = ArrayList<TestResult>()
////                    val rc = ExitingResultCollector(
////                        ConcreteResultCollector(results)
////                    )
////                    group.execute(rc)
////                    return results
////                }
////            }
////        }
////    }
////
////    init {
////        this.mutater = mutater
////        this.hotswap = hotswap
////        this.fullMutationMatrix = fullMutationMatrix
////    }
////}