package io.moco.engine.mutator.replacement

import io.moco.engine.MethodInfo
import io.moco.engine.mutation.MutationID
import io.moco.engine.operator.ReplacementOperator
import io.moco.engine.tracker.MutatedMethodTracker
import io.moco.utils.ASMInfoUtil
import io.moco.utils.MoCoLogger
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes


class AOR1Visitor(
    val operator: ReplacementOperator,
    val tracker: MutatedMethodTracker,
    delegateMethodVisitor: MethodVisitor
) : MethodVisitor(ASMInfoUtil.ASM_VERSION, delegateMethodVisitor) {
    private val logger = MoCoLogger()

    companion object {
        val opcodeToMutator: MutableMap<Int, ReplaceOperation> = mutableMapOf()

        init {
            opcodeToMutator[Opcodes.LADD] = ReplaceOperation(
                Opcodes.LDIV,
                "Replacement of long addition with division"
            )
            opcodeToMutator[Opcodes.LSUB] = ReplaceOperation(
                Opcodes.LDIV,
                "Replacement of long subtraction with division"
            )
            opcodeToMutator[Opcodes.LMUL] = ReplaceOperation(
                Opcodes.LADD,
                "Replacement of long multiplication with addition"
            )
            opcodeToMutator[Opcodes.LDIV] = ReplaceOperation(
                Opcodes.LADD,
                "Replacement of long division with addition"
            )
            opcodeToMutator[Opcodes.LREM] = ReplaceOperation(
                Opcodes.LADD,
                "Replacement of long modulus with addition"
            )



            opcodeToMutator[Opcodes.DADD] = ReplaceOperation(
                Opcodes.DDIV,
                "Replacement of double addition with division"
            )
            opcodeToMutator[Opcodes.DSUB] = ReplaceOperation(
                Opcodes.DDIV,
                "Replacement of double subtraction with division"
            )
            opcodeToMutator[Opcodes.DMUL] = ReplaceOperation(
                Opcodes.DADD,
                "Replacement of double multiplication with addition"
            )
            opcodeToMutator[Opcodes.DDIV] = ReplaceOperation(
                Opcodes.DADD,
                "Replacement of double division with addition"
            )
            opcodeToMutator[Opcodes.DREM] = ReplaceOperation(
                Opcodes.DADD,
                "Replacement of double modulus with addition"
            )



            opcodeToMutator[Opcodes.IADD] = ReplaceOperation(
                Opcodes.IDIV,
                "Replacement of integer addition with division"
            )
            opcodeToMutator[Opcodes.ISUB] = ReplaceOperation(
                Opcodes.IDIV,
                "Replacement of integer subtraction with division"
            )
            opcodeToMutator[Opcodes.IMUL] = ReplaceOperation(
                Opcodes.IADD,
                "Replacement of integer multiplication with addition"
            )
            opcodeToMutator[Opcodes.IDIV] = ReplaceOperation(
                Opcodes.IADD,
                "Replacement of integer division with addition"
            )
            opcodeToMutator[Opcodes.IREM] = ReplaceOperation(
                Opcodes.IADD,
                "Replacement of integer modulus with addition"
            )



            opcodeToMutator[Opcodes.FADD] = ReplaceOperation(
                Opcodes.FDIV,
                "Replacement of float addition with division"
            )
            opcodeToMutator[Opcodes.FSUB] = ReplaceOperation(
                Opcodes.FDIV,
                "Replacement of float subtraction with division"
            )
            opcodeToMutator[Opcodes.FMUL] = ReplaceOperation(
                Opcodes.FADD,
                "Replacement of float multiplication with addition"
            )
            opcodeToMutator[Opcodes.FDIV] = ReplaceOperation(
                Opcodes.FADD,
                "Replacement of float division with addition"
            )
            opcodeToMutator[Opcodes.FREM] = ReplaceOperation(
                Opcodes.FADD,
                "Replacement of float modulus with addition"
            )
        }
    }


    override fun visitInsn(opcode: Int) {
        if (opcodeToMutator.containsKey(opcode)) {
            val operation: ReplaceOperation? = opcodeToMutator[opcode]
            val newMutationId: MutationID? = operation?.let {
                tracker.registerMutant(operator, it.message)
            }
            if (newMutationId?.let { tracker.isTargetMutation(it) } == true) {
                logger.debug("Old Opcode: $opcode")
                logger.debug("New Opcode: ${operation.replacementOpcode}")
                operation.accept(mv)
            } else {
                mv.visitInsn(opcode)
            }
        } else {
            mv.visitInsn(opcode)
        }
    }
}
