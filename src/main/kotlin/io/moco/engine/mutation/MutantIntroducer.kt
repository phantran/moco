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
