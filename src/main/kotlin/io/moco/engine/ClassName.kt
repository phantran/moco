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
import java.io.Serializable
import java.util.stream.Stream


data class ClassName (@JsonProperty("className") val name: String): Serializable {

    @JsonIgnore
    private val javaName = name.replace('/', '.')

    fun getJavaName(): String {
        return javaName
    }

    companion object {
        fun fromClass(cls: Class<*>): ClassName {
            return fromString(cls.name)
        }

        fun fromString(cls: String): ClassName {
            val name = cls.replace('.', '/')
            return ClassName(name)
        }

        fun strToClsName(): (String) -> ClassName {
            return { cls: String ->
                fromString(
                    cls
                )
            }
        }

        fun clsNameToClass(
            clsName: ClassName, loader: ClassLoader? = ClassLoaderUtil.clsLoader
        ) : Class<*>? {
            try {
                return Class.forName(
                    clsName.javaName, false,
                    loader
                )
            } catch (ex: ClassNotFoundException) {
                //TODO: Log error
                return null
            } catch (ex: NoClassDefFoundError) {
                //TODO: Log error
                return null
            } catch (ex: LinkageError) {
                //TODO: Log error
                return null
            } catch (ex: SecurityException) {
                //TODO: Log error
                return null
            }
        }


        fun nameToClass1(
            loader: ClassLoader? = ClassLoaderUtil.clsLoader
        ): (clsName: ClassName) -> Stream<Class<*>> {
            return { clsName ->
                try {
                    val cls: Class<*> = Class.forName(
                        clsName.javaName, false,
                        loader
                    )
                    Stream.of(cls)
                } catch (ex: ClassNotFoundException) {
                    Stream.empty()
                } catch (ex: NoClassDefFoundError) {
                    Stream.empty()
                } catch (ex: LinkageError) {
                    Stream.empty()
                } catch (ex: SecurityException) {
                    Stream.empty()
                }
            }
        }
    }
}