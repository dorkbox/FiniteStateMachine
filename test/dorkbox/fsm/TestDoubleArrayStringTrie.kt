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
class TestDoubleArrayStringTrie {
    private fun buildASimpleDoubleArrayStringTrie(): DoubleArrayStringTrie<String> {
        // Collect test data set
        val map = TreeMap<String, String>()
        val keyArray = arrayOf("hers", "his", "she", "he")
        for (key in keyArray) {
            map[key] = key
        }
        // Build an DoubleArrayStringTrie
        return build(map)
    }

    private fun validateASimpleDoubleArrayStringTrie(acdat: DoubleArrayStringTrie<String>) {
        // Test it
        val text = "uhers"
        acdat.parseText(text, object : IHit<String> {
            override fun hit(begin: Int, end: Int, value: String) {
                System.out.printf("[%d:%d]=%s\n", begin, end, value)
                Assert.assertEquals(text.substring(begin, end), value)
            }
        })
        // Or simply use
        val wordList = acdat.parseText(text)
        println(wordList)
    }

    @Test
    @Throws(Exception::class)
    fun testBuildAndParseSimply() {
        val acdat = buildASimpleDoubleArrayStringTrie()
        validateASimpleDoubleArrayStringTrie(acdat)
    }

    @Test
    @Throws(Exception::class)
    fun testBuildVeryLongWord() {
        val map = TreeMap<String, String?>()
        val longWordLength = 20000
        val word = loadText("dorkbox/fsm/text.txt")
        map[word.substring(10, longWordLength)] = word.substring(10, longWordLength)
        map[word.substring(30, 40)] = null

        // word = loadText("en/text.txt");
        // map.put(word.substring(10, longWordLength), word.substring(10, longWordLength));
        // map.put(word.substring(30, 40), null);

        // Build an DoubleArrayStringTrie
        val acdat: DoubleArrayStringTrie<String?> = build(map)

        val result = acdat.parseText(word)
        Assert.assertEquals(2, result.size.toLong())
        Assert.assertEquals(
            30, result[0].begin.toLong()
        )
        Assert.assertEquals(
            40, result[0].end.toLong()
        )
        Assert.assertEquals(
            10, result[1].begin.toLong()
        )
        Assert.assertEquals(
            longWordLength.toLong(), result[1].end.toLong()
        )
    }

    @Test
    @Throws(Exception::class)
    fun testBuildAndParseWithBigFile() {
        // Load test data from disk
        val dictionary = loadDictionary("dorkbox/fsm/dictionary.txt")

        val text = loadText("dorkbox/fsm/text.txt")
        // You can use any type of Map to hold data
        val map: MutableMap<String, String> = TreeMap()
        //        Map<String, String> map = new HashMap<String, String>();
//        Map<String, String> map = new LinkedHashMap<String, String>();
        for (key in dictionary) {
            map[key] = key
        }
        // Build an DoubleArrayStringTrie
        val acdat = build(map)

        // Test it
        acdat.parseText(text, object : IHit<String> {
            override fun hit(begin: Int, end: Int, value: String) {
                Assert.assertEquals(text.substring(begin, end), value)
            }
        })
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

    @Throws(IOException::class)
    private fun loadText(path: String): String {
        val sbText = StringBuilder()
        val br = BufferedReader(
            InputStreamReader(
                Thread.currentThread().contextClassLoader.getResourceAsStream(path), "UTF-8"
            )
        )
        var line: String?
        while (br.readLine().also { line = it } != null) {
            sbText.append(line).append("\n")
        }
        br.close()
        return sbText.toString()
    }

    @Throws(IOException::class)
    private fun loadDictionary(path: String): Set<String> {

        val dictionary: MutableSet<String> = TreeSet()
        val br = BufferedReader(
            InputStreamReader(
                Thread.currentThread().contextClassLoader.getResourceAsStream(path), "UTF-8"
            )
        )


        var line: String?
        while (br.readLine().also { line = it } != null) {
            dictionary.add(line!!)
        }
        br.close()
        return dictionary
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    @Throws(Exception::class)
    fun testSaveAndLoad() {
        var acdat = buildASimpleDoubleArrayStringTrie()
        val tmpPath = System.getProperty("java.io.tmpdir").replace("\\\\", "/") + "/acdat.tmp"

        println("Saving acdat to: $tmpPath")
        val out = ObjectOutputStream(Files.newOutputStream(Paths.get(tmpPath)))
        out.writeObject(acdat)
        out.close()

        println("Loading acdat from: $tmpPath")
        val `in` = ObjectInputStream(Files.newInputStream(Paths.get(tmpPath)))
        acdat = `in`.readObject() as DoubleArrayStringTrie<String>
        validateASimpleDoubleArrayStringTrie(acdat)
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
