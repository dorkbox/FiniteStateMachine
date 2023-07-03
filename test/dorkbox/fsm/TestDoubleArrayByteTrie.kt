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

import dorkbox.fsm.FiniteStateMachine.build
import org.junit.Assert
import org.junit.Test
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * @author hankcs
 */
class TestDoubleArrayByteTrie {
    private fun buildASimpleDoubleArrayByteArrayTrie(): DoubleArrayByteArrayTrie<String> {
        // Collect test data set
        val map = mutableMapOf<ByteArray, String>()
        val keyArray = arrayOf("hers", "his", "she", "he")
        keyArray.forEach { key ->
            map[key.toByteArray()] = key
        }
        // Build an DoubleArrayStringTrie
        return build(map)
    }

    private fun validateASimpleDoubleArrayByteArrayTrie(acdat: DoubleArrayByteArrayTrie<String>) {
        // Test it
        val bytes = "uhers".toByteArray()
        acdat.parseBytes(bytes, object : IHit<String> {
            override fun hit(begin: Int, end: Int, value: String) {
                System.out.printf("[%d:%d]=%s\n", begin, end, value)

                Assert.assertEquals(String(bytes.copyOfRange(begin, end)), value)
            }
        })

        // Or simply use
        val wordList = acdat.parseBytes(bytes)
        println(wordList)
    }

    @Test
    @Throws(Exception::class)
    fun testBuildAndParseSimply() {
        val acdat = buildASimpleDoubleArrayByteArrayTrie()
        validateASimpleDoubleArrayByteArrayTrie(acdat)
    }

    private class CountHits internal constructor(private val countAll: Boolean) : IHitCancellable<String> {
        var count = 0
            private set

        override fun hit(begin: Int, end: Int, value: String): Boolean {
            count += 1
            return countAll
        }
    }

    @Test
    fun testMatches() {
        val map: MutableMap<String, Int> = HashMap()
        map["space"] = 1
        map["keyword"] = 2
        map["ch"] = 3
        val trie = build(map)
        Assert.assertTrue(trie.matches("space"))
        Assert.assertTrue(trie.matches("keyword"))
        Assert.assertTrue(trie.matches("ch"))
        Assert.assertTrue(trie.matches("  ch"))
        Assert.assertTrue(trie.matches("chkeyword"))
        Assert.assertTrue(trie.matches("oooospace2"))
        Assert.assertFalse(trie.matches("c"))
        Assert.assertFalse(trie.matches(""))
        Assert.assertFalse(trie.matches("spac"))
        Assert.assertFalse(trie.matches("nothing"))
    }

    @Test
    fun testFirstMatch() {
        val map: MutableMap<String, Int> = HashMap()
        map["space"] = 1
        map["keyword"] = 2
        map["ch"] = 3
        val trie = build(map)
        var hit = trie.findFirst("space")
        Assert.assertEquals(0, hit!!.begin.toLong())
        Assert.assertEquals(5, hit.end.toLong())
        Assert.assertEquals(
            1, hit.value.toLong()
        )
        hit = trie.findFirst("a lot of garbage in the space ch")
        Assert.assertEquals(24, hit!!.begin.toLong())
        Assert.assertEquals(29, hit.end.toLong())
        Assert.assertEquals(
            1, hit.value.toLong()
        )
        Assert.assertNull(trie.findFirst(""))
        Assert.assertNull(trie.findFirst("value"))
        Assert.assertNull(trie.findFirst("keywork"))
        Assert.assertNull(trie.findFirst(" no pace"))
    }

    @Test
    @Throws(Exception::class)
    fun testCancellation() {
        // Collect test data set
        val map = TreeMap<String, String>()
        val keyArray = arrayOf("foo", "bar")
        for (key in keyArray) {
            map[key] = key
        }
        // Build an DoubleArrayStringTrie
        val acdat = build(map)

        // count matches
        val haystack = "sfwtfoowercwbarqwrcq"
        val cancellingMatcher = CountHits(false)
        val countingMatcher = CountHits(true)

        println("Testing cancellation")
        acdat.parseText(haystack, cancellingMatcher)
        acdat.parseText(haystack, countingMatcher)
        Assert.assertEquals(cancellingMatcher.count.toLong(), 1)
        Assert.assertEquals(countingMatcher.count.toLong(), 2)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    @Throws(Exception::class)
    fun testSaveAndLoad() {
        var acdat = buildASimpleDoubleArrayByteArrayTrie()
        val tmpPath = System.getProperty("java.io.tmpdir").replace("\\\\", "/") + "/acdat.tmp"

        println("Saving acdat to: $tmpPath")
        val out = ObjectOutputStream(Files.newOutputStream(Paths.get(tmpPath)))
        out.writeObject(acdat)
        out.close()
        
        println("Loading acdat from: $tmpPath")
        val `in` = ObjectInputStream(Files.newInputStream(Paths.get(tmpPath)))
        acdat = `in`.readObject() as DoubleArrayByteArrayTrie<String>
        validateASimpleDoubleArrayByteArrayTrie(acdat)
    }

    @Test
    fun testBuildEmptyTrie() {
        val map = TreeMap<String, String>()
        val acdat = build(map)
        Assert.assertEquals(0, acdat.size.toLong())
        val hits = acdat.parseText("uhers")
        Assert.assertEquals(0, hits.size.toLong())
    }
}
