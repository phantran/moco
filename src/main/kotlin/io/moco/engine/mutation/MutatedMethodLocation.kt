package io.moco.engine.mutation

import io.moco.engine.ClassName
import io.moco.engine.MethodName

data class MutatedMethodLocation(
    val className: ClassName?,
    val methodName: MethodName,
    val methodDesc: String
)