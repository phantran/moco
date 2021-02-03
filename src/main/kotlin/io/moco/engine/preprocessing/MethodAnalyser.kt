package io.moco.engine.preprocessing

import io.moco.engine.tracker.Block
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*


object MethodAnalyser {

    // analyse method to collect information about blocks
    // TODO: Add logic to record static initializer and finally blocks
    fun analyse(mn: MethodNode): List<Block> {
        val blocks: MutableList<Block> = mutableListOf()
        val jumpTargets: MutableSet<LabelNode> = getJumpTargets(mn.instructions)

        for (each in mn.tryCatchBlocks) {
            jumpTargets.add(each.handler)
        }

        var blockLines: MutableSet<Int?> = mutableSetOf()
        var lastLine = Int.MIN_VALUE
        val lastInstruction = mn.instructions.size() - 1
        var blockStart = 0
        for (i in 0 until mn.instructions.size()) {
            val ins = mn.instructions[i]
            if (ins is LineNumberNode) {
                blockLines.add(ins.line)
                lastLine = ins.line
            } else if (jumpTargets.contains(ins) && blockStart != i) {
                if (blockLines.isEmpty() && blocks.size > 0 && !blocks[blocks.size - 1].getLines().isEmpty()) {
                    blockLines.addAll(blocks[blocks.size - 1].getLines())
                }
                blocks.add(Block(blockStart, i - 1, blockLines))
                blockStart = i
                blockLines = mutableSetOf()
            } else if (blockEnding(ins)) {
                if (blockLines.isEmpty() && blocks.size > 0 && !blocks[blocks.size - 1].getLines().isEmpty()) {
                    blockLines.addAll(blocks[blocks.size - 1].getLines())
                }
                blocks.add(Block(blockStart, i, blockLines))
                blockStart = i + 1
                blockLines = mutableSetOf()
            } else if (!(ins is LabelNode || ins is FrameNode) && lastLine != Int.MIN_VALUE) {
                blockLines.add(lastLine)
            }
        }

        if (blockStart != lastInstruction) {
            blocks.add(Block(blockStart, lastInstruction, blockLines))
        }
        return blocks
    }


    private fun blockEnding(
        ins: AbstractInsnNode
    ): Boolean {
        return (isReturnIns(ins) || ins is JumpInsnNode || (ins.type == AbstractInsnNode.METHOD_INSN))
    }


    private fun isReturnIns(ins: AbstractInsnNode): Boolean {
        when (ins.opcode) {
            Opcodes.RETURN, Opcodes.ARETURN, Opcodes.DRETURN, Opcodes.FRETURN, Opcodes.IRETURN, Opcodes.LRETURN, Opcodes.ATHROW -> return true
        }
        return false
    }

    private fun getJumpTargets(instructions: InsnList): MutableSet<LabelNode> {
        val jumpTargets: MutableSet<LabelNode> = mutableSetOf()
        for (o in instructions) {
            when (o) {
                is JumpInsnNode -> jumpTargets.add(o.label)
                is TableSwitchInsnNode -> {
                    jumpTargets.add(o.dflt)
                    jumpTargets.addAll(o.labels)
                }
                is LookupSwitchInsnNode -> {
                    jumpTargets.add(o.dflt)
                    jumpTargets.addAll(o.labels)
                }
            }
        }
        return jumpTargets
    }
}