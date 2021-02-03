package io.moco.utils

object ClassLoaderUtil {
    val contextClsLoader: ClassLoader
        get() = Thread.currentThread().contextClassLoader

    val clsLoader: ClassLoader
        get() = ClassLoader.getSystemClassLoader()
}
