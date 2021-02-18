package io.moco.engine.mutation

import io.moco.engine.ClassName
import io.moco.engine.MocoAgent
import io.moco.engine.io.ByteArrayLoader


class MutantIntroducer(private val byteArrayLoader: ByteArrayLoader) {

    private lateinit var previousByteArr: ByteArray
    private var previousClass: ClassName? = null
    private var previousLoader: ClassLoader? = null

    fun introduce(
        clsName: ClassName?, loader: ClassLoader, byteArr: ByteArray?
    ): Boolean {
        return try {
            if (previousClass != null && previousClass!! != clsName) {
                val cls = Class.forName(
                    previousClass?.name, false,
                    loader
                )
                MocoAgent.introduceMutant(cls, previousByteArr)
            }
            if (previousClass == null || previousClass!! != clsName) {
                previousByteArr = byteArrayLoader.getByteArray(clsName?.getJavaName())!!
            }
            previousClass = clsName
            previousLoader = loader

            MocoAgent.introduceMutant(Class.forName(clsName?.getJavaName(), false, loader), byteArr)
        } catch (e: ClassNotFoundException) {
            throw e
        }
    }
}
