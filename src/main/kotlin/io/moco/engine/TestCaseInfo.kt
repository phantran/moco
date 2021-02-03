package io.moco.engine


import java.util.*


data class TestCaseInfo(
    val enclosingClass: String?,
    val name: String,
    val time: Int,
    val clsUnderTest: Optional<ClassName?>,
    val coveredBlocks: Int
) {
    fun sameClsUnderTest(targetClass: ClassName?): Boolean {
        return this.clsUnderTest.equals(targetClass)
    }
}