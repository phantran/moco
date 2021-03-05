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

package io.moco.engine;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;


public class MoCoAgent {

    public static boolean introduceMutant(final Class<?> toBeReplacedCls, final byte[] bytes) {
        final ClassDefinition[] definitions = {new ClassDefinition(toBeReplacedCls, bytes)};
        try {
            instrumentation.redefineClasses(definitions);
            return true;
        } catch (final ClassNotFoundException | UnmodifiableClassException | VerifyError | InternalError ex) {
            ex.printStackTrace();
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

    public static void agentmain(final String agentArguments, final Instrumentation inst) {
        instrumentation = inst;
    }
}
