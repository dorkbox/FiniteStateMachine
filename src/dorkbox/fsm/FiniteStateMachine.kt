/*
 * Copyright 2023 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dorkbox.fsm

import java.util.*

/**
 * Creates a Finite State Machine for very fast string matching.
 *
 * This is a wrapper for DoubleArrayTrie, since that class is awkward to use
 */
object FiniteStateMachine {
    /**
     * Gets the version number.
     */
    const val version = "1.0"

    init {
        // Add this project to the updates system, which verifies this class + UUID + version information
        dorkbox.updates.Updates.add(FiniteStateMachine::class.java, "f3e65e67f9b243c0afcf59d1a3066fee", version)
    }


    fun <V> build(map: Map<String, V>): DoubleArrayStringTrie<V> {
        return DoubleArrayStringTrie(map)
    }

    fun <V> build(map: Map<ByteArray, V>): DoubleArrayByteArrayTrie<V> {
        return DoubleArrayByteArrayTrie(map)
    }

    fun build(strings: List<String>): DoubleArrayStringTrie<Boolean> {
        val map = TreeMap<String, Boolean>()
        for (key in strings) {
            map[key] = java.lang.Boolean.TRUE
        }

        return build(map)
    }

    fun build(strings: List<ByteArray>): DoubleArrayByteArrayTrie<Boolean> {
        val map = HashMap<ByteArray, Boolean>()
        for (key in strings) {
            map[key] = java.lang.Boolean.TRUE
        }

        return build(map)
    }

    fun build(vararg strings: String): DoubleArrayStringTrie<Boolean> {
        val map = HashMap<String, Boolean>()
        for (key in strings) {
            map[key] = java.lang.Boolean.TRUE
        }

        return build(map)
    }

    fun build(vararg strings: ByteArray): DoubleArrayByteArrayTrie<Boolean> {
        val map = HashMap<ByteArray, Boolean>()
        for (key in strings) {
            map[key] = java.lang.Boolean.TRUE
        }

        return build(map)
    }
}
