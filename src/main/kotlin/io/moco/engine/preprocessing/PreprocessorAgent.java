package io.moco.engine.preprocessing;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;


public class PreprocessorAgent {

    private static Instrumentation instrumentation;

    public static void premain(final String agentArguments, // NO_UCD
                               final Instrumentation inst) {
        System.out.println("Mutation agent");
        instrumentation = inst;
    }

    public static void addTransformer(final ClassFileTransformer transformer) {
        instrumentation.addTransformer(transformer);
    }

    public static void agentmain(final String agentArguments, // NO_UCD
                                 final Instrumentation inst) throws Exception {
        instrumentation = inst;
    }
}
