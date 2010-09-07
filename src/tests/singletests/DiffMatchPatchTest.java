/*
 * Test harness for DiffMatchPatch.java
 *
 * Copyright 2006 Google Inc.
 * http://code.google.com/p/google-diff-match-patch/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* Changes by jdownloader.org
 * - refaktoring to fit jdownloader.org Naming conventions
 * - changing some visibility modifiers to match new packages
 * - renaming package
 */
package tests.singletests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jd.nutils.DiffMatchPatch;
import jd.nutils.DiffMatchPatch.Diff;
import jd.nutils.DiffMatchPatch.Patch;

import org.junit.Before;
import org.junit.Test;

public class DiffMatchPatchTest {

    private DiffMatchPatch           dmp;
    private DiffMatchPatch.Operation DELETE = DiffMatchPatch.Operation.DELETE;
    private DiffMatchPatch.Operation EQUAL  = DiffMatchPatch.Operation.EQUAL;
    private DiffMatchPatch.Operation INSERT = DiffMatchPatch.Operation.INSERT;

    @Before
    protected void setUp() {
        // Create an instance of the DiffMatchPatch object.
        dmp = new DiffMatchPatch();
    }

    // DIFF TEST FUNCTIONS

    @Test
    public void testDiffCommonPrefix() {
        // Detect and remove any common prefix.
        assertEquals("diffCommonPrefix: Null case.", 0, dmp.diffCommonPrefix("abc", "xyz"));

        assertEquals("diffCommonPrefix: Non-null case.", 4, dmp.diffCommonPrefix("1234abcdef", "1234xyz"));
    }

    @Test
    public void testDiffCommonSuffix() {
        // Detect and remove any common suffix.
        assertEquals("diffCommonSuffix: Null case.", 0, dmp.diffCommonSuffix("abc", "xyz"));

        assertEquals("diffCommonSuffix: Non-null case.", 4, dmp.diffCommonSuffix("abcdef1234", "xyz1234"));
    }

    @Test
    public void testDiffHalfmatch() {
        // Detect a halfmatch.
        assertNull("diffHalfMatch: No match.", dmp.diffHalfMatch("1234567890", "abcdef"));

        assertArrayEquals("diffHalfMatch: Single Match #1.", new String[] { "12", "90", "a", "z", "345678" }, dmp.diffHalfMatch("1234567890", "a345678z"));

        assertArrayEquals("diffHalfMatch: Single Match #2.", new String[] { "a", "z", "12", "90", "345678" }, dmp.diffHalfMatch("a345678z", "1234567890"));

        assertArrayEquals("diffHalfMatch: Multiple Matches #1.", new String[] { "12123", "123121", "a", "z", "1234123451234" }, dmp.diffHalfMatch("121231234123451234123121", "a1234123451234z"));

        assertArrayEquals("diffHalfMatch: Multiple Matches #2.", new String[] { "", "-=-=-=-=-=", "x", "", "x-=-=-=-=-=-=-=" }, dmp.diffHalfMatch("x-=-=-=-=-=-=-=-=-=-=-=-=", "xx-=-=-=-=-=-=-="));

        assertArrayEquals("diffHalfMatch: Multiple Matches #3.", new String[] { "-=-=-=-=-=", "", "", "y", "-=-=-=-=-=-=-=y" }, dmp.diffHalfMatch("-=-=-=-=-=-=-=-=-=-=-=-=y", "-=-=-=-=-=-=-=yy"));
    }

    @Test
    public void testDiffLinesToChars() {
        // Convert lines down to characters
        ArrayList<String> tmpVector = new ArrayList<String>();
        tmpVector.add("");
        tmpVector.add("alpha\n");
        tmpVector.add("beta\n");
        assertArrayEquals("diffLinesToChars:", new Object[] { "\u0001\u0002\u0001", "\u0002\u0001\u0002", tmpVector }, dmp.diffLinesToChars("alpha\nbeta\nalpha\n", "beta\nalpha\nbeta\n"));

        tmpVector.clear();
        tmpVector.add("");
        tmpVector.add("alpha\r\n");
        tmpVector.add("beta\r\n");
        tmpVector.add("\r\n");
        assertArrayEquals("diffLinesToChars:", new Object[] { "", "\u0001\u0002\u0003\u0003", tmpVector }, dmp.diffLinesToChars("", "alpha\r\nbeta\r\n\r\n\r\n"));

        tmpVector.clear();
        tmpVector.add("");
        tmpVector.add("a");
        tmpVector.add("b");
        assertArrayEquals("diffLinesToChars:", new Object[] { "\u0001", "\u0002", tmpVector }, dmp.diffLinesToChars("a", "b"));

        // More than 256
        int n = 300;
        // StringBuilder lineList = new StringBuilder();
        tmpVector.clear();
        StringBuilder lineList = new StringBuilder();
        StringBuilder charList = new StringBuilder();
        for (int x = 1; x < n + 1; x++) {
            tmpVector.add(x + "\n");
            lineList.append(x + "\n");
            charList.append(String.valueOf((char) x));
        }
        assertEquals(n, tmpVector.size());
        String lines = lineList.toString();
        String chars = charList.toString();
        assertEquals(n, chars.length());
        tmpVector.add(0, "");
        assertArrayEquals("diffLinesToChars: More than 256.", new Object[] { chars, "", tmpVector }, dmp.diffLinesToChars(lines, ""));
    }

    @Test
    public void testDiffCharsToLines() {
        // First check that Diff equality works
        assertTrue("diffCharsToLines:", new Diff(EQUAL, "a").equals(new Diff(EQUAL, "a")));

        assertEquals("diffCharsToLines:", new Diff(EQUAL, "a"), new Diff(EQUAL, "a"));

        // Convert chars up to lines
        LinkedList<Diff> diffs = diffList(new Diff(EQUAL, "\u0001\u0002\u0001"), new Diff(INSERT, "\u0002\u0001\u0002"));
        ArrayList<String> tmpVector = new ArrayList<String>();
        tmpVector.add("");
        tmpVector.add("alpha\n");
        tmpVector.add("beta\n");
        dmp.diffCharsToLines(diffs, tmpVector);
        assertEquals("diffCharsToLines:", diffList(new Diff(EQUAL, "alpha\nbeta\nalpha\n"), new Diff(INSERT, "beta\nalpha\nbeta\n")), diffs);

        // More than 256
        int n = 300;
        // StringBuilder lineList = new StringBuilder();
        tmpVector.clear();
        StringBuilder lineList = new StringBuilder();
        StringBuilder charList = new StringBuilder();
        for (int x = 1; x < n + 1; x++) {
            tmpVector.add(x + "\n");
            lineList.append(x + "\n");
            charList.append(String.valueOf((char) x));
        }
        assertEquals(n, tmpVector.size());
        String lines = lineList.toString();
        String chars = charList.toString();
        assertEquals(n, chars.length());
        tmpVector.add(0, "");
        diffs = diffList(new Diff(DELETE, chars));
        dmp.diffCharsToLines(diffs, tmpVector);
        assertEquals("diffCharsToLines: More than 256.", diffList(new Diff(DELETE, lines)), diffs);
    }

    @Test
    public void testDiffCleanupMerge() {
        // Cleanup a messy diff
        LinkedList<Diff> diffs = diffList();
        dmp.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Null case.", diffList(), diffs);

        diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "b"), new Diff(INSERT, "c"));
        dmp.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: No change case.", diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "b"), new Diff(INSERT, "c")), diffs);

        diffs = diffList(new Diff(EQUAL, "a"), new Diff(EQUAL, "b"), new Diff(EQUAL, "c"));
        dmp.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Merge equalities.", diffList(new Diff(EQUAL, "abc")), diffs);

        diffs = diffList(new Diff(DELETE, "a"), new Diff(DELETE, "b"), new Diff(DELETE, "c"));
        dmp.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Merge deletions.", diffList(new Diff(DELETE, "abc")), diffs);

        diffs = diffList(new Diff(INSERT, "a"), new Diff(INSERT, "b"), new Diff(INSERT, "c"));
        dmp.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Merge insertions.", diffList(new Diff(INSERT, "abc")), diffs);

        diffs = diffList(new Diff(DELETE, "a"), new Diff(INSERT, "b"), new Diff(DELETE, "c"), new Diff(INSERT, "d"), new Diff(EQUAL, "e"), new Diff(EQUAL, "f"));
        dmp.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Merge interweave.", diffList(new Diff(DELETE, "ac"), new Diff(INSERT, "bd"), new Diff(EQUAL, "ef")), diffs);

        diffs = diffList(new Diff(DELETE, "a"), new Diff(INSERT, "abc"), new Diff(DELETE, "dc"));
        dmp.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Prefix and suffix detection.", diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "d"), new Diff(INSERT, "b"), new Diff(EQUAL, "c")), diffs);

        diffs = diffList(new Diff(EQUAL, "a"), new Diff(INSERT, "ba"), new Diff(EQUAL, "c"));
        dmp.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Slide edit left.", diffList(new Diff(INSERT, "ab"), new Diff(EQUAL, "ac")), diffs);

        diffs = diffList(new Diff(EQUAL, "c"), new Diff(INSERT, "ab"), new Diff(EQUAL, "a"));
        dmp.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Slide edit right.", diffList(new Diff(EQUAL, "ca"), new Diff(INSERT, "ba")), diffs);

        diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "b"), new Diff(EQUAL, "c"), new Diff(DELETE, "ac"), new Diff(EQUAL, "x"));
        dmp.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Slide edit left recursive.", diffList(new Diff(DELETE, "abc"), new Diff(EQUAL, "acx")), diffs);

        diffs = diffList(new Diff(EQUAL, "x"), new Diff(DELETE, "ca"), new Diff(EQUAL, "c"), new Diff(DELETE, "b"), new Diff(EQUAL, "a"));
        dmp.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Slide edit right recursive.", diffList(new Diff(EQUAL, "xca"), new Diff(DELETE, "cba")), diffs);
    }

    @Test
    public void testDiffCleanupSemanticLossless() {
        // Slide diffs to match logical boundaries
        LinkedList<Diff> diffs = diffList();
        dmp.diffCleanupSemanticLossless(diffs);
        assertEquals("diffCleanupSemanticLossless: Null case.", diffList(), diffs);

        diffs = diffList(new Diff(EQUAL, "AAA\r\n\r\nBBB"), new Diff(INSERT, "\r\nDDD\r\n\r\nBBB"), new Diff(EQUAL, "\r\nEEE"));
        dmp.diffCleanupSemanticLossless(diffs);
        assertEquals("diffCleanupSemanticLossless: Blank lines.", diffList(new Diff(EQUAL, "AAA\r\n\r\n"), new Diff(INSERT, "BBB\r\nDDD\r\n\r\n"), new Diff(EQUAL, "BBB\r\nEEE")), diffs);

        diffs = diffList(new Diff(EQUAL, "AAA\r\nBBB"), new Diff(INSERT, " DDD\r\nBBB"), new Diff(EQUAL, " EEE"));
        dmp.diffCleanupSemanticLossless(diffs);
        assertEquals("diffCleanupSemanticLossless: Line boundaries.", diffList(new Diff(EQUAL, "AAA\r\n"), new Diff(INSERT, "BBB DDD\r\n"), new Diff(EQUAL, "BBB EEE")), diffs);

        diffs = diffList(new Diff(EQUAL, "The c"), new Diff(INSERT, "ow and the c"), new Diff(EQUAL, "at."));
        dmp.diffCleanupSemanticLossless(diffs);
        assertEquals("diffCleanupSemanticLossless: Word boundaries.", diffList(new Diff(EQUAL, "The "), new Diff(INSERT, "cow and the "), new Diff(EQUAL, "cat.")), diffs);

        diffs = diffList(new Diff(EQUAL, "The-c"), new Diff(INSERT, "ow-and-the-c"), new Diff(EQUAL, "at."));
        dmp.diffCleanupSemanticLossless(diffs);
        assertEquals("diffCleanupSemanticLossless: Alphanumeric boundaries.", diffList(new Diff(EQUAL, "The-"), new Diff(INSERT, "cow-and-the-"), new Diff(EQUAL, "cat.")), diffs);

        diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "a"), new Diff(EQUAL, "ax"));
        dmp.diffCleanupSemanticLossless(diffs);
        assertEquals("diffCleanupSemanticLossless: Hitting the start.", diffList(new Diff(DELETE, "a"), new Diff(EQUAL, "aax")), diffs);

        diffs = diffList(new Diff(EQUAL, "xa"), new Diff(DELETE, "a"), new Diff(EQUAL, "a"));
        dmp.diffCleanupSemanticLossless(diffs);
        assertEquals("diffCleanupSemanticLossless: Hitting the end.", diffList(new Diff(EQUAL, "xaa"), new Diff(DELETE, "a")), diffs);
    }

    @Test
    public void testDiffCleanupSemantic() {
        // Cleanup semantically trivial equalities
        LinkedList<Diff> diffs = diffList();
        dmp.diffCleanupSemantic(diffs);
        assertEquals("diffCleanupSemantic: Null case.", diffList(), diffs);

        diffs = diffList(new Diff(DELETE, "a"), new Diff(INSERT, "b"), new Diff(EQUAL, "cd"), new Diff(DELETE, "e"));
        dmp.diffCleanupSemantic(diffs);
        assertEquals("diffCleanupSemantic: No elimination.", diffList(new Diff(DELETE, "a"), new Diff(INSERT, "b"), new Diff(EQUAL, "cd"), new Diff(DELETE, "e")), diffs);

        diffs = diffList(new Diff(DELETE, "a"), new Diff(EQUAL, "b"), new Diff(DELETE, "c"));
        dmp.diffCleanupSemantic(diffs);
        assertEquals("diffCleanupSemantic: Simple elimination.", diffList(new Diff(DELETE, "abc"), new Diff(INSERT, "b")), diffs);

        diffs = diffList(new Diff(DELETE, "ab"), new Diff(EQUAL, "cd"), new Diff(DELETE, "e"), new Diff(EQUAL, "f"), new Diff(INSERT, "g"));
        dmp.diffCleanupSemantic(diffs);
        assertEquals("diffCleanupSemantic: Backpass elimination.", diffList(new Diff(DELETE, "abcdef"), new Diff(INSERT, "cdfg")), diffs);

        diffs = diffList(new Diff(INSERT, "1"), new Diff(EQUAL, "A"), new Diff(DELETE, "B"), new Diff(INSERT, "2"), new Diff(EQUAL, "_"), new Diff(INSERT, "1"), new Diff(EQUAL, "A"), new Diff(DELETE, "B"), new Diff(INSERT, "2"));
        dmp.diffCleanupSemantic(diffs);
        assertEquals("diffCleanupSemantic: Multiple elimination.", diffList(new Diff(DELETE, "AB_AB"), new Diff(INSERT, "1A2_1A2")), diffs);

        diffs = diffList(new Diff(EQUAL, "The c"), new Diff(DELETE, "ow and the c"), new Diff(EQUAL, "at."));
        dmp.diffCleanupSemantic(diffs);
        assertEquals("diffCleanupSemantic: Word boundaries.", diffList(new Diff(EQUAL, "The "), new Diff(DELETE, "cow and the "), new Diff(EQUAL, "cat.")), diffs);
    }

    @Test
    public void testDiffCleanupEfficiency() {
        // Cleanup operationally trivial equalities
        dmp.diffEditCost = 4;
        LinkedList<Diff> diffs = diffList();
        dmp.diffCleanupEfficiency(diffs);
        assertEquals("diffCleanupEfficiency: Null case.", diffList(), diffs);

        diffs = diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "12"), new Diff(EQUAL, "wxyz"), new Diff(DELETE, "cd"), new Diff(INSERT, "34"));
        dmp.diffCleanupEfficiency(diffs);
        assertEquals("diffCleanupEfficiency: No elimination.", diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "12"), new Diff(EQUAL, "wxyz"), new Diff(DELETE, "cd"), new Diff(INSERT, "34")), diffs);

        diffs = diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "12"), new Diff(EQUAL, "xyz"), new Diff(DELETE, "cd"), new Diff(INSERT, "34"));
        dmp.diffCleanupEfficiency(diffs);
        assertEquals("diffCleanupEfficiency: Four-edit elimination.", diffList(new Diff(DELETE, "abxyzcd"), new Diff(INSERT, "12xyz34")), diffs);

        diffs = diffList(new Diff(INSERT, "12"), new Diff(EQUAL, "x"), new Diff(DELETE, "cd"), new Diff(INSERT, "34"));
        dmp.diffCleanupEfficiency(diffs);
        assertEquals("diffCleanupEfficiency: Three-edit elimination.", diffList(new Diff(DELETE, "xcd"), new Diff(INSERT, "12x34")), diffs);

        diffs = diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "12"), new Diff(EQUAL, "xy"), new Diff(INSERT, "34"), new Diff(EQUAL, "z"), new Diff(DELETE, "cd"), new Diff(INSERT, "56"));
        dmp.diffCleanupEfficiency(diffs);
        assertEquals("diffCleanupEfficiency: Backpass elimination.", diffList(new Diff(DELETE, "abxyzcd"), new Diff(INSERT, "12xy34z56")), diffs);

        dmp.diffEditCost = 5;
        diffs = diffList(new Diff(DELETE, "ab"), new Diff(INSERT, "12"), new Diff(EQUAL, "wxyz"), new Diff(DELETE, "cd"), new Diff(INSERT, "34"));
        dmp.diffCleanupEfficiency(diffs);
        assertEquals("diffCleanupEfficiency: High cost elimination.", diffList(new Diff(DELETE, "abwxyzcd"), new Diff(INSERT, "12wxyz34")), diffs);
        dmp.diffEditCost = 4;
    }

    @Test
    public void testDiffPrettyHtml() {
        // Pretty print
        LinkedList<Diff> diffs = diffList(new Diff(EQUAL, "a\n"), new Diff(DELETE, "<B>b</B>"), new Diff(INSERT, "c&d"));
        assertEquals("diffPrettyHtml:", "<SPAN TITLE=\"i=0\">a&para;<BR></SPAN><DEL STYLE=\"background:#FFE6E6;\" TITLE=\"i=2\">&lt;B&gt;b&lt;/B&gt;</DEL><INS STYLE=\"background:#E6FFE6;\" TITLE=\"i=2\">c&amp;d</INS>", dmp.diffPrettyHtml(diffs));
    }

    @Test
    public void testDiffText() {
        // Compute the source and destination texts
        LinkedList<Diff> diffs = diffList(new Diff(EQUAL, "jump"), new Diff(DELETE, "s"), new Diff(INSERT, "ed"), new Diff(EQUAL, " over "), new Diff(DELETE, "the"), new Diff(INSERT, "a"), new Diff(EQUAL, " lazy"));
        assertEquals("diffText1:", "jumps over the lazy", dmp.diffText1(diffs));
        assertEquals("diffText2:", "jumped over a lazy", dmp.diffText2(diffs));
    }

    @Test
    public void testDiffDelta() {
        // Convert a diff into delta string
        LinkedList<Diff> diffs = diffList(new Diff(EQUAL, "jump"), new Diff(DELETE, "s"), new Diff(INSERT, "ed"), new Diff(EQUAL, " over "), new Diff(DELETE, "the"), new Diff(INSERT, "a"), new Diff(EQUAL, " lazy"), new Diff(INSERT, "old dog"));
        String text1 = dmp.diffText1(diffs);
        assertEquals("diffText1: Base text.", "jumps over the lazy", text1);

        String delta = dmp.diffToDelta(diffs);
        assertEquals("diffToDelta:", "=4\t-1\t+ed\t=6\t-3\t+a\t=5\t+old dog", delta);

        // Convert delta string into a diff
        assertEquals("diffFromDelta: Normal.", diffs, dmp.diffFromDelta(text1, delta));

        // Generates error (19 < 20).
        try {
            dmp.diffFromDelta(text1 + "x", delta);
            fail("diffFromDelta: Too long.");
        } catch (IllegalArgumentException ex) {
            // Exception expected.
        }

        // Generates error (19 > 18).
        try {
            dmp.diffFromDelta(text1.substring(1), delta);
            fail("diffFromDelta: Too short.");
        } catch (IllegalArgumentException ex) {
            // Exception expected.
        }

        // Generates error (%c3%xy invalid Unicode).
        try {
            dmp.diffFromDelta("", "+%c3%xy");
            fail("diffFromDelta: Invalid character.");
        } catch (IllegalArgumentException ex) {
            // Exception expected.
        }

        // Test deltas with special characters
        diffs = diffList(new Diff(EQUAL, "\u0680 \000 \t %"), new Diff(DELETE, "\u0681 \001 \n ^"), new Diff(INSERT, "\u0682 \002 \\ |"));
        text1 = dmp.diffText1(diffs);
        assertEquals("diffText1: Unicode text.", "\u0680 \000 \t %\u0681 \001 \n ^", text1);

        delta = dmp.diffToDelta(diffs);
        assertEquals("diffToDelta: Unicode.", "=7\t-7\t+%DA%82 %02 %5C %7C", delta);

        assertEquals("diffFromDelta: Unicode.", diffs, dmp.diffFromDelta(text1, delta));

        // Verify pool of unchanged characters
        diffs = diffList(new Diff(INSERT, "A-Z a-z 0-9 - _ . ! ~ * ' ( ) ; / ? : @ & = + $ , # "));
        String text2 = dmp.diffText2(diffs);
        assertEquals("diffText2: Unchanged characters.", "A-Z a-z 0-9 - _ . ! ~ * \' ( ) ; / ? : @ & = + $ , # ", text2);

        delta = dmp.diffToDelta(diffs);
        assertEquals("diffToDelta: Unchanged characters.", "+A-Z a-z 0-9 - _ . ! ~ * \' ( ) ; / ? : @ & = + $ , # ", delta);

        // Convert delta string into a diff
        assertEquals("diffFromDelta: Unchanged characters.", diffs, dmp.diffFromDelta("", delta));
    }

    @Test
    public void testDiffXIndex() {
        // Translate a location in text1 to text2
        LinkedList<Diff> diffs = diffList(new Diff(DELETE, "a"), new Diff(INSERT, "1234"), new Diff(EQUAL, "xyz"));
        assertEquals("diffXIndex: Translation on equality.", 5, dmp.diffXIndex(diffs, 2));

        diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "1234"), new Diff(EQUAL, "xyz"));
        assertEquals("diffXIndex: Translation on deletion.", 1, dmp.diffXIndex(diffs, 3));
    }

    @Test
    public void testDiffLevenshtein() {
        LinkedList<Diff> diffs = diffList(new Diff(DELETE, "abc"), new Diff(INSERT, "1234"), new Diff(EQUAL, "xyz"));
        assertEquals("Levenshtein with trailing equality.", 4, dmp.diffLevenshtein(diffs));

        diffs = diffList(new Diff(EQUAL, "xyz"), new Diff(DELETE, "abc"), new Diff(INSERT, "1234"));
        assertEquals("Levenshtein with leading equality.", 4, dmp.diffLevenshtein(diffs));

        diffs = diffList(new Diff(DELETE, "abc"), new Diff(EQUAL, "xyz"), new Diff(INSERT, "1234"));
        assertEquals("Levenshtein with middle equality.", 7, dmp.diffLevenshtein(diffs));
    }

    @Test
    public void testDiffPath() {
        // First, check footprints are different.
        assertTrue("diffFootprint:", dmp.diffFootprint(1, 10) != dmp.diffFootprint(10, 1));

        // Single letters
        // Trace a path from back to front.
        List<Set<Long>> vMap;
        Set<Long> rowSet;
        vMap = new ArrayList<Set<Long>>();
        {
            rowSet = new HashSet<Long>();
            rowSet.add(dmp.diffFootprint(0, 0));
            vMap.add(rowSet);
            rowSet = new HashSet<Long>();
            rowSet.add(dmp.diffFootprint(0, 1));
            rowSet.add(dmp.diffFootprint(1, 0));
            vMap.add(rowSet);
            rowSet = new HashSet<Long>();
            rowSet.add(dmp.diffFootprint(0, 2));
            rowSet.add(dmp.diffFootprint(2, 0));
            rowSet.add(dmp.diffFootprint(2, 2));
            vMap.add(rowSet);
            rowSet = new HashSet<Long>();
            rowSet.add(dmp.diffFootprint(0, 3));
            rowSet.add(dmp.diffFootprint(2, 3));
            rowSet.add(dmp.diffFootprint(3, 0));
            rowSet.add(dmp.diffFootprint(4, 3));
            vMap.add(rowSet);
            rowSet = new HashSet<Long>();
            rowSet.add(dmp.diffFootprint(0, 4));
            rowSet.add(dmp.diffFootprint(2, 4));
            rowSet.add(dmp.diffFootprint(4, 0));
            rowSet.add(dmp.diffFootprint(4, 4));
            rowSet.add(dmp.diffFootprint(5, 3));
            vMap.add(rowSet);
            rowSet = new HashSet<Long>();
            rowSet.add(dmp.diffFootprint(0, 5));
            rowSet.add(dmp.diffFootprint(2, 5));
            rowSet.add(dmp.diffFootprint(4, 5));
            rowSet.add(dmp.diffFootprint(5, 0));
            rowSet.add(dmp.diffFootprint(6, 3));
            rowSet.add(dmp.diffFootprint(6, 5));
            vMap.add(rowSet);
            rowSet = new HashSet<Long>();
            rowSet.add(dmp.diffFootprint(0, 6));
            rowSet.add(dmp.diffFootprint(2, 6));
            rowSet.add(dmp.diffFootprint(4, 6));
            rowSet.add(dmp.diffFootprint(6, 6));
            rowSet.add(dmp.diffFootprint(7, 5));
            vMap.add(rowSet);
        }
        LinkedList<Diff> diffs = diffList(new Diff(INSERT, "W"), new Diff(DELETE, "A"), new Diff(EQUAL, "1"), new Diff(DELETE, "B"), new Diff(EQUAL, "2"), new Diff(INSERT, "X"), new Diff(DELETE, "C"), new Diff(EQUAL, "3"), new Diff(DELETE, "D"));
        assertEquals("diffPath1: Single letters.", diffs, dmp.diffPath1(vMap, "A1B2C3D", "W12X3"));

        // Trace a path from front to back.
        vMap.remove(vMap.size() - 1);
        diffs = diffList(new Diff(EQUAL, "4"), new Diff(DELETE, "E"), new Diff(INSERT, "Y"), new Diff(EQUAL, "5"), new Diff(DELETE, "F"), new Diff(EQUAL, "6"), new Diff(DELETE, "G"), new Diff(INSERT, "Z"));
        assertEquals("diffPath2: Single letters.", diffs, dmp.diffPath2(vMap, "4E5F6G", "4Y56Z"));

        // Double letters
        // Trace a path from back to front.
        vMap = new ArrayList<Set<Long>>();
        {
            rowSet = new HashSet<Long>();
            rowSet.add(dmp.diffFootprint(0, 0));
            vMap.add(rowSet);
            rowSet = new HashSet<Long>();
            rowSet.add(dmp.diffFootprint(0, 1));
            rowSet.add(dmp.diffFootprint(1, 0));
            vMap.add(rowSet);
            rowSet = new HashSet<Long>();
            rowSet.add(dmp.diffFootprint(0, 2));
            rowSet.add(dmp.diffFootprint(1, 1));
            rowSet.add(dmp.diffFootprint(2, 0));
            vMap.add(rowSet);
            rowSet = new HashSet<Long>();
            rowSet.add(dmp.diffFootprint(0, 3));
            rowSet.add(dmp.diffFootprint(1, 2));
            rowSet.add(dmp.diffFootprint(2, 1));
            rowSet.add(dmp.diffFootprint(3, 0));
            vMap.add(rowSet);
            rowSet = new HashSet<Long>();
            rowSet.add(dmp.diffFootprint(0, 4));
            rowSet.add(dmp.diffFootprint(1, 3));
            rowSet.add(dmp.diffFootprint(3, 1));
            rowSet.add(dmp.diffFootprint(4, 0));
            rowSet.add(dmp.diffFootprint(4, 4));
            vMap.add(rowSet);
        }
        diffs = diffList(new Diff(INSERT, "WX"), new Diff(DELETE, "AB"), new Diff(EQUAL, "12"));
        assertEquals("diffPath1: Double letters.", diffs, dmp.diffPath1(vMap, "AB12", "WX12"));

        // Trace a path from front to back.
        vMap = new ArrayList<Set<Long>>();
        {
            rowSet = new HashSet<Long>();
            rowSet.add(dmp.diffFootprint(0, 0));
            vMap.add(rowSet);
            rowSet = new HashSet<Long>();
            rowSet.add(dmp.diffFootprint(0, 1));
            rowSet.add(dmp.diffFootprint(1, 0));
            vMap.add(rowSet);
            rowSet = new HashSet<Long>();
            rowSet.add(dmp.diffFootprint(1, 1));
            rowSet.add(dmp.diffFootprint(2, 0));
            rowSet.add(dmp.diffFootprint(2, 4));
            vMap.add(rowSet);
            rowSet = new HashSet<Long>();
            rowSet.add(dmp.diffFootprint(2, 1));
            rowSet.add(dmp.diffFootprint(2, 5));
            rowSet.add(dmp.diffFootprint(3, 0));
            rowSet.add(dmp.diffFootprint(3, 4));
            vMap.add(rowSet);
            rowSet = new HashSet<Long>();
            rowSet.add(dmp.diffFootprint(2, 6));
            rowSet.add(dmp.diffFootprint(3, 5));
            rowSet.add(dmp.diffFootprint(4, 4));
            vMap.add(rowSet);
        }
        diffs = diffList(new Diff(DELETE, "CD"), new Diff(EQUAL, "34"), new Diff(INSERT, "YZ"));
        assertEquals("diffPath2: Double letters.", diffs, dmp.diffPath2(vMap, "CD34", "34YZ"));
    }

    @Test
    public void testDiffMain() {
        // Perform a trivial diff
        LinkedList<Diff> diffs = diffList(new Diff(EQUAL, "abc"));
        assertEquals("diffMain: Null case.", diffs, dmp.diffMain("abc", "abc", false));

        diffs = diffList(new Diff(EQUAL, "ab"), new Diff(INSERT, "123"), new Diff(EQUAL, "c"));
        assertEquals("diffMain: Simple insertion.", diffs, dmp.diffMain("abc", "ab123c", false));

        diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "123"), new Diff(EQUAL, "bc"));
        assertEquals("diffMain: Simple deletion.", diffs, dmp.diffMain("a123bc", "abc", false));

        diffs = diffList(new Diff(EQUAL, "a"), new Diff(INSERT, "123"), new Diff(EQUAL, "b"), new Diff(INSERT, "456"), new Diff(EQUAL, "c"));
        assertEquals("diffMain: Two insertions.", diffs, dmp.diffMain("abc", "a123b456c", false));

        diffs = diffList(new Diff(EQUAL, "a"), new Diff(DELETE, "123"), new Diff(EQUAL, "b"), new Diff(DELETE, "456"), new Diff(EQUAL, "c"));
        assertEquals("diffMain: Two deletions.", diffs, dmp.diffMain("a123b456c", "abc", false));

        // Perform a real diff
        // Switch off the timeout.
        dmp.diffTimeout = 0;
        dmp.diffDualThreshold = 32;
        diffs = diffList(new Diff(DELETE, "a"), new Diff(INSERT, "b"));
        assertEquals("diffMain: Simple case #1.", diffs, dmp.diffMain("a", "b", false));

        diffs = diffList(new Diff(DELETE, "Apple"), new Diff(INSERT, "Banana"), new Diff(EQUAL, "s are a"), new Diff(INSERT, "lso"), new Diff(EQUAL, " fruit."));
        assertEquals("diffMain: Simple case #2.", diffs, dmp.diffMain("Apples are a fruit.", "Bananas are also fruit.", false));

        diffs = diffList(new Diff(DELETE, "a"), new Diff(INSERT, "\u0680"), new Diff(EQUAL, "x"), new Diff(DELETE, "\t"), new Diff(INSERT, "\000"));
        assertEquals("diffMain: Simple case #3.", diffs, dmp.diffMain("ax\t", "\u0680x\000", false));

        diffs = diffList(new Diff(DELETE, "1"), new Diff(EQUAL, "a"), new Diff(DELETE, "y"), new Diff(EQUAL, "b"), new Diff(DELETE, "2"), new Diff(INSERT, "xab"));
        assertEquals("diffMain: Overlap #1.", diffs, dmp.diffMain("1ayb2", "abxab", false));

        diffs = diffList(new Diff(INSERT, "xaxcx"), new Diff(EQUAL, "abc"), new Diff(DELETE, "y"));
        assertEquals("diffMain: Overlap #2.", diffs, dmp.diffMain("abcy", "xaxcxabc", false));

        // Sub-optimal double-ended diff.
        dmp.diffDualThreshold = 2;
        diffs = diffList(new Diff(INSERT, "x"), new Diff(EQUAL, "a"), new Diff(DELETE, "b"), new Diff(INSERT, "x"), new Diff(EQUAL, "c"), new Diff(DELETE, "y"), new Diff(INSERT, "xabc"));
        assertEquals("diffMain: Overlap #3.", diffs, dmp.diffMain("abcy", "xaxcxabc", false));

        dmp.diffDualThreshold = 32;
        dmp.diffTimeout = 0.001f; // 1ms
        String a = "`Twas brillig, and the slithy toves\nDid gyre and gimble in the wabe:\nAll mimsy were the borogoves,\nAnd the mome raths outgrabe.\n";
        String b = "I am the very model of a modern major general,\nI've information vegetable, animal, and mineral,\nI know the kings of England, and I quote the fights historical,\nFrom Marathon to Waterloo, in order categorical.\n";
        // Increase the text lengths by 1024 times to ensure a timeout.
        for (int x = 0; x < 10; x++) {
            a = a + a;
            b = b + b;
        }
        assertNull("diffMain: Timeout.", dmp.diffMap(a, b));
        dmp.diffTimeout = 0;

        // Test the linemode speedup
        // Must be long to pass the 200 char cutoff.
        a = "1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n";
        b = "abcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\n";
        assertEquals("diffMain: Simple.", dmp.diffMain(a, b, true), dmp.diffMain(a, b, false));

        a = "1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n";
        b = "abcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n";
        String[] textsLinemode = diffRebuildTexts(dmp.diffMain(a, b, true));
        String[] textsTextmode = diffRebuildTexts(dmp.diffMain(a, b, false));
        assertArrayEquals("diffMain: Overlap.", textsTextmode, textsLinemode);
    }

    // MATCH TEST FUNCTIONS
    @Test
    public void testMatchAlphabet() {
        // Initialise the bitmasks for Bitap
        Map<Character, Integer> bitmask;
        bitmask = new HashMap<Character, Integer>();
        bitmask.put('a', 4);
        bitmask.put('b', 2);
        bitmask.put('c', 1);
        assertEquals("matchAlphabet: Unique.", bitmask, dmp.matchAlphabet("abc"));

        bitmask = new HashMap<Character, Integer>();
        bitmask.put('a', 37);
        bitmask.put('b', 18);
        bitmask.put('c', 8);
        assertEquals("matchAlphabet: Duplicates.", bitmask, dmp.matchAlphabet("abcaba"));
    }

    @Test
    public void testMatchBitap() {
        // Bitap algorithm
        dmp.matchBalance = 0.5f;
        dmp.matchThreshold = 0.5f;
        dmp.matchMinLength = 100;
        dmp.matchMaxLength = 1000;
        assertEquals("matchBitap: Exact match #1.", 5, dmp.matchBitap("abcdefghijk", "fgh", 5));

        assertEquals("matchBitap: Exact match #2.", 5, dmp.matchBitap("abcdefghijk", "fgh", 0));

        assertEquals("matchBitap: Fuzzy match #1.", 4, dmp.matchBitap("abcdefghijk", "efxhi", 0));

        assertEquals("matchBitap: Fuzzy match #2.", 2, dmp.matchBitap("abcdefghijk", "cdefxyhijk", 5));

        assertEquals("matchBitap: Fuzzy match #3.", -1, dmp.matchBitap("abcdefghijk", "bxy", 1));

        assertEquals("matchBitap: Overflow.", 2, dmp.matchBitap("123456789xx0", "3456789x0", 2));

        dmp.matchThreshold = 0.75f;
        assertEquals("matchBitap: Threshold #1.", 4, dmp.matchBitap("abcdefghijk", "efxyhi", 1));

        dmp.matchThreshold = 0.1f;
        assertEquals("matchBitap: Threshold #2.", 1, dmp.matchBitap("abcdefghijk", "bcdef", 1));

        dmp.matchThreshold = 0.5f;
        assertEquals("matchBitap: Multiple select #1.", 0, dmp.matchBitap("abcdexyzabcde", "abccde", 3));

        assertEquals("matchBitap: Multiple select #2.", 8, dmp.matchBitap("abcdexyzabcde", "abccde", 5));

        dmp.matchBalance = 0.6f; // Strict location, loose accuracy.
        assertEquals("matchBitap: Balance test #1.", -1, dmp.matchBitap("abcdefghijklmnopqrstuvwxyz", "abcdefg", 24));

        assertEquals("matchBitap: Balance test #2.", 0, dmp.matchBitap("abcdefghijklmnopqrstuvwxyz", "abcxdxexfgh", 1));

        dmp.matchBalance = 0.4f; // Strict accuracy, loose location.
        assertEquals("matchBitap: Balance test #3.", 0, dmp.matchBitap("abcdefghijklmnopqrstuvwxyz", "abcdefg", 24));

        assertEquals("matchBitap: Balance test #4.", -1, dmp.matchBitap("abcdefghijklmnopqrstuvwxyz", "abcxdxexfgh", 1));
        dmp.matchBalance = 0.5f;
    }

    @Test
    public void testMatchMain() {
        // Full match
        assertEquals("matchMain: Equality.", 0, dmp.matchMain("abcdef", "abcdef", 1000));

        assertEquals("matchMain: Null text.", -1, dmp.matchMain("", "abcdef", 1));

        assertEquals("matchMain: Null pattern.", 3, dmp.matchMain("abcdef", "", 3));

        assertEquals("matchMain: Exact match.", 3, dmp.matchMain("abcdef", "de", 3));

        dmp.matchThreshold = 0.7f;
        assertEquals("matchMain: Complex match.", 4, dmp.matchMain("I am the very model of a modern major general.", " that berry ", 5));
        dmp.matchThreshold = 0.5f;
    }

    // PATCH TEST FUNCTIONS
    @Test
    public void testPatchObj() {
        // Patch Object
        Patch p = new Patch();
        p.start1 = 20;
        p.start2 = 21;
        p.length1 = 18;
        p.length2 = 17;
        p.diffs = diffList(new Diff(EQUAL, "jump"), new Diff(DELETE, "s"), new Diff(INSERT, "ed"), new Diff(EQUAL, " over "), new Diff(DELETE, "the"), new Diff(INSERT, "a"), new Diff(EQUAL, "\nlaz"));
        String strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n %0Alaz\n";
        assertEquals("Patch: toString.", strp, p.toString());
    }

    @Test
    public void testPatchFromText() {
        assertTrue("patchFromText: #0.", dmp.patchFromText("").isEmpty());

        String strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n %0Alaz\n";
        assertEquals("patchFromText: #1.", strp, dmp.patchFromText(strp).get(0).toString());

        assertEquals("patchFromText: #2.", "@@ -1 +1 @@\n-a\n+b\n", dmp.patchFromText("@@ -1 +1 @@\n-a\n+b\n").get(0).toString());

        assertEquals("patchFromText: #3.", "@@ -1,3 +0,0 @@\n-abc\n", dmp.patchFromText("@@ -1,3 +0,0 @@\n-abc\n").get(0).toString());

        assertEquals("patchFromText: #4.", "@@ -0,0 +1,3 @@\n+abc\n", dmp.patchFromText("@@ -0,0 +1,3 @@\n+abc\n").get(0).toString());

        // Generates error.
        try {
            dmp.patchFromText("Bad\nPatch\n");
            fail("patchFromText: #5.");
        } catch (IllegalArgumentException ex) {
            // Exception expected.
        }
    }

    @Test
    public void testPatchToText() {
        String strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n  laz\n";
        List<Patch> patches;
        patches = dmp.patchFromText(strp);
        assertEquals("patchToText: Single", strp, dmp.patchToText(patches));

        strp = "@@ -1,9 +1,9 @@\n-f\n+F\n oo+fooba\n@@ -7,9 +7,9 @@\n obar\n-,\n+.\n  tes\n";
        patches = dmp.patchFromText(strp);
        assertEquals("patchToText: Dual", strp, dmp.patchToText(patches));
    }

    @Test
    public void testPatchAddContext() {
        dmp.patchMargin = 4;
        Patch p;
        p = dmp.patchFromText("@@ -21,4 +21,10 @@\n-jump\n+somersault\n").get(0);
        dmp.patchAddContext(p, "The quick brown fox jumps over the lazy dog.");
        assertEquals("patchAddContext: Simple case.", "@@ -17,12 +17,18 @@\n fox \n-jump\n+somersault\n s ov\n", p.toString());

        p = dmp.patchFromText("@@ -21,4 +21,10 @@\n-jump\n+somersault\n").get(0);
        dmp.patchAddContext(p, "The quick brown fox jumps.");
        assertEquals("patchAddContext: Not enough trailing context.", "@@ -17,10 +17,16 @@\n fox \n-jump\n+somersault\n s.\n", p.toString());

        p = dmp.patchFromText("@@ -3 +3,2 @@\n-e\n+at\n").get(0);
        dmp.patchAddContext(p, "The quick brown fox jumps.");
        assertEquals("patchAddContext: Not enough leading context.", "@@ -1,7 +1,8 @@\n Th\n-e\n+at\n  qui\n", p.toString());

        p = dmp.patchFromText("@@ -3 +3,2 @@\n-e\n+at\n").get(0);
        dmp.patchAddContext(p, "The quick brown fox jumps.  The quick brown fox crashes.");
        assertEquals("patchAddContext: Ambiguity.", "@@ -1,27 +1,28 @@\n Th\n-e\n+at\n  quick brown fox jumps. \n", p.toString());
    }

    @Test
    public void testPatchMake() {
        LinkedList<Patch> patches;
        String text1 = "The quick brown fox jumps over the lazy dog.";
        String text2 = "That quick brown fox jumped over a lazy dog.";
        String expectedPatch = "@@ -1,8 +1,7 @@\n Th\n-at\n+e\n  qui\n@@ -21,17 +21,18 @@\n jump\n-ed\n+s\n  over \n-a\n+the\n  laz\n";
        // The second patch must be "-21,17 +21,18", not "-22,17 +21,18" due to
        // rolling context.
        patches = dmp.patchMake(text2, text1);
        assertEquals("patchMake: Text2+Text1 inputs", expectedPatch, dmp.patchToText(patches));

        expectedPatch = "@@ -1,11 +1,12 @@\n Th\n-e\n+at\n  quick b\n@@ -22,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n  laz\n";
        patches = dmp.patchMake(text1, text2);
        assertEquals("patchMake: Text1+Text2 inputs", expectedPatch, dmp.patchToText(patches));

        LinkedList<Diff> diffs = dmp.diffMain(text1, text2, false);
        patches = dmp.patchMake(diffs);
        assertEquals("patchMake: Diff input", expectedPatch, dmp.patchToText(patches));

        patches = dmp.patchMake(text1, diffs);
        assertEquals("patchMake: Text1+Diff inputs", expectedPatch, dmp.patchToText(patches));

        patches = dmp.patchMake("`1234567890-=[]\\;',./", "~!@#$%^&*()_+{}|:\"<>?");
        assertEquals("patchToText: Character encoding.", "@@ -1,21 +1,21 @@\n-%601234567890-=%5B%5D%5C;',./\n+~!@#$%25%5E&*()_+%7B%7D%7C:%22%3C%3E?\n", dmp.patchToText(patches));

        diffs = diffList(new Diff(DELETE, "`1234567890-=[]\\;',./"), new Diff(INSERT, "~!@#$%^&*()_+{}|:\"<>?"));
        assertEquals("patchFromText: Character decoding.", diffs, dmp.patchFromText("@@ -1,21 +1,21 @@\n-%601234567890-=%5B%5D%5C;',./\n+~!@#$%25%5E&*()_+%7B%7D%7C:%22%3C%3E?\n").get(0).diffs);

        text1 = "";
        for (int x = 0; x < 100; x++) {
            text1 += "abcdef";
        }
        text2 = text1 + "123";
        expectedPatch = "@@ -573,28 +573,31 @@\n cdefabcdefabcdefabcdefabcdef\n+123\n";
        patches = dmp.patchMake(text1, text2);
        assertEquals("patchMake: Long string with repeats.", expectedPatch, dmp.patchToText(patches));
    }

    @Test
    public void testPatchSplitMax() {
        // Assumes that MatchMaxBits is 32.
        LinkedList<Patch> patches;
        patches = dmp.patchMake("abcdef1234567890123456789012345678901234567890123456789012345678901234567890uvwxyz", "abcdefuvwxyz");
        dmp.patchSplitMax(patches);
        assertEquals("patchSplitMax: #1.", "@@ -3,32 +3,8 @@\n cdef\n-123456789012345678901234\n 5678\n@@ -27,32 +3,8 @@\n cdef\n-567890123456789012345678\n 9012\n@@ -51,30 +3,8 @@\n cdef\n-9012345678901234567890\n uvwx\n", dmp.patchToText(patches));

        patches = dmp.patchMake("1234567890123456789012345678901234567890123456789012345678901234567890", "abc");
        dmp.patchSplitMax(patches);
        assertEquals("patchSplitMax: #2.", "@@ -1,32 +1,4 @@\n-1234567890123456789012345678\n 9012\n@@ -29,32 +1,4 @@\n-9012345678901234567890123456\n 7890\n@@ -57,14 +1,3 @@\n-78901234567890\n+abc\n", dmp.patchToText(patches));

        patches = dmp.patchMake("abcdefghij , h : 0 , t : 1 abcdefghij , h : 0 , t : 1 abcdefghij , h : 0 , t : 1", "abcdefghij , h : 1 , t : 1 abcdefghij , h : 1 , t : 1 abcdefghij , h : 0 , t : 1");
        dmp.patchSplitMax(patches);
        assertEquals("patchSplitMax: #3.", "@@ -2,32 +2,32 @@\n bcdefghij , h : \n-0\n+1\n  , t : 1 abcdef\n@@ -29,32 +29,32 @@\n bcdefghij , h : \n-0\n+1\n  , t : 1 abcdef\n", dmp.patchToText(patches));
    }

    @Test
    public void testPatchAddPadding() {
        LinkedList<Patch> patches;
        patches = dmp.patchMake("", "test");
        assertEquals("patchAddPadding: Both edges full.", "@@ -0,0 +1,4 @@\n+test\n", dmp.patchToText(patches));
        dmp.patchAddPadding(patches);
        assertEquals("patchAddPadding: Both edges full.", "@@ -1,8 +1,12 @@\n %00%01%02%03\n+test\n %00%01%02%03\n", dmp.patchToText(patches));

        patches = dmp.patchMake("XY", "XtestY");
        assertEquals("patchAddPadding: Both edges partial.", "@@ -1,2 +1,6 @@\n X\n+test\n Y\n", dmp.patchToText(patches));
        dmp.patchAddPadding(patches);
        assertEquals("patchAddPadding: Both edges partial.", "@@ -2,8 +2,12 @@\n %01%02%03X\n+test\n Y%00%01%02\n", dmp.patchToText(patches));

        patches = dmp.patchMake("XXXXYYYY", "XXXXtestYYYY");
        assertEquals("patchAddPadding: Both edges none.", "@@ -1,8 +1,12 @@\n XXXX\n+test\n YYYY\n", dmp.patchToText(patches));
        dmp.patchAddPadding(patches);
        assertEquals("patchAddPadding: Both edges none.", "@@ -5,8 +5,12 @@\n XXXX\n+test\n YYYY\n", dmp.patchToText(patches));
    }

    @Test
    public void testPatchApply() {
        LinkedList<Patch> patches;
        patches = dmp.patchMake("The quick brown fox jumps over the lazy dog.", "That quick brown fox jumped over a lazy dog.");
        Object[] results = dmp.patchApply(patches, "The quick brown fox jumps over the lazy dog.");
        boolean[] boolArray = (boolean[]) results[1];
        String resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
        assertEquals("patchApply: Exact match.", "That quick brown fox jumped over a lazy dog.\ttrue\ttrue", resultStr);

        results = dmp.patchApply(patches, "The quick red rabbit jumps over the tired tiger.");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
        assertEquals("patchApply: Partial match.", "That quick red rabbit jumped over a tired tiger.\ttrue\ttrue", resultStr);

        results = dmp.patchApply(patches, "I am the very model of a modern major general.");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
        assertEquals("patchApply: Failed match.", "I am the very model of a modern major general.\tfalse\tfalse", resultStr);

        patches = dmp.patchMake("", "test");
        String patchStr = dmp.patchToText(patches);
        dmp.patchApply(patches, "");
        assertEquals("patchApply: No side effects.", patchStr, dmp.patchToText(patches));

        patches = dmp.patchMake("The quick brown fox jumps over the lazy dog.", "Woof");
        patchStr = dmp.patchToText(patches);
        dmp.patchApply(patches, "The quick brown fox jumps over the lazy dog.");
        assertEquals("patchApply: No side effects with major delete.", patchStr, dmp.patchToText(patches));

        patches = dmp.patchMake("", "test");
        results = dmp.patchApply(patches, "");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0];
        assertEquals("patchApply: Edge exact match.", "test\ttrue", resultStr);

        patches = dmp.patchMake("XY", "XtestY");
        results = dmp.patchApply(patches, "XY");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0];
        assertEquals("patchApply: Near edge exact match.", "XtestY\ttrue", resultStr);

        patches = dmp.patchMake("y", "y123");
        results = dmp.patchApply(patches, "x");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0];
        assertEquals("patchApply: Edge partial match.", "x123\ttrue", resultStr);
    }

    private void assertArrayEquals(String errorMsg, Object[] a, Object[] b) {
        List<Object> listA = Arrays.asList(a);
        List<Object> listB = Arrays.asList(b);
        assertEquals(errorMsg, listA, listB);
    }

    // Construct the two texts which made up the diff originally.
    private static String[] diffRebuildTexts(LinkedList<Diff> diffs) {
        String[] text = { "", "" };
        for (Diff myDiff : diffs) {
            if (myDiff.operation != DiffMatchPatch.Operation.INSERT) {
                text[0] += myDiff.text;
            }
            if (myDiff.operation != DiffMatchPatch.Operation.DELETE) {
                text[1] += myDiff.text;
            }
        }
        return text;
    }

    // Private function for quickly building lists of diffs.
    private static LinkedList<Diff> diffList(Diff... diffs) {
        LinkedList<Diff> myDiffList = new LinkedList<Diff>();
        for (Diff myDiff : diffs) {
            myDiffList.add(myDiff);
        }
        return myDiffList;
    }
}
