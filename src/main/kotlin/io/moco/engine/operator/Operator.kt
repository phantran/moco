package io.moco.engine.operator

import io.moco.engine.MethodInfo
import io.moco.engine.tracker.MutatedMethodTracker
import org.objectweb.asm.MethodVisitor


interface Operator {

    fun generateVisitor(
        tracker: MutatedMethodTracker,
        methodInfo: MethodInfo,
        delegateMethodVisitor: MethodVisitor
    ): MethodVisitor

    fun getName(): String
}