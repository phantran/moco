package io.moco.engine


data class MutatedMethodLocation(
    val className: ClassName?,
    val methodName: MethodName,
    val methodDesc: String
)

