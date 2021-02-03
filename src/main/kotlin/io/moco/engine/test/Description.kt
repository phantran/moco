package io.moco.engine.test

import java.io.Serializable


data class Description constructor(val name: String, val testCls: String?) :
    Serializable {
    val testClass: String? = testCls?.intern()

    val qualifiedName: String
        get() = if (testClass != null && testClass != name) {
            "$testClass.$name"
        } else {
            name
        }
}
