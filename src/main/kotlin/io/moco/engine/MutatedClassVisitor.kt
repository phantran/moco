package io.moco.engine


import io.moco.engine.operator.Operator
import io.moco.engine.tracker.MutatedClassTracker
import io.moco.engine.tracker.MutatedMethodTracker
import io.moco.utils.ASMInfoUtil
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

import java.util.HashSet


class MutatedClassVisitor(
    delegateClassVisitor: ClassVisitor?, val tracker: MutatedClassTracker,
    val filter: List<String> = mutableListOf(), operators: List<Operator>?
) : ClassVisitor(ASMInfoUtil.ASM_VERSION, delegateClassVisitor) {

    private val chosenOperators: MutableSet<Operator> = HashSet<Operator>()

    init {
        chosenOperators.addAll(operators!!)
    }

    override fun visit(
        version: Int, access: Int, name: String,
        signature: String, superName: String, interfaces: Array<String>
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        tracker.setClsInfo(ClassInfo(
            version, access, name, signature,
            superName, interfaces
        ))
    }

    override fun visitSource(source: String, debug: String) {
        super.visitSource(source, debug)
        tracker.setFileName(source)
    }

    override fun visitMethod(
        access: Int, methodName: String,
        methodDescriptor: String, signature: String,
        exceptions: Array<String>
    ): MethodVisitor {

        val clsInfo = tracker.getClsInfo()
        val methodTracker = MutatedMethodTracker(
            tracker, MutatedMethodLocation(
                clsInfo?.let { ClassName.fromString(it.name) },
                MethodName(methodName), methodDescriptor)
            )

        val methodVisitor = cv.visitMethod(
            access, methodName,
            methodDescriptor, signature, exceptions
        )

        val info = MethodInfo(tracker.getClsInfo(), access, methodName, methodDescriptor)
        //TODO: change filter to kind of regex or predicate to filter methods that should be excluded from mutation
        return if (!filter.contains(info.name)) {
            var chain = methodVisitor
            for (each in chosenOperators) {
                chain = each.generateVisitor(methodTracker, info, chain)
            }
            chain
        }else {
            methodVisitor
        }
    }
}