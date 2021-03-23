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

package io.moco.engine

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.moco.utils.ClassLoaderUtil
import io.moco.utils.MoCoLogger
import java.io.Serializable

/**
 * Class name
 *
 * This class wrap class name string. It also contain a Companion Object which can be used to
 * load classes from their names.
 * @property name
 * @constructor Create empty Class name
 */
data class ClassName(@JsonProperty("className") val name: String) : Serializable {

    @JsonIgnore
    private val javaName = name.replace('/', '.')
    
    fun getJavaName(): String {
        return javaName
    }

    companion object {
        private val logger = MoCoLogger()

        fun fromString(cls: String): ClassName {
            val name = cls.replace('.', '/')
            return ClassName(name)
        }

        fun clsNameToClass(clsName: ClassName, loader: ClassLoader? = ClassLoaderUtil.contextClsLoader): Class<*>? {
            return try {
                Class.forName(clsName.javaName, false, loader)
            } catch (ex: Exception) {
                when (ex) {
                    is ClassNotFoundException -> logger.error("Error ClassNotFoundException while loading class $clsName using name")
                    is NoClassDefFoundError -> logger.error("Error NoClassDefFoundError while loading class $clsName using name")
                    is LinkageError -> logger.error("Error LinkageError while loading class $clsName using name")
                    is SecurityException -> logger.error("Error SecurityException while loading class $clsName using name")
                    is ClassFormatError -> logger.error("Error ClassFormatError while loading class $clsName using name")
                }
                null
            }
        }
    }
}