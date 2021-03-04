/*
 * Copyright (c) 2021. Tran Phan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package io.moco.engine.mutation

import io.moco.engine.ClassName
import io.moco.engine.MoCoAgent
import io.moco.utils.ByteArrayLoader
import io.moco.utils.MoCoLogger


class MutantIntroducer(private val byteArrayLoader: ByteArrayLoader) {

    private lateinit var previousByteArr: ByteArray
    private var previousClass: ClassName? = null
    private var previousLoader: ClassLoader? = null
    val logger = MoCoLogger()
    fun introduce(
        clsName: ClassName?, loader: ClassLoader, byteArr: ByteArray?
    ): Boolean {
        return try {
            if (previousClass != null && previousClass!! != clsName) {
                val cls = Class.forName(
                    previousClass?.name, false,
                    loader
                )
                MoCoAgent.introduceMutant(cls, previousByteArr)
            }
            if (previousClass == null || previousClass!! != clsName) {
                previousByteArr = byteArrayLoader.getByteArray(clsName?.getJavaName())!!
            }
            previousClass = clsName
            previousLoader = loader

            MoCoAgent.introduceMutant(Class.forName(clsName?.getJavaName(), false, loader), byteArr)
        } catch (e: ClassNotFoundException) {
            logger.error("Error while replacing a class under test by its mutant " + clsName?.getJavaName())
            throw e
        }
    }
}
