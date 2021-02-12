package io.moco.engine.operator

import io.moco.engine.MethodInfo
import io.moco.engine.tracker.MutatedMethodTracker
import org.objectweb.asm.MethodVisitor


interface Operator {
    companion object {
        fun nameToOperator(it: String): Operator? {
            return mapping[it]
        }

        private val mapping: Map<String, Operator> = mapOf(
            "AOR" to ReplacementOperator(), // Arithmetic operator replacement
            "LCR" to ReplacementOperator(), // Logical connector replacement
            "ROR" to ReplacementOperator(), // Relational operator replacement
            "UOI" to ReplacementOperator() // Unary operator insertion
        )
    }

    fun generateVisitor(
        tracker: MutatedMethodTracker,
        methodInfo: MethodInfo,
        delegateMethodVisitor: MethodVisitor
    ): MethodVisitor

    fun getName(): String
}