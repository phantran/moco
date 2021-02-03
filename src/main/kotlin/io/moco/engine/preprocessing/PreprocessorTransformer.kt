package io.moco.engine.preprocessing

//import jdk.internal.org.objectweb.asm.ClassWriter.COMPUTE_MAXS
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.IllegalClassFormatException
import java.security.ProtectionDomain


class PreprocessorTransformer : ClassFileTransformer {
    @Throws(IllegalClassFormatException::class)
    override fun transform(
        loader: ClassLoader, className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain, classfileBuffer: ByteArray
    ): ByteArray? {
        return if (isCUT(className)) {
            try {
                val cr = ClassReader(classfileBuffer)
                val cw = ClassWriter(cr, 0);
                return try {
                    cr.accept(
                        PreprocessorClassVisitor(cw),
                        ClassReader.EXPAND_FRAMES
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
        return true
    }
}