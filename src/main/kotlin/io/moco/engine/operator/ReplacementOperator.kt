package io.moco.engine.operator

import io.moco.engine.MethodInfo
import io.moco.engine.mutator.replacement.AOR
import io.moco.engine.mutator.replacement.ROR
import io.moco.engine.tracker.MutatedMethodTracker
import org.objectweb.asm.MethodVisitor


class ReplacementOperator(override val operatorName: String): Operator {

    override fun generateVisitor(
        tracker: MutatedMethodTracker,
        methodInfo: MethodInfo,
        delegateMethodVisitor: MethodVisitor
    ): MethodVisitor? {
        if (operatorName == "AOR") {
            return AOR(this, tracker, delegateMethodVisitor)
        } else if (operatorName == "ROR") {
            return ROR(this, tracker, delegateMethodVisitor)
        }
        return null
    }
}