package io.moco.engine.operator

import io.moco.engine.MethodInfo
import io.moco.engine.tracker.MutatedMethodTracker
import org.objectweb.asm.MethodVisitor


class ReplacementOperator: Operator {
    val operatorName = "AOR"

    override fun generateVisitor(
        tracker: MutatedMethodTracker,
        methodInfo: MethodInfo,
        delegateMethodVisitor: MethodVisitor
    ): MethodVisitor {
        return AOR1Visitor(this, tracker, methodInfo, delegateMethodVisitor)
    }

    override fun getName(): String {
        return operatorName
    }
}