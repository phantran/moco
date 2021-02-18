package io.moco.engine.mutation

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.moco.engine.ClassName
import io.moco.engine.MethodName
import java.io.Serializable

data class MutatedMethodLocation(
    @JsonIgnore val className: ClassName?,
    @JsonProperty("methodName") val methodName: MethodName,
    @JsonProperty("methodDescription") val methodDesc: String
) : Serializable