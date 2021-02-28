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

package io.moco.utils

import mu.KLogger
import mu.KotlinLogging
import org.apache.maven.plugin.logging.Log
import java.time.LocalDateTime

class MoCoLogger {

    private var devLogger: KLogger? = if (useMvnLog) null else KotlinLogging.logger("[MoCo]")

    @Synchronized
    fun info(m: String) {
        if (useMvnLog) {
            mvnLogger?.info(m)
        } else {
            devLogger?.info { m }
        }
    }

    @Synchronized
    fun infoVerbose(m: String) {
        if (verbose) {
            if (useMvnLog) {
                mvnLogger?.info(m)
            } else {
                devLogger?.info { m }
            }
        }
    }

    @Synchronized
    fun debug(m: String) {
        if (debugEnabled) {
            if (useMvnLog) {
                mvnLogger?.debug(m)
            } else {
                devLogger?.info { "${LocalDateTime.now()} [DEBUG] $m" }
            }
        }
    }

    @Synchronized
    fun warn(m: String) {
        if (useMvnLog) {
            mvnLogger?.warn(m)
        } else {
            devLogger?.warn { m }
        }
    }

    @Synchronized
    fun error(m: String) {
        if (useMvnLog) {
            mvnLogger?.error(m)
        } else {
            devLogger?.error { m }
        }
    }

    companion object {
        var useMvnLog = false
        var mvnLogger: Log? = null
        var debugEnabled = false
        var verbose = true

        fun useMvnLog(logger: Log? = null) {
            useMvnLog = true
            mvnLogger = logger
        }

        fun useKotlinLog() {
            useMvnLog = false
        }
    }
}