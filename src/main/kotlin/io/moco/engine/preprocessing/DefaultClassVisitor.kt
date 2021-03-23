package io.moco.engine.preprocessing

import io.moco.utils.JavaInfo
import org.objectweb.asm.*

class DefaultClassVisitor : ClassVisitor(JavaInfo.ASM_VERSION) {
    class DefaultAnnotationVisitor internal constructor() : AnnotationVisitor(JavaInfo.ASM_VERSION) {
        override fun visit(arg0: String?, arg1: Any) {}
        override fun visitAnnotation(
            arg0: String?, arg1: String?
        ): AnnotationVisitor {
            return DefaultAnnotationVisitor()
        }

        override fun visitArray(arg0: String?): AnnotationVisitor {
            return DefaultAnnotationVisitor()
        }

        override fun visitEnd() {}
        override fun visitEnum(
            arg0: String?, arg1: String?,
            arg2: String?
        ) {
        }
    }

    class DefaultMethodVisitor internal constructor() : MethodVisitor(JavaInfo.ASM_VERSION) {
        override fun visitAnnotation(
            arg0: String?,
            arg1: Boolean
        ): AnnotationVisitor {
            return DefaultAnnotationVisitor()
        }

        override fun visitAnnotationDefault(): AnnotationVisitor {
            return DefaultAnnotationVisitor()
        }

        override fun visitAttribute(arg0: Attribute?) {}
        override fun visitCode() {}
        override fun visitEnd() {}
        override fun visitFieldInsn(
            arg0: Int, arg1: String?,
            arg2: String?, arg3: String?
        ) {
        }

        override fun visitFrame(
            arg0: Int, arg1: Int, arg2: Array<Any>?,
            arg3: Int, arg4: Array<Any>?
        ) {
        }

        override fun visitIincInsn(arg0: Int, arg1: Int) {}
        override fun visitInsn(arg0: Int) {}
        override fun visitIntInsn(arg0: Int, arg1: Int) {}
        override fun visitJumpInsn(arg0: Int, arg1: Label?) {}
        override fun visitLabel(arg0: Label?) {}
        override fun visitLdcInsn(arg0: Any?) {}
        override fun visitLineNumber(arg0: Int, arg1: Label?) {}
        override fun visitLocalVariable(
            arg0: String?, arg1: String?,
            arg2: String?, arg3: Label?, arg4: Label?, arg5: Int
        ) {
        }

        override fun visitLookupSwitchInsn(
            arg0: Label?, arg1: IntArray?,
            arg2: Array<Label>?
        ) {
        }

        override fun visitMaxs(arg0: Int, arg1: Int) {}
        override fun visitMethodInsn(
            arg0: Int, arg1: String?,
            arg2: String?, arg3: String?
        ) {
        }

        override fun visitMultiANewArrayInsn(arg0: String?, arg1: Int) {}
        override fun visitParameterAnnotation(
            arg0: Int,
            arg1: String?, arg2: Boolean
        ): AnnotationVisitor {
            return DefaultAnnotationVisitor()
        }

        override fun visitTableSwitchInsn(
            arg0: Int, arg1: Int,
            arg2: Label?, vararg labels: Label?
        ) {
        }

        override fun visitTryCatchBlock(
            arg0: Label?, arg1: Label?,
            arg2: Label?, arg3: String?
        ) {
        }

        override fun visitTypeInsn(arg0: Int, arg1: String?) {}
        override fun visitVarInsn(arg0: Int, arg1: Int) {}
    }

    override fun visit(
        arg0: Int, arg1: Int, arg2: String?,
        arg3: String?, arg4: String?, arg5: Array<String>?
    ) {
    }

    override fun visitAnnotation(arg0: String?, arg1: Boolean): AnnotationVisitor {
        return DefaultAnnotationVisitor()
    }

    override fun visitAttribute(arg0: Attribute) {}
    override fun visitEnd() {}
    override fun visitField(
        arg0: Int, arg1: String?,
        arg2: String?, arg3: String?, arg4: Any?
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
        arg0: String?, arg1: String?,
        arg2: String?, arg3: Int
    ) {
    }

    override fun visitMethod(
        arg0: Int, arg1: String?,
        arg2: String?, arg3: String?, arg4: Array<String>?
    ): MethodVisitor {
        return DefaultMethodVisitor()
    }

    override fun visitOuterClass(
        arg0: String?, arg1: String?,
        arg2: String?
    ) {
    }

    override fun visitSource(arg0: String?, arg1: String?) {}
}