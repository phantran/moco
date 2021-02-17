package io.moco.engine;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;


public class MocoAgent {
    public static boolean introduceMutant(final Class<?> toBeReplacedCls, final byte[] bytes) {
        final ClassDefinition[] definitions = {new ClassDefinition(toBeReplacedCls, bytes)};
        try {
            instrumentation.redefineClasses(definitions);
            return true;
        } catch (final ClassNotFoundException | UnmodifiableClassException | VerifyError | InternalError ignored) {
        }
        return false;
    }

    private static Instrumentation instrumentation;

    public static void premain(final String agentArguments,
                               final Instrumentation inst) {
        instrumentation = inst;
    }

    public static void addTransformer(final ClassFileTransformer transformer) {
        instrumentation.addTransformer(transformer);
    }

    public static void agentmain(final String agentArguments,
                                 final Instrumentation inst) {
        instrumentation = inst;
    }


}
