package io.moco.engine.operator

import io.moco.engine.MethodInfo
import io.moco.engine.tracker.MutatedMethodTracker
import org.objectweb.asm.MethodVisitor


interface Operator {
    val operatorName: String

    companion object {
//        val supportedOperatorNames = listOf("AOR", "ROR", "LCR", "UOI")
        val supportedOperatorNames = listOf("AOR", "ROR")

        fun nameToOperator(it: String): Operator? {
            return mapping[it]
        }
        private val mapping: Map<String, Operator> = mapOf(
            "AOR" to ReplacementOperator("AOR"), // Arithmetic operator replacement
            "ROR" to ReplacementOperator("ROR"), // Relational operator replacement
//            "LCR" to ReplacementOperator(), // Logical connector replacement
//            "UOI" to ReplacementOperator() // Unary operator insertion
        )
    }

    fun generateVisitor(
        tracker: MutatedMethodTracker,
        methodInfo: MethodInfo,
        delegateMethodVisitor: MethodVisitor
    ): MethodVisitor?
}