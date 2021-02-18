package io.moco.engine

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable


data class MethodName (
    @JsonProperty("methodName") val name: String,
) : Serializable
