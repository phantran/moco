package io.moco.engine.mutator.replacement

import io.moco.engine.mutation.MutationID
import io.moco.engine.operator.ReplacementOperator
import io.moco.engine.tracker.MutatedMethodTracker
import io.moco.utils.ASMInfoUtil
import io.moco.utils.MoCoLogger
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes


class AORVisitor(
    val operator: ReplacementOperator,
    val tracker: MutatedMethodTracker,
    delegateMethodVisitor: MethodVisitor
) : MethodVisitor(ASMInfoUtil.ASM_VERSION, delegateMethodVisitor) {
    private val logger = MoCoLogger()

    companion object {

        private val opcodeDesc: Map<Int, String> = mapOf(
            Opcodes.IADD to "integer addition", Opcodes.ISUB to "integer subtraction",
            Opcodes.IMUL to "integer multiplication", Opcodes.IDIV to "integer division",
            Opcodes.IREM to "integer modulo",

            Opcodes.LADD to "long addition", Opcodes.LSUB to "long subtraction",
            Opcodes.LMUL to "long multiplication", Opcodes.LDIV to "long division",
            Opcodes.LREM to "long modulo",

            Opcodes.FADD to "float addition", Opcodes.FSUB to "float subtraction",
            Opcodes.FMUL to "float multiplication", Opcodes.FDIV to "float division",
            Opcodes.FREM to "float modulo",

            Opcodes.DADD to "double addition", Opcodes.DSUB to "double subtraction",
            Opcodes.DMUL to "double multiplication", Opcodes.DDIV to "double division",
            Opcodes.DREM to "double modulo",
        )

        fun createDescription(op1: Int, op2: Int): String {
            return "Replacement of ${opcodeDesc[op1]} with ${opcodeDesc[op2]}"
        }

        val supportedOpcodes = mapOf(
            "int" to listOf(Opcodes.IADD, Opcodes.ISUB, Opcodes.IMUL, Opcodes.IDIV, Opcodes.IREM),
            "long" to listOf(Opcodes.LADD, Opcodes.LSUB, Opcodes.LMUL, Opcodes.LDIV, Opcodes.LREM),
            "float" to listOf(Opcodes.FADD, Opcodes.FSUB, Opcodes.FMUL, Opcodes.FDIV, Opcodes.FREM),
            "double" to listOf(Opcodes.DADD, Opcodes.DSUB, Opcodes.DMUL, Opcodes.DDIV, Opcodes.DREM)
        )
    }

    override fun visitInsn(opcode: Int) {
        var supported = false
        var type = ""
        for (key in supportedOpcodes.keys) {
            if (supportedOpcodes[key]!!.contains(opcode)) {
                supported = true
                type = key
                break
            }
        }
        var visited = false
        if (supported) {
            for (newOpcode in supportedOpcodes[type]!!) {
                if (newOpcode != opcode) {
                    val operation = ReplaceOperation(newOpcode, createDescription(opcode, newOpcode))
                    // Collect mutation information
                    val newMutation = tracker.registerMutation(operator, operation.message)

                    if (tracker.mutatedClassTracker.targetMutationID != null) {
                        // In mutant creation phase, visit corresponding instruction to mutate it
                        if (tracker.isTargetMutation(newMutation.mutationID)) {
                            tracker.mutatedClassTracker.setTargetMutation(newMutation)
                            logger.debug("Old Opcode: $opcode")
                            logger.debug("New Opcode: ${operation.newOpcode}")
                            operation.mutate(mv)
                            visited = true
                            break
                        }
                    }
                }
            }
            if (!visited) {
                // Go on without mutating bytecode after collecting all possible mutations
                mv.visitInsn(opcode)
            }
        } else {
            mv.visitInsn(opcode)
        }

    }
}
