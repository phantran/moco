package io.moco.engine.preprocessing

//import jdk.internal.org.objectweb.asm.ClassWriter.COMPUTE_MAXS

import io.moco.engine.ClassName
import org.objectweb.asm.*;
import org.objectweb.asm.ClassWriter.COMPUTE_FRAMES
import org.objectweb.asm.ClassWriter.COMPUTE_MAXS
import org.objectweb.asm.util.TraceClassVisitor
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.IllegalClassFormatException
import java.security.ProtectionDomain
import java.io.PrintWriter


class PreprocessorTransformer(targets: MutableList<ClassName?>) : ClassFileTransformer {

    private val includedTargets = targets

    @Throws(IllegalClassFormatException::class)
    override fun transform(
        loader: ClassLoader, className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain, classfileBuffer: ByteArray
    ): ByteArray? {
        return if (isCUT(className)) {
            try {
                val cr = ClassReader(classfileBuffer)
                val cw = ClassWriter(cr, COMPUTE_MAXS);
                val cv = TraceClassVisitor(cw, PrintWriter(System.out))

                return try {
                    cr.accept(
                        PreprocessorClassVisitor(cw),
                        ClassReader.SKIP_FRAMES
                    )
                    cw.toByteArray()
                } catch (e: Exception) {
                    // Log exception here
                    null
                }
            } catch (t: RuntimeException) {
                System.err.println("RuntimeException while transforming and preprocessing $className")
                t.printStackTrace()
                throw t
            }
        } else {
            null
        }
    }

    // TODO: find a way to choose only source classes -> by using codebase class for instance
    private fun isCUT(className: String): Boolean {
        if (includedTargets.any { it?.getInternalName() == className })  {
            if (className == "io/moco/engine/preprocessing/PreprocessorTracker") {
                return false
            }
            return true
        }
        return false
    }
}