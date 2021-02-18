package io.moco.engine.mutation

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import io.moco.engine.ClassName
import io.moco.engine.MethodName
import java.io.Serializable


data class MutatedMethodLocation(
    @JsonUnwrapped val className: ClassName?,
    @JsonUnwrapped val methodName: MethodName,
    @JsonProperty("methodDescription") val methodDesc: String
) : Serializable