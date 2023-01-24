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

package dorkbox.fsm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Test;

/**
 * @author hankcs
 */
public
class TestDoubleArrayStringTrie {
    private
    DoubleArrayStringTrie<String> buildASimpleDoubleArrayStringTrie() {
        // Collect test data set
        TreeMap<String, String> map = new TreeMap<String, String>();
        String[] keyArray = new String[] {"hers", "his", "she", "he"};
        for (String key : keyArray) {
            map.put(key, key);
        }
        // Build an DoubleArrayStringTrie
        return FiniteStateMachine.INSTANCE.build(map);
    }

    private
    void validateASimpleDoubleArrayStringTrie(DoubleArrayStringTrie<String> acdat) {
        // Test it
        final String text = "uhers";
        acdat.parseText(text, new IHit<String>() {
            @Override
            public
            void hit(int begin, int end, String value) {
                System.out.printf("[%d:%d]=%s\n", begin, end, value);
                assertEquals(text.substring(begin, end), value);
            }
        });
        // Or simply use
        List<Hit<String>> wordList = acdat.parseText(text);
        System.out.println(wordList);
    }

    @Test
    public
    void testBuildAndParseSimply() throws Exception {
        DoubleArrayStringTrie<String> acdat = buildASimpleDoubleArrayStringTrie();
        validateASimpleDoubleArrayStringTrie(acdat);
    }

    @Test
    public
    void testBuildVeryLongWord() throws Exception {
        TreeMap<String, String> map = new TreeMap<String, String>();

        int longWordLength = 20000;

        String word = loadText("dorkbox/fsm/text.txt");
        map.put(word.substring(10, longWordLength), word.substring(10, longWordLength));
        map.put(word.substring(30, 40), null);

        // word = loadText("en/text.txt");
        // map.put(word.substring(10, longWordLength), word.substring(10, longWordLength));
        // map.put(word.substring(30, 40), null);

        // Build an DoubleArrayStringTrie
        DoubleArrayStringTrie<String> acdat = FiniteStateMachine.INSTANCE.build(map);

        List<Hit<String>> result = acdat.parseText(word);

        assertEquals(2, result.size());
        assertEquals(30,
                     result.get(0)
                           .getBegin());
        assertEquals(40,
                     result.get(0)
                           .getEnd());
        assertEquals(10,
                     result.get(1)
                           .getBegin());
        assertEquals(longWordLength,
                     result.get(1)
                           .getEnd());
    }

    @Test
    public
    void testBuildAndParseWithBigFile() throws Exception {
        // Load test data from disk
        Set<String> dictionary = loadDictionary("dorkbox/fsm/dictionary.txt");
        final String text = loadText("dorkbox/fsm/text.txt");
        // You can use any type of Map to hold data
        Map<String, String> map = new TreeMap<String, String>();
//        Map<String, String> map = new HashMap<String, String>();
//        Map<String, String> map = new LinkedHashMap<String, String>();
        for (String key : dictionary) {
            map.put(key, key);
        }
        // Build an DoubleArrayStringTrie
        DoubleArrayStringTrie<String> acdat = FiniteStateMachine.INSTANCE.build(map);
        // Test it
        acdat.parseText(text, new IHit<String>() {
            @Override
            public
            void hit(int begin, int end, String value) {
                assertEquals(text.substring(begin, end), value);
            }
        });
    }

    private static
    class CountHits implements IHitCancellable<String> {
        private int count;
        private boolean countAll;

        CountHits(boolean countAll) {
            this.count = 0;
            this.countAll = countAll;
        }

        public
        int getCount() {
            return count;
        }

        @Override
        public
        boolean hit(int begin, int end, String value) {
            count += 1;
            return countAll;
        }
    }

    @Test
    public
    void testMatches() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("space", 1);
        map.put("keyword", 2);
        map.put("ch", 3);
        DoubleArrayStringTrie<Integer> trie = FiniteStateMachine.INSTANCE.build(map);

        assertTrue(trie.matches("space"));
        assertTrue(trie.matches("keyword"));
        assertTrue(trie.matches("ch"));
        assertTrue(trie.matches("  ch"));
        assertTrue(trie.matches("chkeyword"));
        assertTrue(trie.matches("oooospace2"));
        assertFalse(trie.matches("c"));
        assertFalse(trie.matches(""));
        assertFalse(trie.matches("spac"));
        assertFalse(trie.matches("nothing"));
    }

    @Test
    public
    void testFirstMatch() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("space", 1);
        map.put("keyword", 2);
        map.put("ch", 3);
        DoubleArrayStringTrie<Integer> trie = FiniteStateMachine.INSTANCE.build(map);

        Hit<Integer> hit = trie.findFirst("space");
        assertEquals(0, hit.getBegin());
        assertEquals(5, hit.getEnd());
        assertEquals(1,
                     hit.getValue()
                        .intValue());

        hit = trie.findFirst("a lot of garbage in the space ch");
        assertEquals(24, hit.getBegin());
        assertEquals(29, hit.getEnd());
        assertEquals(1,
                     hit.getValue()
                        .intValue());

        assertNull(trie.findFirst(""));
        assertNull(trie.findFirst("value"));
        assertNull(trie.findFirst("keywork"));
        assertNull(trie.findFirst(" no pace"));
    }

    @Test
    public
    void testCancellation() throws Exception {
        // Collect test data set
        TreeMap<String, String> map = new TreeMap<String, String>();
        String[] keyArray = new String[] {"foo", "bar"};
        for (String key : keyArray) {
            map.put(key, key);
        }
        // Build an DoubleArrayStringTrie
        DoubleArrayStringTrie<String> acdat = FiniteStateMachine.INSTANCE.build(map);
        // count matches
        String haystack = "sfwtfoowercwbarqwrcq";
        CountHits cancellingMatcher = new CountHits(false);
        CountHits countingMatcher = new CountHits(true);
        System.out.println("Testing cancellation");
        acdat.parseText(haystack, cancellingMatcher);
        acdat.parseText(haystack, countingMatcher);
        assertEquals(cancellingMatcher.count, 1);
        assertEquals(countingMatcher.count, 2);
    }

    private
    String loadText(String path) throws IOException {
        StringBuilder sbText = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(Thread.currentThread()
                                                                           .getContextClassLoader()
                                                                           .getResourceAsStream(path), "UTF-8"));
        String line;
        while ((line = br.readLine()) != null) {
            sbText.append(line)
                  .append("\n");
        }
        br.close();

        return sbText.toString();
    }

    private
    Set<String> loadDictionary(String path) throws IOException {
        Set<String> dictionary = new TreeSet<String>();
        BufferedReader br = new BufferedReader(new InputStreamReader(Thread.currentThread()
                                                                           .getContextClassLoader()
                                                                           .getResourceAsStream(path), "UTF-8"));
        String line;
        while ((line = br.readLine()) != null) {
            dictionary.add(line);
        }
        br.close();

        return dictionary;
    }

    @SuppressWarnings("unchecked")
    @Test
    public
    void testSaveAndLoad() throws Exception {
        DoubleArrayStringTrie<String> acdat = buildASimpleDoubleArrayStringTrie();
        final String tmpPath = System.getProperty("java.io.tmpdir")
                                     .replace("\\\\", "/") + "/acdat.tmp";
        System.out.println("Saving acdat to: " + tmpPath);

        ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(Paths.get(tmpPath)));
        out.writeObject(acdat);
        out.close();
        System.out.println("Loading acdat from: " + tmpPath);

        ObjectInputStream in = new ObjectInputStream(Files.newInputStream(Paths.get(tmpPath)));

        acdat = (DoubleArrayStringTrie<String>) in.readObject();
        validateASimpleDoubleArrayStringTrie(acdat);
    }

    @Test
    public
    void testBuildEmptyTrie() {
        TreeMap<String, String> map = new TreeMap<String, String>();
        DoubleArrayStringTrie<String> acdat = FiniteStateMachine.INSTANCE.build(map);
        assertEquals(0, acdat.getSize());
        List<Hit<String>> hits = acdat.parseText("uhers");
        assertEquals(0, hits.size());
    }
}
