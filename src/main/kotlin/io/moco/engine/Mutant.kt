package io.moco.engine


import org.gamekins.mutation.operator.Operator
import java.util.*


data class Mutant(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String? = "unknown_source_file",
    val lineOfCode: Int,  // Line number of the mutant
    val description: String,  // Additional information about the mutant
    val mutatedMethodLocation: MutatedMethodLocation, // Location of method that contains this mutant
    val instructionIndices: List<Int?>, // Index of the instructions within mutated method (maybe more than 1)
    val operatorName: String?, // Name of the mutation operator
    //TODO: Add feature for find finally block and static initializer later
    //val block: Int, // block number to determine whether two mutants belong to the same code block
    //val isInFinallyBlock: Boolean, // Ignore code in finally block because they are generated by compiler
    //val isTroublesome: Boolean
) {

    val coveredByTests: MutableList<TestCaseInfo> = mutableListOf()  // Test cases that will be executed for this mutant
    private var byteCode: ByteArray? = null

    fun setByteCode(byteCode: ByteArray) {
        this.byteCode = byteCode
    }

    fun getByteCode(): ByteArray? {
        return this.byteCode
    }

    fun getInstructionIndex(): Int {
        return getFirstIndex() - 1
    }

    fun getFirstIndex(): Int {
        return instructionIndices.iterator().next()!!
    }

    fun addTestCases(tcNames: Collection<TestCaseInfo>) {
        coveredByTests.addAll(tcNames)
    }
}