package io.moco.engine


class ClassInfo(
    val version: Int, val access: Int, val name: String,
    val signature: String, val superClassName: String, val interfaces: Array<String>
) {
    val isEnum: Boolean
        get() = superClassName == "java/lang/Enum"
}

