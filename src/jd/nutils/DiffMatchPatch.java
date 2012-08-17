package jd.nutils;

/*
 * Diff Match and Patch
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Functions for diff, match and patch.
 * Computes the difference between two texts to create a patch.
 * Applies the patch onto another text, allowing for errors.
 * Copyright:
 * @author fraser@google.com (Neil Fraser)
 */

/**
 * Class containing the diff, match and patch methods. Also contains the
 * behaviour settings.
 */
public class DiffMatchPatch {

    // Defaults.
    // Set these on your DiffMatchPatch instance to override the defaults.

    /**
     * Number of seconds to map a diff before giving up. (0 for infinity)
     */
    public float diffTimeout       = 1.0f;
    /**
     * Cost of an empty edit operation in terms of edit characters.
     */
    public short diffEditCost      = 4;
    /**
     * The size beyond which the double-ended diff activates. Double-ending is
     * twice as fast, but less accurate.
     */

    public short diffDualThreshold = 32;
    /**
     * Tweak the relative importance (0.0 = accuracy, 1.0 = proximity)
     */
    public float matchBalance      = 0.5f;
    /**
     * At what point is no match declared (0.0 = perfection, 1.0 = very loose)
     */
    public float matchThreshold    = 0.5f;
    /**
     * The min and max cutoffs used when computing text lengths.
     */
    public int   matchMinLength    = 100;
    public int   matchMaxLength    = 1000;
    /**
     * Chunk size for context length.
     * 
     */
    public short patchMargin       = 4;

    /**
     * The number of bits in an int.
     */
    private int  matchMaxBits      = 32;

    // DIFF FUNCTIONS

    /**
     * - The data structure representing a diff is a Linked list of Diff
     * objects: {Diff(Operation.DELETE, "Hello"), Diff(Operation.INSERT,
     * "Goodbye"), Diff(Operation.EQUAL, " world.")} which means: delete
     * "Hello", add "Goodbye" and keep " world."
     */
    public enum Operation {
        DELETE, INSERT, EQUAL
    }

    /**
     * Find the differences between two texts. Run a faster slightly less
     * optimal diff This method allows the 'checklines' of diffMain() to be
     * optional. Most of the time checklines is wanted, so default to true.
     * 
     * @param text1
     *            Old string to be diffed.
     * @param text2
     *            New string to be diffed.
     * @return Linked List of Diff objects.
     */
    public LinkedList<Diff> diffMain(String text1, String text2) {
        return diffMain(text1, text2, true);
    }

    /**
     * Find the differences between two texts. Simplifies the problem by
     * stripping any common prefix or suffix off the texts before diffing.
     * 
     * @param text1
     *            Old string to be diffed.
     * @param text2
     *            New string to be diffed.
     * @param checklines
     *            Speedup flag. If false, then don't run a line-level diff first
     *            to identify the changed areas. If true, then run a faster
     *            slightly less optimal diff
     * @return Linked List of Diff objects.
     */
    public LinkedList<Diff> diffMain(String text1, String text2, boolean checklines) {
        // Check for equality (speedup)
        LinkedList<Diff> diffs;
        if (text1.equals(text2)) {
            diffs = new LinkedList<Diff>();
            diffs.add(new Diff(Operation.EQUAL, text1));
            return diffs;
        }

        // Trim off common prefix (speedup)
        int commonlength = diffCommonPrefix(text1, text2);
        String commonPrefix = text1.substring(0, commonlength);
        text1 = text1.substring(commonlength);
        text2 = text2.substring(commonlength);

        // Trim off common suffix (speedup)
        commonlength = diffCommonSuffix(text1, text2);
        String commonSuffix = text1.substring(text1.length() - commonlength);
        text1 = text1.substring(0, text1.length() - commonlength);
        text2 = text2.substring(0, text2.length() - commonlength);

        // Compute the diff on the middle block
        diffs = diffCompute(text1, text2, checklines);

        // Restore the prefix and suffix
        if (commonPrefix.length() != 0) {
            diffs.addFirst(new Diff(Operation.EQUAL, commonPrefix));
        }
        if (commonSuffix.length() != 0) {
            diffs.addLast(new Diff(Operation.EQUAL, commonSuffix));
        }

        diffCleanupMerge(diffs);
        return diffs;
    }

    /**
     * Find the differences between two texts. Assumes that the texts do not
     * have any common prefix or suffix.
     * 
     * @param text1
     *            Old string to be diffed.
     * @param text2
     *            New string to be diffed.
     * @param checkLines
     *            Speedup flag. If false, then don't run a line-level diff first
     *            to identify the changed areas. If true, then run a faster
     *            slightly less optimal diff
     * @return Linked List of Diff objects.
     */
    @SuppressWarnings("unchecked")
    protected LinkedList<Diff> diffCompute(String text1, String text2, boolean checkLines) {
        LinkedList<Diff> diffs = new LinkedList<Diff>();

        if (text1.length() == 0) {
            // Just add some text (speedup)
            diffs.add(new Diff(Operation.INSERT, text2));
            return diffs;
        }

        if (text2.length() == 0) {
            // Just delete some text (speedup)
            diffs.add(new Diff(Operation.DELETE, text1));
            return diffs;
        }

        String longtext = text1.length() > text2.length() ? text1 : text2;
        String shorttext = text1.length() > text2.length() ? text2 : text1;
        int i = longtext.indexOf(shorttext);
        if (i != -1) {
            // Shorter text is inside the longer text (speedup)
            Operation op = (text1.length() > text2.length()) ? Operation.DELETE : Operation.INSERT;
            diffs.add(new Diff(op, longtext.substring(0, i)));
            diffs.add(new Diff(Operation.EQUAL, shorttext));
            diffs.add(new Diff(op, longtext.substring(i + shorttext.length())));
            return diffs;
        }
        longtext = null;
        shorttext = null; // Garbage collect

        // Check to see if the problem can be split in two.
        String[] hm = diffHalfMatch(text1, text2);
        if (hm != null) {
            // A half-match was found, sort out the return data.
            String text1A = hm[0];
            String text1B = hm[1];
            String text2A = hm[2];
            String text2B = hm[3];
            String midCommon = hm[4];
            // Send both pairs off for separate processing.
            LinkedList<Diff> diffsA = diffMain(text1A, text2A, checkLines);
            LinkedList<Diff> diffsB = diffMain(text1B, text2B, checkLines);
            // Merge the results.
            diffs = diffsA;
            diffs.add(new Diff(Operation.EQUAL, midCommon));
            diffs.addAll(diffsB);
            return diffs;
        }

        // Perform a real diff.
        if (checkLines && (text1.length() < 100 || text2.length() < 100)) {
            checkLines = false; // Too trivial for the overhead.
        }
        java.util.List<String> lineArray = null;
        if (checkLines) {
            // Scan the text on a line-by-line basis first.
            Object b[] = diffLinesToChars(text1, text2);
            text1 = (String) b[0];
            text2 = (String) b[1];
            // The following Java warning is harmless.
            // Suggestions for how to clear it would be appreciated.
            lineArray = (java.util.List<String>) b[2];
        }

        diffs = diffMap(text1, text2);
        if (diffs == null) {
            // No acceptable result.
            diffs = new LinkedList<Diff>();
            diffs.add(new Diff(Operation.DELETE, text1));
            diffs.add(new Diff(Operation.INSERT, text2));
        }

        if (checkLines) {
            // Convert the diff back to original text.
            diffCharsToLines(diffs, lineArray);
            // Eliminate freak matches (e.g. blank lines)
            diffCleanupSemantic(diffs);

            // Rediff any replacement blocks, this time character-by-character.
            // Add a dummy entry at the end.
            diffs.add(new Diff(Operation.EQUAL, ""));
            int countDelete = 0;
            int count_insert = 0;
            String textDelete = "";
            String text_insert = "";
            ListIterator<Diff> pointer = diffs.listIterator();
            Diff thisDiff = pointer.next();
            while (thisDiff != null) {
                switch (thisDiff.operation) {
                case INSERT:
                    count_insert++;
                    text_insert += thisDiff.text;
                    break;
                case DELETE:
                    countDelete++;
                    textDelete += thisDiff.text;
                    break;
                case EQUAL:
                    // Upon reaching an equality, check for prior redundancies.
                    if (countDelete >= 1 && count_insert >= 1) {
                        // Delete the offending records and add the merged ones.
                        pointer.previous();
                        for (int j = 0; j < countDelete + count_insert; j++) {
                            pointer.previous();
                            pointer.remove();
                        }
                        for (Diff newDiff : diffMain(textDelete, text_insert, false)) {
                            pointer.add(newDiff);
                        }
                    }
                    count_insert = 0;
                    countDelete = 0;
                    textDelete = "";
                    text_insert = "";
                    break;
                }
                thisDiff = pointer.hasNext() ? pointer.next() : null;
            }
            diffs.removeLast(); // Remove the dummy entry at the end.
        }
        return diffs;
    }

    /**
     * Split two texts into a list of strings. Reduce the texts to a string of
     * hashes where each Unicode character represents one line.
     * 
     * @param text1
     *            First string.
     * @param text2
     *            Second string.
     * @return Three element Object array, containing the encoded text1, the
     *         encoded text2 and the List of unique strings. The zeroth element
     *         of the List of unique strings is intentionally blank.
     */
    public Object[] diffLinesToChars(String text1, String text2) {
        List<String> lineArray = new ArrayList<String>();
        Map<String, Integer> lineHash = new HashMap<String, Integer>();
        // e.g. linearray[4] == "Hello\n"
        // e.g. linehash.get("Hello\n") == 4

        // "\x00" is a valid character, but various debuggers don't like it.
        // So we'll insert a junk entry to avoid generating a null character.
        lineArray.add("");

        String chars1 = diffLinesToCharsMunge(text1, lineArray, lineHash);
        String chars2 = diffLinesToCharsMunge(text2, lineArray, lineHash);
        return new Object[] { chars1, chars2, lineArray };
    }

    /**
     * Split a text into a list of strings. Reduce the texts to a string of
     * hashes where each Unicode character represents one line.
     * 
     * @param text
     *            String to encode.
     * @param lineArray
     *            List of unique strings.
     * @param lineHash
     *            Map of strings to indices.
     * @return Encoded string.
     */
    private String diffLinesToCharsMunge(String text, List<String> lineArray, Map<String, Integer> lineHash) {
        int lineStart = 0;
        int lineEnd = -1;
        String line;
        StringBuilder chars = new StringBuilder();
        // Walk the text, pulling out a substring for each line.
        // text.split('\n') would would temporarily double our memory footprint.
        // Modifying text would create many large strings to garbage collect.
        while (lineEnd < text.length() - 1) {
            lineEnd = text.indexOf('\n', lineStart);
            if (lineEnd == -1) {
                lineEnd = text.length() - 1;
            }
            line = text.substring(lineStart, lineEnd + 1);
            lineStart = lineEnd + 1;

            if (lineHash.containsKey(line)) {
                chars.append(String.valueOf((char) (int) lineHash.get(line)));
            } else {
                lineArray.add(line);
                lineHash.put(line, lineArray.size() - 1);
                chars.append(String.valueOf((char) (lineArray.size() - 1)));
            }
        }
        return chars.toString();
    }

    /**
     * Rehydrate the text in a diff from a string of line hashes to real lines
     * of text.
     * 
     * @param diffs
     *            LinkedList of Diff objects.
     * @param lineArray
     *            List of unique strings.
     */
    public void diffCharsToLines(LinkedList<Diff> diffs, List<String> lineArray) {
        StringBuilder text;
        for (Diff diff : diffs) {
            text = new StringBuilder();
            for (int y = 0; y < diff.text.length(); y++) {
                text.append(lineArray.get(diff.text.charAt(y)));
            }
            diff.text = text.toString();
        }
    }

    /**
     * Explore the intersection points between the two texts.
     * 
     * @param text1
     *            Old string to be diffed.
     * @param text2
     *            New string to be diffed.
     * @return LinkedList of Diff objects or null if no diff available.
     */
    public LinkedList<Diff> diffMap(String text1, String text2) {
        long ms_end = System.currentTimeMillis() + (long) (diffTimeout * 1000);
        int maxD = text1.length() + text2.length() - 1;
        boolean doubleEnd = diffDualThreshold * 2 < maxD;
        List<Set<Long>> vMap1 = new ArrayList<Set<Long>>();
        List<Set<Long>> vMap2 = new ArrayList<Set<Long>>();
        Map<Integer, Integer> v1 = new HashMap<Integer, Integer>();
        Map<Integer, Integer> v2 = new HashMap<Integer, Integer>();
        v1.put(1, 0);
        v2.put(1, 0);
        int x, y;
        Long footstep = 0L; // Used to track overlapping paths.
        Map<Long, Integer> footsteps = new HashMap<Long, Integer>();
        boolean done = false;
        // If the total number of characters is odd, then the front path will
        // collide with the reverse path.
        boolean front = ((text1.length() + text2.length()) % 2 == 1);
        for (int d = 0; d < maxD; d++) {
            // Bail out if timeout reached.
            if (diffTimeout > 0 && System.currentTimeMillis() > ms_end) { return null; }

            // Walk the front path one step.
            vMap1.add(new HashSet<Long>()); // Adds at index 'd'.
            for (int k = -d; k <= d; k += 2) {
                if (k == -d || k != d && v1.get(k - 1) < v1.get(k + 1)) {
                    x = v1.get(k + 1);
                } else {
                    x = v1.get(k - 1) + 1;
                }
                y = x - k;
                if (doubleEnd) {
                    footstep = diffFootprint(x, y);
                    if (front && (footsteps.containsKey(footstep))) {
                        done = true;
                    }
                    if (!front) {
                        footsteps.put(footstep, d);
                    }
                }
                while (!done && x < text1.length() && y < text2.length() && text1.charAt(x) == text2.charAt(y)) {
                    x++;
                    y++;
                    if (doubleEnd) {
                        footstep = diffFootprint(x, y);
                        if (front && (footsteps.containsKey(footstep))) {
                            done = true;
                        }
                        if (!front) {
                            footsteps.put(footstep, d);
                        }
                    }
                }
                v1.put(k, x);
                vMap1.get(d).add(diffFootprint(x, y));
                if (x == text1.length() && y == text2.length()) {
                    // Reached the end in single-path mode.
                    return diffPath1(vMap1, text1, text2);
                } else if (done) {
                    // Front path ran over reverse path.
                    vMap2 = vMap2.subList(0, footsteps.get(footstep) + 1);
                    LinkedList<Diff> a = diffPath1(vMap1, text1.substring(0, x), text2.substring(0, y));
                    a.addAll(diffPath2(vMap2, text1.substring(x), text2.substring(y)));
                    return a;
                }
            }

            if (doubleEnd) {
                // Walk the reverse path one step.
                vMap2.add(new HashSet<Long>()); // Adds at index 'd'.
                for (int k = -d; k <= d; k += 2) {
                    if (k == -d || k != d && v2.get(k - 1) < v2.get(k + 1)) {
                        x = v2.get(k + 1);
                    } else {
                        x = v2.get(k - 1) + 1;
                    }
                    y = x - k;
                    footstep = diffFootprint(text1.length() - x, text2.length() - y);
                    if (!front && (footsteps.containsKey(footstep))) {
                        done = true;
                    }
                    if (front) {
                        footsteps.put(footstep, d);
                    }
                    while (!done && x < text1.length() && y < text2.length() && text1.charAt(text1.length() - x - 1) == text2.charAt(text2.length() - y - 1)) {
                        x++;
                        y++;
                        footstep = diffFootprint(text1.length() - x, text2.length() - y);
                        if (!front && (footsteps.containsKey(footstep))) {
                            done = true;
                        }
                        if (front) {
                            footsteps.put(footstep, d);
                        }
                    }
                    v2.put(k, x);
                    vMap2.get(d).add(diffFootprint(x, y));
                    if (done) {
                        // Reverse path ran over front path.
                        vMap1 = vMap1.subList(0, footsteps.get(footstep) + 1);
                        LinkedList<Diff> a = diffPath1(vMap1, text1.substring(0, text1.length() - x), text2.substring(0, text2.length() - y));
                        a.addAll(diffPath2(vMap2, text1.substring(text1.length() - x), text2.substring(text2.length() - y)));
                        return a;
                    }
                }
            }
        }
        // Number of diffs equals number of characters, no commonality at all.
        return null;
    }

    /**
     * Work from the middle back to the start to determine the path.
     * 
     * @param vMap
     *            List of path sets.
     * @param text1
     *            Old string fragment to be diffed.
     * @param text2
     *            New string fragment to be diffed.
     * @return LinkedList of Diff objects.
     */
    public LinkedList<Diff> diffPath1(List<Set<Long>> vMap, String text1, String text2) {
        LinkedList<Diff> path = new LinkedList<Diff>();
        int x = text1.length();
        int y = text2.length();
        Operation last_op = null;
        for (int d = vMap.size() - 2; d >= 0; d--) {
            while (true) {
                if (vMap.get(d).contains(diffFootprint(x - 1, y))) {
                    x--;
                    if (last_op == Operation.DELETE) {
                        path.getFirst().text = text1.charAt(x) + path.getFirst().text;
                    } else {
                        path.addFirst(new Diff(Operation.DELETE, text1.substring(x, x + 1)));
                    }
                    last_op = Operation.DELETE;
                    break;
                } else if (vMap.get(d).contains(diffFootprint(x, y - 1))) {
                    y--;
                    if (last_op == Operation.INSERT) {
                        path.getFirst().text = text2.charAt(y) + path.getFirst().text;
                    } else {
                        path.addFirst(new Diff(Operation.INSERT, text2.substring(y, y + 1)));
                    }
                    last_op = Operation.INSERT;
                    break;
                } else {
                    x--;
                    y--;
                    assert (text1.charAt(x) == text2.charAt(y)) : "No diagonal.  Can't happen. (diffPath1)";
                    if (last_op == Operation.EQUAL) {
                        path.getFirst().text = text1.charAt(x) + path.getFirst().text;
                    } else {
                        path.addFirst(new Diff(Operation.EQUAL, text1.substring(x, x + 1)));
                    }
                    last_op = Operation.EQUAL;
                }
            }
        }
        return path;
    }

    /**
     * Work from the middle back to the end to determine the path.
     * 
     * @param vMap
     *            List of path sets.
     * @param text1
     *            Old string fragment to be diffed.
     * @param text2
     *            New string fragment to be diffed.
     * @return LinkedList of Diff objects.
     */
    public LinkedList<Diff> diffPath2(List<Set<Long>> vMap, String text1, String text2) {
        LinkedList<Diff> path = new LinkedList<Diff>();
        int x = text1.length();
        int y = text2.length();
        Operation last_op = null;
        for (int d = vMap.size() - 2; d >= 0; d--) {
            while (true) {
                if (vMap.get(d).contains(diffFootprint(x - 1, y))) {
                    x--;
                    if (last_op == Operation.DELETE) {
                        path.getLast().text += text1.charAt(text1.length() - x - 1);
                    } else {
                        path.addLast(new Diff(Operation.DELETE, text1.substring(text1.length() - x - 1, text1.length() - x)));
                    }
                    last_op = Operation.DELETE;
                    break;
                } else if (vMap.get(d).contains(diffFootprint(x, y - 1))) {
                    y--;
                    if (last_op == Operation.INSERT) {
                        path.getLast().text += text2.charAt(text2.length() - y - 1);
                    } else {
                        path.addLast(new Diff(Operation.INSERT, text2.substring(text2.length() - y - 1, text2.length() - y)));
                    }
                    last_op = Operation.INSERT;
                    break;
                } else {
                    x--;
                    y--;
                    assert (text1.charAt(text1.length() - x - 1) == text2.charAt(text2.length() - y - 1)) : "No diagonal.  Can't happen. (diffPath2)";
                    if (last_op == Operation.EQUAL) {
                        path.getLast().text += text1.charAt(text1.length() - x - 1);
                    } else {
                        path.addLast(new Diff(Operation.EQUAL, text1.substring(text1.length() - x - 1, text1.length() - x)));
                    }
                    last_op = Operation.EQUAL;
                }
            }
        }
        return path;
    }

    /**
     * Compute a good hash of two integers.
     * 
     * @param x
     *            First int.
     * @param y
     *            Second int.
     * @return A long made up of both ints.
     */
    public Long diffFootprint(int x, int y) {
        // The maximum size for a long is 9,223,372,036,854,775,807
        // The maximum size for an int is 2,147,483,647
        // Two ints fit nicely in one long.
        // The return value is usually destined as a key in a hash, so return an
        // object rather than a primitive, thus skipping an automatic boxing.
        long result = x;
        result = result << 32;
        result += y;
        return result;
    }

    /**
     * Determine the common prefix of two strings
     * 
     * @param text1
     *            First string.
     * @param text2
     *            Second string.
     * @return The number of characters common to the start of each string.
     */
    public int diffCommonPrefix(String text1, String text2) {
        // Performance analysis: http://neil.fraser.name/news/2007/10/09/
        int n = Math.min(text1.length(), text2.length());
        for (int i = 0; i < n; i++) {
            if (text1.charAt(i) != text2.charAt(i)) { return i; }
        }
        return n;
    }

    /**
     * Determine the common suffix of two strings
     * 
     * @param text1
     *            First string.
     * @param text2
     *            Second string.
     * @return The number of characters common to the end of each string.
     */
    public int diffCommonSuffix(String text1, String text2) {
        // Performance analysis: http://neil.fraser.name/news/2007/10/09/
        int n = Math.min(text1.length(), text2.length());
        for (int i = 0; i < n; i++) {
            if (text1.charAt(text1.length() - i - 1) != text2.charAt(text2.length() - i - 1)) { return i; }
        }
        return n;
    }

    /**
     * Do the two texts share a substring which is at least half the length of
     * the longer text?
     * 
     * @param text1
     *            First string.
     * @param text2
     *            Second string.
     * @return Five element String array, containing the prefix of text1, the
     *         suffix of text1, the prefix of text2, the suffix of text2 and the
     *         common middle. Or null if there was no match.
     */
    public String[] diffHalfMatch(String text1, String text2) {
        String longtext = text1.length() > text2.length() ? text1 : text2;
        String shorttext = text1.length() > text2.length() ? text2 : text1;
        if (longtext.length() < 10 || shorttext.length() < 1) { return null; // Pointless.
        }

        // First check if the second quarter is the seed for a half-match.
        String[] hm1 = diffHalfMatchI(longtext, shorttext, (longtext.length() + 3) / 4);
        // Check again based on the third quarter.
        String[] hm2 = diffHalfMatchI(longtext, shorttext, (longtext.length() + 1) / 2);
        String[] hm;
        if (hm1 == null && hm2 == null) {
            return null;
        } else if (hm2 == null) {
            hm = hm1;
        } else if (hm1 == null) {
            hm = hm2;
        } else {
            // Both matched. Select the longest.
            hm = hm1[4].length() > hm2[4].length() ? hm1 : hm2;
        }

        // A half-match was found, sort out the return data.
        if (text1.length() > text2.length()) {
            return hm;
            // return new String[]{hm[0], hm[1], hm[2], hm[3], hm[4]};
        } else {
            return new String[] { hm[2], hm[3], hm[0], hm[1], hm[4] };
        }
    }

    /**
     * Does a substring of shorttext exist within longtext such that the
     * substring is at least half the length of longtext?
     * 
     * @param longtext
     *            Longer string.
     * @param shorttext
     *            Shorter string.
     * @param i
     *            Start index of quarter length substring within longtext.
     * @return Five element String array, containing the prefix of longtext, the
     *         suffix of longtext, the prefix of shorttext, the suffix of
     *         shorttext and the common middle. Or null if there was no match.
     */
    private String[] diffHalfMatchI(String longtext, String shorttext, int i) {
        // Start with a 1/4 length substring at position i as a seed.
        String seed = longtext.substring(i, i + longtext.length() / 4);
        int j = -1;
        String bestCommon = "";
        String bestLongtextA = "", bestLongtextB = "";
        String bestShorttextA = "", bestShorttextB = "";
        while ((j = shorttext.indexOf(seed, j + 1)) != -1) {
            int prefixLength = diffCommonPrefix(longtext.substring(i), shorttext.substring(j));
            int suffixLength = diffCommonSuffix(longtext.substring(0, i), shorttext.substring(0, j));
            if (bestCommon.length() < suffixLength + prefixLength) {
                bestCommon = shorttext.substring(j - suffixLength, j) + shorttext.substring(j, j + prefixLength);
                bestLongtextA = longtext.substring(0, i - suffixLength);
                bestLongtextB = longtext.substring(i + prefixLength);
                bestShorttextA = shorttext.substring(0, j - suffixLength);
                bestShorttextB = shorttext.substring(j + prefixLength);
            }
        }
        if (bestCommon.length() >= longtext.length() / 2) {
            return new String[] { bestLongtextA, bestLongtextB, bestShorttextA, bestShorttextB, bestCommon };
        } else {
            return null;
        }
    }

    /**
     * Reduce the number of edits by eliminating semantically trivial
     * equalities.
     * 
     * @param diffs
     *            LinkedList of Diff objects.
     */
    public void diffCleanupSemantic(LinkedList<Diff> diffs) {
        if (diffs.isEmpty()) { return; }
        boolean changes = false;
        Stack<Diff> equalities = new Stack<Diff>(); // Stack of qualities.
        String lastequality = null; // Always equal to
        // equalities.lastElement().text
        ListIterator<Diff> pointer = diffs.listIterator();
        // Number of characters that changed prior to the equality.
        int lengthChanges1 = 0;
        // Number of characters that changed after the equality.
        int lengthChanges2 = 0;
        Diff thisDiff = pointer.next();
        while (thisDiff != null) {
            if (thisDiff.operation == Operation.EQUAL) {
                // equality found
                equalities.push(thisDiff);
                lengthChanges1 = lengthChanges2;
                lengthChanges2 = 0;
                lastequality = thisDiff.text;
            } else {
                // an insertion or deletion
                lengthChanges2 += thisDiff.text.length();
                if (lastequality != null && (lastequality.length() <= lengthChanges1) && (lastequality.length() <= lengthChanges2)) {
                    // System.out.println("Splitting: '" + lastequality + "'");
                    // Walk back to offending equality.
                    while (thisDiff != equalities.lastElement()) {
                        thisDiff = pointer.previous();
                    }
                    pointer.next();

                    // Replace equality with a delete.
                    pointer.set(new Diff(Operation.DELETE, lastequality));
                    // Insert a corresponding an insert.
                    pointer.add(new Diff(Operation.INSERT, lastequality));

                    equalities.pop(); // Throw away the equality we just
                    // deleted.
                    if (!equalities.empty()) {
                        // Throw away the previous equality (it needs to be
                        // reevaluated).
                        equalities.pop();
                    }
                    if (equalities.empty()) {
                        // There are no previous equalities, walk back to the
                        // start.
                        while (pointer.hasPrevious()) {
                            pointer.previous();
                        }
                    } else {
                        // There is a safe equality we can fall back to.
                        thisDiff = equalities.lastElement();
                        while (thisDiff != pointer.previous()) {
                            // Intentionally empty loop.
                        }
                    }

                    lengthChanges1 = 0; // Reset the counters.
                    lengthChanges2 = 0;
                    lastequality = null;
                    changes = true;
                }
            }
            thisDiff = pointer.hasNext() ? pointer.next() : null;
        }

        if (changes) {
            diffCleanupMerge(diffs);
        }
        diffCleanupSemanticLossless(diffs);
    }

    /**
     * Look for single edits surrounded on both sides by equalities which can be
     * shifted sideways to align the edit to a word boundary. e.g: The c<ins>at
     * c</ins>ame. -> The <ins>cat </ins>came.
     * 
     * @param diffs
     *            LinkedList of Diff objects.
     */
    public void diffCleanupSemanticLossless(LinkedList<Diff> diffs) {
        String equality1, edit, equality2;
        String commonString;
        int commonOffset;
        int score, bestScore;
        String bestEquality1, bestEdit, bestEquality2;
        // Create a new iterator at the start.
        ListIterator<Diff> pointer = diffs.listIterator();
        Diff prevDiff = pointer.hasNext() ? pointer.next() : null;
        Diff thisDiff = pointer.hasNext() ? pointer.next() : null;
        Diff nextDiff = pointer.hasNext() ? pointer.next() : null;
        // Intentionally ignore the first and last element (don't need
        // checking).
        while (nextDiff != null) {
            if (prevDiff.operation == Operation.EQUAL && nextDiff.operation == Operation.EQUAL) {
                // This is a single edit surrounded by equalities.
                equality1 = prevDiff.text;
                edit = thisDiff.text;
                equality2 = nextDiff.text;

                // First, shift the edit as far left as possible.
                commonOffset = diffCommonSuffix(equality1, edit);
                if (commonOffset != 0) {
                    commonString = edit.substring(edit.length() - commonOffset);
                    equality1 = equality1.substring(0, equality1.length() - commonOffset);
                    edit = commonString + edit.substring(0, edit.length() - commonOffset);
                    equality2 = commonString + equality2;
                }

                // Second, step character by character right, looking for the
                // best fit.
                bestEquality1 = equality1;
                bestEdit = edit;
                bestEquality2 = equality2;
                bestScore = diffCleanupSemanticScore(equality1, edit) + diffCleanupSemanticScore(edit, equality2);
                while (edit.length() != 0 && equality2.length() != 0 && edit.charAt(0) == equality2.charAt(0)) {
                    equality1 += edit.charAt(0);
                    edit = edit.substring(1) + equality2.charAt(0);
                    equality2 = equality2.substring(1);
                    score = diffCleanupSemanticScore(equality1, edit) + diffCleanupSemanticScore(edit, equality2);
                    // The >= encourages trailing rather than leading whitespace
                    // on edits.
                    if (score >= bestScore) {
                        bestScore = score;
                        bestEquality1 = equality1;
                        bestEdit = edit;
                        bestEquality2 = equality2;
                    }
                }

                if (!prevDiff.text.equals(bestEquality1)) {
                    // We have an improvement, save it back to the diff.
                    if (bestEquality1.length() != 0) {
                        prevDiff.text = bestEquality1;
                    } else {
                        pointer.previous(); // Walk past nextDiff.
                        pointer.previous(); // Walk past thisDiff.
                        pointer.previous(); // Walk past prevDiff.
                        pointer.remove(); // Delete prevDiff.
                        pointer.next(); // Walk past thisDiff.
                        pointer.next(); // Walk past nextDiff.
                    }
                    thisDiff.text = bestEdit;
                    if (bestEquality2.length() != 0) {
                        nextDiff.text = bestEquality2;
                    } else {
                        pointer.remove(); // Delete nextDiff.
                        nextDiff = thisDiff;
                        thisDiff = prevDiff;
                    }
                }
            }
            prevDiff = thisDiff;
            thisDiff = nextDiff;
            nextDiff = pointer.hasNext() ? pointer.next() : null;
        }
    }

    /**
     * Given two strings, compute a score representing whether the internal
     * boundary falls on logical boundaries. Scores range from 5 (best) to 0
     * (worst).
     * 
     * @param one
     *            First string.
     * @param two
     *            Second string.
     * @return The score.
     */
    private int diffCleanupSemanticScore(String one, String two) {
        if (one.length() == 0 || two.length() == 0) {
            // Edges are the best.
            return 5;
        }

        // Each port of this function behaves slightly differently due to
        // subtle differences in each language's definition of things like
        // 'whitespace'. Since this function's purpose is largely cosmetic,
        // the choice has been made to use each language's native features
        // rather than force total conformity.
        int score = 0;
        // One point for non-alphanumeric.
        if (!Character.isLetterOrDigit(one.charAt(one.length() - 1)) || !Character.isLetterOrDigit(two.charAt(0))) {
            score++;
            // Two points for whitespace.
            if (Character.isWhitespace(one.charAt(one.length() - 1)) || Character.isWhitespace(two.charAt(0))) {
                score++;
                // Three points for line breaks.
                if (Character.getType(one.charAt(one.length() - 1)) == Character.CONTROL || Character.getType(two.charAt(0)) == Character.CONTROL) {
                    score++;
                    // Four points for blank lines.
                    if (BLANKLINEEND.matcher(one).find() || BLANKLINESTART.matcher(two).find()) {
                        score++;
                    }
                }
            }
        }
        return score;
    }

    private Pattern BLANKLINEEND   = Pattern.compile("\\n\\r?\\n\\Z", Pattern.DOTALL);
    private Pattern BLANKLINESTART = Pattern.compile("\\A\\r?\\n\\r?\\n", Pattern.DOTALL);

    /**
     * Reduce the number of edits by eliminating operationally trivial
     * equalities.
     * 
     * @param diffs
     *            LinkedList of Diff objects.
     */
    public void diffCleanupEfficiency(LinkedList<Diff> diffs) {
        if (diffs.isEmpty()) { return; }
        boolean changes = false;
        Stack<Diff> equalities = new Stack<Diff>(); // Stack of equalities.
        String lastequality = null; // Always equal to
        // equalities.lastElement().text
        ListIterator<Diff> pointer = diffs.listIterator();
        // Is there an insertion operation before the last equality.
        boolean pre_ins = false;
        // Is there a deletion operation before the last equality.
        boolean preDel = false;
        // Is there an insertion operation after the last equality.
        boolean post_ins = false;
        // Is there a deletion operation after the last equality.
        boolean postDel = false;
        Diff thisDiff = pointer.next();
        Diff safeDiff = thisDiff; // The last Diff that is known to be
        // unsplitable.
        while (thisDiff != null) {
            if (thisDiff.operation == Operation.EQUAL) {
                // equality found
                if (thisDiff.text.length() < diffEditCost && (post_ins || postDel)) {
                    // Candidate found.
                    equalities.push(thisDiff);
                    pre_ins = post_ins;
                    preDel = postDel;
                    lastequality = thisDiff.text;
                } else {
                    // Not a candidate, and can never become one.
                    equalities.clear();
                    lastequality = null;
                    safeDiff = thisDiff;
                }
                post_ins = postDel = false;
            } else {
                // an insertion or deletion
                if (thisDiff.operation == Operation.DELETE) {
                    postDel = true;
                } else {
                    post_ins = true;
                }
                /*
                 * Five types to be split:
                 * <ins>A</ins><del>B</del>XY<ins>C</ins><del>D</del>
                 * <ins>A</ins>X<ins>C</ins><del>D</del>
                 * <ins>A</ins><del>B</del>X<ins>C</ins>
                 * <ins>A</del>X<ins>C</ins><del>D</del>
                 * <ins>A</ins><del>B</del>X<del>C</del>
                 */
                if (lastequality != null && ((pre_ins && preDel && post_ins && postDel) || ((lastequality.length() < diffEditCost / 2) && ((pre_ins ? 1 : 0) + (preDel ? 1 : 0) + (post_ins ? 1 : 0) + (postDel ? 1 : 0)) == 3))) {
                    // System.out.println("Splitting: '" + lastequality + "'");
                    // Walk back to offending equality.
                    while (thisDiff != equalities.lastElement()) {
                        thisDiff = pointer.previous();
                    }
                    pointer.next();

                    // Replace equality with a delete.
                    pointer.set(new Diff(Operation.DELETE, lastequality));
                    // Insert a corresponding an insert.
                    pointer.add(thisDiff = new Diff(Operation.INSERT, lastequality));

                    equalities.pop(); // Throw away the equality we just
                    // deleted.
                    lastequality = null;
                    if (pre_ins && preDel) {
                        // No changes made which could affect previous entry,
                        // keep going.
                        post_ins = postDel = true;
                        equalities.clear();
                        safeDiff = thisDiff;
                    } else {
                        if (!equalities.empty()) {
                            // Throw away the previous equality (it needs to be
                            // reevaluated).
                            equalities.pop();
                        }
                        if (equalities.empty()) {
                            // There are no previous questionable equalities,
                            // walk back to the last known safe diff.
                            thisDiff = safeDiff;
                        } else {
                            // There is an equality we can fall back to.
                            thisDiff = equalities.lastElement();
                        }
                        while (thisDiff != pointer.previous()) {
                            // Intentionally empty loop.
                        }
                        post_ins = postDel = false;
                    }

                    changes = true;
                }
            }
            thisDiff = pointer.hasNext() ? pointer.next() : null;
        }

        if (changes) {
            diffCleanupMerge(diffs);
        }
    }

    /**
     * Reorder and merge like edit sections. Merge equalities. Any edit section
     * can move as long as it doesn't cross an equality.
     * 
     * @param diffs
     *            LinkedList of Diff objects.
     */
    public void diffCleanupMerge(LinkedList<Diff> diffs) {
        diffs.add(new Diff(Operation.EQUAL, "")); // Add a dummy entry at the
        // end.
        ListIterator<Diff> pointer = diffs.listIterator();
        int countDelete = 0;
        int count_insert = 0;
        String textDelete = "";
        String text_insert = "";
        Diff thisDiff = pointer.next();
        Diff prevEqual = null;
        int commonlength;
        while (thisDiff != null) {
            switch (thisDiff.operation) {
            case INSERT:
                count_insert++;
                text_insert += thisDiff.text;
                prevEqual = null;
                break;
            case DELETE:
                countDelete++;
                textDelete += thisDiff.text;
                prevEqual = null;
                break;
            case EQUAL:
                if (countDelete != 0 || count_insert != 0) {
                    // Delete the offending records.
                    pointer.previous(); // Reverse direction.
                    while (countDelete-- > 0) {
                        pointer.previous();
                        pointer.remove();
                    }
                    while (count_insert-- > 0) {
                        pointer.previous();
                        pointer.remove();
                    }
                    if (countDelete != 0 && count_insert != 0) {
                        // Factor out any common prefixies.
                        commonlength = diffCommonPrefix(text_insert, textDelete);
                        if (commonlength != 0) {
                            if (pointer.hasPrevious()) {
                                thisDiff = pointer.previous();
                                assert thisDiff.operation == Operation.EQUAL : "Previous diff should have been an equality.";
                                thisDiff.text += text_insert.substring(0, commonlength);
                                pointer.next();
                            } else {
                                pointer.add(new Diff(Operation.EQUAL, text_insert.substring(0, commonlength)));
                            }
                            text_insert = text_insert.substring(commonlength);
                            textDelete = textDelete.substring(commonlength);
                        }
                        // Factor out any common suffixies.
                        commonlength = diffCommonSuffix(text_insert, textDelete);
                        if (commonlength != 0) {
                            thisDiff = pointer.next();
                            thisDiff.text = text_insert.substring(text_insert.length() - commonlength) + thisDiff.text;
                            text_insert = text_insert.substring(0, text_insert.length() - commonlength);
                            textDelete = textDelete.substring(0, textDelete.length() - commonlength);
                            pointer.previous();
                        }
                    }
                    // Insert the merged records.
                    if (textDelete.length() != 0) {
                        pointer.add(new Diff(Operation.DELETE, textDelete));
                    }
                    if (text_insert.length() != 0) {
                        pointer.add(new Diff(Operation.INSERT, text_insert));
                    }
                    // Step forward to the equality.
                    thisDiff = pointer.hasNext() ? pointer.next() : null;
                } else if (prevEqual != null) {
                    // Merge this equality with the previous one.
                    prevEqual.text += thisDiff.text;
                    pointer.remove();
                    thisDiff = pointer.previous();
                    pointer.next(); // Forward direction
                }
                count_insert = 0;
                countDelete = 0;
                textDelete = "";
                text_insert = "";
                prevEqual = thisDiff;
                break;
            }
            thisDiff = pointer.hasNext() ? pointer.next() : null;
        }
        // System.out.println(diff);
        if (diffs.getLast().text.length() == 0) {
            diffs.removeLast(); // Remove the dummy entry at the end.
        }

        /*
         * Second pass: look for single edits surrounded on both sides by
         * equalities which can be shifted sideways to eliminate an equality.
         * e.g: A<ins>BA</ins>C -> <ins>AB</ins>AC
         */
        boolean changes = false;
        // Create a new iterator at the start.
        // (As opposed to walking the current one back.)
        pointer = diffs.listIterator();
        Diff prevDiff = pointer.hasNext() ? pointer.next() : null;
        thisDiff = pointer.hasNext() ? pointer.next() : null;
        Diff nextDiff = pointer.hasNext() ? pointer.next() : null;
        // Intentionally ignore the first and last element (don't need
        // checking).
        while (nextDiff != null) {
            if (prevDiff.operation == Operation.EQUAL && nextDiff.operation == Operation.EQUAL) {
                // This is a single edit surrounded by equalities.
                if (thisDiff.text.endsWith(prevDiff.text)) {
                    // Shift the edit over the previous equality.
                    thisDiff.text = prevDiff.text + thisDiff.text.substring(0, thisDiff.text.length() - prevDiff.text.length());
                    nextDiff.text = prevDiff.text + nextDiff.text;
                    pointer.previous(); // Walk past nextDiff.
                    pointer.previous(); // Walk past thisDiff.
                    pointer.previous(); // Walk past prevDiff.
                    pointer.remove(); // Delete prevDiff.
                    pointer.next(); // Walk past thisDiff.
                    thisDiff = pointer.next(); // Walk past nextDiff.
                    nextDiff = pointer.hasNext() ? pointer.next() : null;
                    changes = true;
                } else if (thisDiff.text.startsWith(nextDiff.text)) {
                    // Shift the edit over the next equality.
                    prevDiff.text += nextDiff.text;
                    thisDiff.text = thisDiff.text.substring(nextDiff.text.length()) + nextDiff.text;
                    pointer.remove(); // Delete nextDiff.
                    nextDiff = pointer.hasNext() ? pointer.next() : null;
                    changes = true;
                }
            }
            prevDiff = thisDiff;
            thisDiff = nextDiff;
            nextDiff = pointer.hasNext() ? pointer.next() : null;
        }
        // If shifts were made, the diff needs reordering and another shift
        // sweep.
        if (changes) {
            diffCleanupMerge(diffs);
        }
    }

    /**
     * loc is a location in text1, compute and return the equivalent location in
     * text2. e.g. "The cat" vs "The big cat", 1->1, 5->8
     * 
     * @param diffs
     *            LinkedList of Diff objects.
     * @param loc
     *            Location within text1.
     * @return Location within text2.
     */
    public int diffXIndex(LinkedList<Diff> diffs, int loc) {
        int chars1 = 0;
        int chars2 = 0;
        int lastChars1 = 0;
        int lastChars2 = 0;
        Diff lastDiff = null;
        for (Diff aDiff : diffs) {
            if (aDiff.operation != Operation.INSERT) {
                // Equality or deletion.
                chars1 += aDiff.text.length();
            }
            if (aDiff.operation != Operation.DELETE) {
                // Equality or insertion.
                chars2 += aDiff.text.length();
            }
            if (chars1 > loc) {
                // Overshot the location.
                lastDiff = aDiff;
                break;
            }
            lastChars1 = chars1;
            lastChars2 = chars2;
        }
        if (lastDiff != null && lastDiff.operation == Operation.DELETE) {
            // The location was deleted.
            return lastChars2;
        }
        // Add the remaining character length.
        return lastChars2 + (loc - lastChars1);
    }

    /**
     * Convert a Diff list into a pretty HTML report.
     * 
     * @param diffs
     *            LinkedList of Diff objects.
     * @return HTML representation.
     */
    public String diffPrettyHtml(LinkedList<Diff> diffs) {
        StringBuilder html = new StringBuilder();
        int i = 0;
        for (Diff aDiff : diffs) {
            String text = aDiff.text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "&para;<BR>");
            switch (aDiff.operation) {
            case INSERT:
                html.append("<INS STYLE=\"background:#E6FFE6;\" TITLE=\"i=").append(i).append("\">").append(text).append("</INS>");
                break;
            case DELETE:
                html.append("<DEL STYLE=\"background:#FFE6E6;\" TITLE=\"i=").append(i).append("\">").append(text).append("</DEL>");
                break;
            case EQUAL:
                html.append("<SPAN TITLE=\"i=").append(i).append("\">").append(text).append("</SPAN>");
                break;
            }
            if (aDiff.operation != Operation.DELETE) {
                i += aDiff.text.length();
            }
        }
        return html.toString();
    }

    /**
     * Compute and return the source text (all equalities and deletions).
     * 
     * @param diffs
     *            LinkedList of Diff objects.
     * @return Source text.
     */
    public String diffText1(LinkedList<Diff> diffs) {
        StringBuilder text = new StringBuilder();
        for (Diff aDiff : diffs) {
            if (aDiff.operation != Operation.INSERT) {
                text.append(aDiff.text);
            }
        }
        return text.toString();
    }

    /**
     * Compute and return the destination text (all equalities and insertions).
     * 
     * @param diffs
     *            LinkedList of Diff objects.
     * @return Destination text.
     */
    public String diffText2(LinkedList<Diff> diffs) {
        StringBuilder text = new StringBuilder();
        for (Diff aDiff : diffs) {
            if (aDiff.operation != Operation.DELETE) {
                text.append(aDiff.text);
            }
        }
        return text.toString();
    }

    /**
     * Compute the Levenshtein distance; the number of inserted, deleted or
     * substituted characters.
     * 
     * @param diffs
     *            LinkedList of Diff objects.
     * @return Number of changes.
     */
    public int diffLevenshtein(LinkedList<Diff> diffs) {
        int levenshtein = 0;
        int insertions = 0;
        int deletions = 0;
        for (Diff aDiff : diffs) {
            switch (aDiff.operation) {
            case INSERT:
                insertions += aDiff.text.length();
                break;
            case DELETE:
                deletions += aDiff.text.length();
                break;
            case EQUAL:
                // A deletion and an insertion is one substitution.
                levenshtein += Math.max(insertions, deletions);
                insertions = 0;
                deletions = 0;
                break;
            }
        }
        levenshtein += Math.max(insertions, deletions);
        return levenshtein;
    }

    /**
     * Crush the diff into an encoded string which describes the operations
     * required to transform text1 into text2. E.g. =3\t-2\t+ing -> Keep 3
     * chars, delete 2 chars, insert 'ing'. Operations are tab-separated.
     * Inserted text is escaped using %xx notation.
     * 
     * @param diffs
     *            Array of diff tuples.
     * @return Delta text.
     */
    public String diffToDelta(LinkedList<Diff> diffs) {
        StringBuilder text = new StringBuilder();
        for (Diff aDiff : diffs) {
            switch (aDiff.operation) {
            case INSERT:
                try {
                    text.append("+").append(URLEncoder.encode(aDiff.text, "UTF-8").replace('+', ' ')).append("\t");
                } catch (UnsupportedEncodingException e) {
                    // Not likely on modern system.
                    throw new Error("This system does not support UTF-8.", e);
                }
                break;
            case DELETE:
                text.append("-").append(aDiff.text.length()).append("\t");
                break;
            case EQUAL:
                text.append("=").append(aDiff.text.length()).append("\t");
                break;
            }
        }
        String delta = text.toString();
        if (delta.length() != 0) {
            // Strip off trailing tab character.
            delta = delta.substring(0, delta.length() - 1);
            delta = unescapeForEncodeUriCompatability(delta);
        }
        return delta;
    }

    /**
     * Given the original text1, and an encoded string which describes the
     * operations required to transform text1 into text2, compute the full diff.
     * 
     * @param text1
     *            Source string for the diff.
     * @param delta
     *            Delta text.
     * @return Array of diff tuples or null if invalid.
     * @throw IllegalArgumentException If invalid input.
     */
    public LinkedList<Diff> diffFromDelta(String text1, String delta) throws IllegalArgumentException {
        LinkedList<Diff> diffs = new LinkedList<Diff>();
        int pointer = 0; // Cursor in text1
        String[] tokens = delta.split("\t");
        for (String token : tokens) {
            if (token.length() == 0) {
                // Blank tokens are ok (from a trailing \t).
                continue;
            }
            // Each token begins with a one character parameter which specifies
            // the
            // operation of this token (delete, insert, equality).
            String param = token.substring(1);
            switch (token.charAt(0)) {
            case '+':
                // decode would change all "+" to " "
                param = param.replace("+", "%2B");
                try {
                    param = URLDecoder.decode(param, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // Not likely on modern system.
                    throw new Error("This system does not support UTF-8.", e);
                } catch (IllegalArgumentException e) {
                    // Malformed URI sequence.
                    throw new IllegalArgumentException("Illegal escape in diffFromDelta: " + param, e);
                }
                diffs.add(new Diff(Operation.INSERT, param));
                break;
            case '-':
                // Fall through.
            case '=':
                int n;
                try {
                    n = Integer.parseInt(param);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid number in diffFromDelta: " + param, e);
                }
                if (n < 0) { throw new IllegalArgumentException("Negative number in diffFromDelta: " + param); }
                String text;
                try {
                    text = text1.substring(pointer, pointer += n);
                } catch (StringIndexOutOfBoundsException e) {
                    throw new IllegalArgumentException("Delta length (" + pointer + ") larger than source text length (" + text1.length() + ").", e);
                }
                if (token.charAt(0) == '=') {
                    diffs.add(new Diff(Operation.EQUAL, text));
                } else {
                    diffs.add(new Diff(Operation.DELETE, text));
                }
                break;
            default:
                // Anything else is an error.
                throw new IllegalArgumentException("Invalid diff operation in diffFromDelta: " + token.charAt(0));
            }
        }
        if (pointer != text1.length()) { throw new IllegalArgumentException("Delta length (" + pointer + ") smaller than source text length (" + text1.length() + ")."); }
        return diffs;
    }

    // MATCH FUNCTIONS

    /**
     * Locate the best instance of 'pattern' in 'text' near 'loc'. Returns -1 if
     * no match found.
     * 
     * @param text
     *            The text to search.
     * @param pattern
     *            The pattern to search for.
     * @param loc
     *            The location to search around.
     * @return Best match index or -1.
     */
    public int matchMain(String text, String pattern, int loc) {
        loc = Math.max(0, Math.min(loc, text.length() - pattern.length()));
        if (text.equals(pattern)) {
            // Shortcut (potentially not guaranteed by the algorithm)
            return 0;
        } else if (text.length() == 0) {
            // Nothing to match.
            return -1;
        } else if (text.substring(loc, loc + pattern.length()).equals(pattern)) {
            // Perfect match at the perfect spot! (Includes case of null
            // pattern)
            return loc;
        } else {
            // Do a fuzzy compare.
            return matchBitap(text, pattern, loc);
        }
    }

    /**
     * Locate the best instance of 'pattern' in 'text' near 'loc' using the
     * Bitap algorithm. Returns -1 if no match found.
     * 
     * @param text
     *            The text to search.
     * @param pattern
     *            The pattern to search for.
     * @param loc
     *            The location to search around.
     * @return Best match index or -1.
     */
    public int matchBitap(String text, String pattern, int loc) {
        assert (matchMaxBits == 0 || pattern.length() <= matchMaxBits) : "Pattern too long for this application.";

        // Initialise the alphabet.
        Map<Character, Integer> s = matchAlphabet(pattern);

        int scoreTextLength = text.length();
        // Coerce the text length between reasonable maximums and minimums.
        scoreTextLength = Math.max(scoreTextLength, matchMinLength);
        scoreTextLength = Math.min(scoreTextLength, matchMaxLength);

        // Highest score beyond which we give up.
        double scoreThreshold = matchThreshold;
        // Is there a nearby exact match? (speedup)
        int bestLoc = text.indexOf(pattern, loc);
        if (bestLoc != -1) {
            scoreThreshold = Math.min(matchBitapScore(0, bestLoc, loc, scoreTextLength, pattern), scoreThreshold);
        }
        // What about in the other direction? (speedup)
        bestLoc = text.lastIndexOf(pattern, loc + pattern.length());
        if (bestLoc != -1) {
            scoreThreshold = Math.min(matchBitapScore(0, bestLoc, loc, scoreTextLength, pattern), scoreThreshold);
        }

        // Initialise the bit arrays.
        int matchmask = 1 << (pattern.length() - 1);
        bestLoc = -1;

        int binMin, binMid;
        int binMax = Math.max(loc + loc, text.length());
        // Empty initialization added to appease Java compiler.
        int[] last_rd = new int[0];
        for (int d = 0; d < pattern.length(); d++) {
            // Scan for the best match; each iteration allows for one more
            // error.
            int[] rd = new int[text.length()];

            // Run a binary search to determine how far from 'loc' we can stray
            // at
            // this error level.
            binMin = loc;
            binMid = binMax;
            while (binMin < binMid) {
                if (matchBitapScore(d, binMid, loc, scoreTextLength, pattern) < scoreThreshold) {
                    binMin = binMid;
                } else {
                    binMax = binMid;
                }
                binMid = (binMax - binMin) / 2 + binMin;
            }
            // Use the result from this iteration as the maximum for the next.
            binMax = binMid;
            int start = Math.max(0, loc - (binMid - loc) - 1);
            int finish = Math.min(text.length() - 1, pattern.length() + binMid);

            if (text.charAt(finish) == pattern.charAt(pattern.length() - 1)) {
                rd[finish] = (1 << (d + 1)) - 1;
            } else {
                rd[finish] = (1 << d) - 1;
            }
            for (int j = finish - 1; j >= start; j--) {
                if (d == 0) {
                    // First pass: exact match.
                    rd[j] = ((rd[j + 1] << 1) | 1) & (s.containsKey(text.charAt(j)) ? s.get(text.charAt(j)) : 0);
                } else {
                    // Subsequent passes: fuzzy match.
                    rd[j] = ((rd[j + 1] << 1) | 1) & (s.containsKey(text.charAt(j)) ? s.get(text.charAt(j)) : 0) | ((last_rd[j + 1] << 1) | 1) | ((last_rd[j] << 1) | 1) | last_rd[j + 1];
                }
                if ((rd[j] & matchmask) != 0) {
                    double score = matchBitapScore(d, j, loc, scoreTextLength, pattern);
                    // This match will almost certainly be better than any
                    // existing
                    // match. But check anyway.
                    if (score <= scoreThreshold) {
                        // Told you so.
                        scoreThreshold = score;
                        bestLoc = j;
                        if (j > loc) {
                            // When passing loc, don't exceed our current
                            // distance from loc.
                            start = Math.max(0, loc - (j - loc));
                        } else {
                            // Already passed loc, downhill from here on in.
                            break;
                        }
                    }
                }
            }
            if (matchBitapScore(d + 1, loc, loc, scoreTextLength, pattern) > scoreThreshold) {
                // No hope for a (better) match at greater error levels.
                break;
            }
            last_rd = rd;
        }
        return bestLoc;
    }

    /**
     * Compute and return the score for a match with e errors and x location.
     * 
     * @param e
     *            Number of errors in match.
     * @param x
     *            Location of match.
     * @param loc
     *            Expected location of match.
     * @param scoreTextLength
     *            Coerced version of text's length.
     * @param pattern
     *            Pattern being sought.
     * @return Overall score for match.
     */
    private double matchBitapScore(int e, int x, int loc, int scoreTextLength, String pattern) {
        int d = Math.abs(loc - x);
        return (e / (float) pattern.length() / matchBalance) + (d / (float) scoreTextLength / (1.0 - matchBalance));
    }

    /**
     * Initialise the alphabet for the Bitap algorithm.
     * 
     * @param pattern
     *            The text to encode.
     * @return Hash of character locations.
     */
    public Map<Character, Integer> matchAlphabet(String pattern) {
        Map<Character, Integer> s = new HashMap<Character, Integer>();
        char[] charPattern = pattern.toCharArray();
        for (char c : charPattern) {
            s.put(c, 0);
        }
        int i = 0;
        for (char c : charPattern) {
            s.put(c, s.get(c) | (1 << (pattern.length() - i - 1)));
            i++;
        }
        return s;
    }

    // PATCH FUNCTIONS

    /**
     * Increase the context until it is unique, but don't let the pattern expand
     * beyond matchMaxBits.
     * 
     * @param patch
     *            The patch to grow.
     * @param text
     *            Source text.
     */
    public void patchAddContext(Patch patch, String text) {
        String pattern = text.substring(patch.start2, patch.start2 + patch.length1);
        int padding = 0;
        // Increase the context until we're unique (but don't let the pattern
        // expand beyond matchMaxBits).
        while (text.indexOf(pattern) != text.lastIndexOf(pattern) && pattern.length() < matchMaxBits - patchMargin - patchMargin) {
            padding += patchMargin;
            pattern = text.substring(Math.max(0, patch.start2 - padding), Math.min(text.length(), patch.start2 + patch.length1 + padding));
        }
        // Add one chunk for good luck.
        padding += patchMargin;
        // Add the prefix.
        String prefix = text.substring(Math.max(0, patch.start2 - padding), patch.start2);
        if (prefix.length() != 0) {
            patch.diffs.addFirst(new Diff(Operation.EQUAL, prefix));
        }
        // Add the suffix.
        String suffix = text.substring(patch.start2 + patch.length1, Math.min(text.length(), patch.start2 + patch.length1 + padding));
        if (suffix.length() != 0) {
            patch.diffs.addLast(new Diff(Operation.EQUAL, suffix));
        }

        // Roll back the start points.
        patch.start1 -= prefix.length();
        patch.start2 -= prefix.length();
        // Extend the lengths.
        patch.length1 += prefix.length() + suffix.length();
        patch.length2 += prefix.length() + suffix.length();
    }

    /**
     * Compute a list of patches to turn text1 into text2. A set of diffs will
     * be computed.
     * 
     * @param text1
     *            Old text.
     * @param text2
     *            New text.
     * @return LinkedList of Patch objects.
     */
    public LinkedList<Patch> patchMake(String text1, String text2) {
        // No diffs provided, compute our own.
        LinkedList<Diff> diffs = diffMain(text1, text2, true);
        if (diffs.size() > 2) {
            diffCleanupSemantic(diffs);
            diffCleanupEfficiency(diffs);
        }
        return patchMake(text1, diffs);
    }

    /**
     * Compute a list of patches to turn text1 into text2. text1 will be derived
     * from the provided diffs.
     * 
     * @param diffs
     *            Array of diff tuples for text1 to text2.
     * @return LinkedList of Patch objects.
     */
    public LinkedList<Patch> patchMake(LinkedList<Diff> diffs) {
        // No origin string provided, compute our own.
        String text1 = diffText1(diffs);
        return patchMake(text1, diffs);
    }

    /**
     * Compute a list of patches to turn text1 into text2. text2 is not
     * provided, diffs are the delta between text1 and text2.
     * 
     * @param text1
     *            Old text.
     * @param diffs
     *            Array of diff tuples for text1 to text2.
     * @return LinkedList of Patch objects.
     */
    public LinkedList<Patch> patchMake(String text1, LinkedList<Diff> diffs) {
        LinkedList<Patch> patches = new LinkedList<Patch>();
        if (diffs.isEmpty()) { return patches; // Get rid of the null case.
        }
        Patch patch = new Patch();
        int charCount1 = 0; // Number of characters into the text1 string.
        int charCount2 = 0; // Number of characters into the text2 string.
        // Start with text1 (prepatchText) and apply the diffs until we arrive
        // at
        // text2 (postpatchText). We recreate the patches one by one to
        // determine
        // context info.
        String prepatchText = text1;
        String postpatchText = text1;
        for (Diff aDiff : diffs) {
            if (patch.diffs.isEmpty() && aDiff.operation != Operation.EQUAL) {
                // A new patch starts here.
                patch.start1 = charCount1;
                patch.start2 = charCount2;
            }

            switch (aDiff.operation) {
            case INSERT:
                patch.diffs.add(aDiff);
                patch.length2 += aDiff.text.length();
                postpatchText = postpatchText.substring(0, charCount2) + aDiff.text + postpatchText.substring(charCount2);
                break;
            case DELETE:
                patch.length1 += aDiff.text.length();
                patch.diffs.add(aDiff);
                postpatchText = postpatchText.substring(0, charCount2) + postpatchText.substring(charCount2 + aDiff.text.length());
                break;
            case EQUAL:
                if (aDiff.text.length() <= 2 * patchMargin && !patch.diffs.isEmpty() && aDiff != diffs.getLast()) {
                    // Small equality inside a patch.
                    patch.diffs.add(aDiff);
                    patch.length1 += aDiff.text.length();
                    patch.length2 += aDiff.text.length();
                }

                if (aDiff.text.length() >= 2 * patchMargin) {
                    // Time for a new patch.
                    if (!patch.diffs.isEmpty()) {
                        patchAddContext(patch, prepatchText);
                        patches.add(patch);
                        patch = new Patch();
                        // Unlike Unidiff, our patch lists have a rolling
                        // context.
                        // http://code.google.com/p/google-diff-match-patch/wiki/Unidiff
                        // Update prepatch text & pos to reflect the application
                        // of the
                        // just completed patch.
                        prepatchText = postpatchText;
                        charCount1 = charCount2;
                    }
                }
                break;
            }

            // Update the current character count.
            if (aDiff.operation != Operation.INSERT) {
                charCount1 += aDiff.text.length();
            }
            if (aDiff.operation != Operation.DELETE) {
                charCount2 += aDiff.text.length();
            }
        }
        // Pick up the leftover patch if not empty.
        if (!patch.diffs.isEmpty()) {
            patchAddContext(patch, prepatchText);
            patches.add(patch);
        }

        return patches;
    }

    /**
     * Given an array of patches, return another array that is identical.
     * 
     * @param patches
     *            Array of patch objects.
     * @return Array of patch objects.
     */
    protected LinkedList<Patch> patchDeepCopy(LinkedList<Patch> patches) {
        LinkedList<Patch> patchesCopy = new LinkedList<Patch>();
        for (Patch aPatch : patches) {
            Patch patchCopy = new Patch();
            for (Diff aDiff : aPatch.diffs) {
                Diff diffCopy = new Diff(aDiff.operation, aDiff.text);
                patchCopy.diffs.add(diffCopy);
            }
            patchCopy.start1 = aPatch.start1;
            patchCopy.start2 = aPatch.start2;
            patchCopy.length1 = aPatch.length1;
            patchCopy.length2 = aPatch.length2;
            patchesCopy.add(patchCopy);
        }
        return patchesCopy;
    }

    /**
     * Merge a set of patches onto the text. Return a patched text, as well as
     * an array of true/false values indicating which patches were applied.
     * 
     * @param patches
     *            Array of patch objects
     * @param text
     *            Old text.
     * @return Two element Object array, containing the new text and an array of
     *         boolean values.
     */
    public Object[] patchApply(LinkedList<Patch> patches, String text) {
        if (patches.isEmpty()) { return new Object[] { text, new boolean[0] }; }

        // Deep copy the patches so that no changes are made to originals.
        patches = patchDeepCopy(patches);

        String nullPadding = this.patchAddPadding(patches);
        text = nullPadding + text + nullPadding;
        patchSplitMax(patches);

        int x = 0;
        // delta keeps track of the offset between the expected and actual
        // location
        // of the previous patch. If there are patches expected at positions 10
        // and
        // 20, but the first patch was found at 12, delta is 2 and the second
        // patch
        // has an effective expected position of 22.
        int delta = 0;
        boolean[] results = new boolean[patches.size()];
        int expectedLoc, startLoc;
        String text1, text2;
        int index1, index2;
        for (Patch aPatch : patches) {
            expectedLoc = aPatch.start2 + delta;
            text1 = diffText1(aPatch.diffs);
            startLoc = matchMain(text, text1, expectedLoc);
            if (startLoc == -1) {
                // No match found. :(
                results[x] = false;
            } else {
                // Found a match. :)
                results[x] = true;
                delta = startLoc - expectedLoc;
                text2 = text.substring(startLoc, Math.min(startLoc + text1.length(), text.length()));
                if (text1.equals(text2)) {
                    // Perfect match, just shove the replacement text in.
                    text = text.substring(0, startLoc) + diffText2(aPatch.diffs) + text.substring(startLoc + text1.length());
                } else {
                    // Imperfect match. Run a diff to get a framework of
                    // equivalent
                    // indicies.
                    LinkedList<Diff> diffs = diffMain(text1, text2, false);
                    diffCleanupSemanticLossless(diffs);
                    index1 = 0;
                    for (Diff aDiff : aPatch.diffs) {
                        if (aDiff.operation != Operation.EQUAL) {
                            index2 = diffXIndex(diffs, index1);
                            if (aDiff.operation == Operation.INSERT) {
                                // Insertion
                                text = text.substring(0, startLoc + index2) + aDiff.text + text.substring(startLoc + index2);
                            } else if (aDiff.operation == Operation.DELETE) {
                                // Deletion
                                text = text.substring(0, startLoc + index2) + text.substring(startLoc + diffXIndex(diffs, index1 + aDiff.text.length()));
                            }
                        }
                        if (aDiff.operation != Operation.DELETE) {
                            index1 += aDiff.text.length();
                        }
                    }
                }
            }
            x++;
        }
        // Strip the padding off.
        text = text.substring(nullPadding.length(), text.length() - nullPadding.length());
        return new Object[] { text, results };
    }

    /**
     * Add some padding on text start and end so that edges can match something.
     * 
     * @param patches
     *            Array of patch objects.
     * @return The padding string added to each side.
     */
    public String patchAddPadding(LinkedList<Patch> patches) {
        LinkedList<Diff> diffs;
        String nullPadding = "";
        for (int x = 0; x < this.patchMargin; x++) {
            nullPadding += String.valueOf((char) x);
        }

        // Bump all the patches forward.
        for (Patch aPatch : patches) {
            aPatch.start1 += nullPadding.length();
            aPatch.start2 += nullPadding.length();
        }

        // Add some padding on start of first diff.
        Patch patch = patches.getFirst();
        diffs = patch.diffs;
        if (diffs.isEmpty() || diffs.getFirst().operation != Operation.EQUAL) {
            // Add nullPadding equality.
            diffs.addFirst(new Diff(Operation.EQUAL, nullPadding));
            patch.start1 -= nullPadding.length(); // Should be 0.
            patch.start2 -= nullPadding.length(); // Should be 0.
            patch.length1 += nullPadding.length();
            patch.length2 += nullPadding.length();
        } else if (nullPadding.length() > diffs.getFirst().text.length()) {
            // Grow first equality.
            Diff firstDiff = diffs.getFirst();
            int extraLength = nullPadding.length() - firstDiff.text.length();
            firstDiff.text = nullPadding.substring(firstDiff.text.length()) + firstDiff.text;
            patch.start1 -= extraLength;
            patch.start2 -= extraLength;
            patch.length1 += extraLength;
            patch.length2 += extraLength;
        }

        // Add some padding on end of last diff.
        patch = patches.getLast();
        diffs = patch.diffs;
        if (diffs.isEmpty() || diffs.getLast().operation != Operation.EQUAL) {
            // Add nullPadding equality.
            diffs.addLast(new Diff(Operation.EQUAL, nullPadding));
            patch.length1 += nullPadding.length();
            patch.length2 += nullPadding.length();
        } else if (nullPadding.length() > diffs.getLast().text.length()) {
            // Grow last equality.
            Diff lastDiff = diffs.getLast();
            int extraLength = nullPadding.length() - lastDiff.text.length();
            lastDiff.text += nullPadding.substring(0, extraLength);
            patch.length1 += extraLength;
            patch.length2 += extraLength;
        }

        return nullPadding;
    }

    /**
     * Look through the patches and break up any which are longer than the
     * maximum limit of the match algorithm.
     * 
     * @param patches
     *            LinkedList of Patch objects.
     */
    public void patchSplitMax(LinkedList<Patch> patches) {
        int patchSize;
        String precontext, postcontext;
        Patch patch;
        int start1, start2;
        boolean empty;
        Operation diffType;
        String diffText;
        ListIterator<Patch> pointer = patches.listIterator();
        Patch bigpatch = pointer.hasNext() ? pointer.next() : null;
        while (bigpatch != null) {
            if (bigpatch.length1 <= matchMaxBits) {
                bigpatch = pointer.hasNext() ? pointer.next() : null;
                continue;
            }
            // Remove the big old patch.
            pointer.remove();
            patchSize = matchMaxBits;
            start1 = bigpatch.start1;
            start2 = bigpatch.start2;
            precontext = "";
            while (!bigpatch.diffs.isEmpty()) {
                // Create one of several smaller patches.
                patch = new Patch();
                empty = true;
                patch.start1 = start1 - precontext.length();
                patch.start2 = start2 - precontext.length();
                if (precontext.length() != 0) {
                    patch.length1 = patch.length2 = precontext.length();
                    patch.diffs.add(new Diff(Operation.EQUAL, precontext));
                }
                while (!bigpatch.diffs.isEmpty() && patch.length1 < patchSize - patchMargin) {
                    diffType = bigpatch.diffs.getFirst().operation;
                    diffText = bigpatch.diffs.getFirst().text;
                    if (diffType == Operation.INSERT) {
                        // Insertions are harmless.
                        patch.length2 += diffText.length();
                        start2 += diffText.length();
                        patch.diffs.addLast(bigpatch.diffs.removeFirst());
                        empty = false;
                    } else {
                        // Deletion or equality. Only take as much as we can
                        // stomach.
                        diffText = diffText.substring(0, Math.min(diffText.length(), patchSize - patch.length1 - patchMargin));
                        patch.length1 += diffText.length();
                        start1 += diffText.length();
                        if (diffType == Operation.EQUAL) {
                            patch.length2 += diffText.length();
                            start2 += diffText.length();
                        } else {
                            empty = false;
                        }
                        patch.diffs.add(new Diff(diffType, diffText));
                        if (diffText.equals(bigpatch.diffs.getFirst().text)) {
                            bigpatch.diffs.removeFirst();
                        } else {
                            bigpatch.diffs.getFirst().text = bigpatch.diffs.getFirst().text.substring(diffText.length());
                        }
                    }
                }
                // Compute the head context for the next patch.
                precontext = diffText2(patch.diffs);
                precontext = precontext.substring(Math.max(0, precontext.length() - patchMargin));
                // Append the end context for this patch.
                if (diffText1(bigpatch.diffs).length() > patchMargin) {
                    postcontext = diffText1(bigpatch.diffs).substring(0, patchMargin);
                } else {
                    postcontext = diffText1(bigpatch.diffs);
                }
                if (postcontext.length() != 0) {
                    patch.length1 += postcontext.length();
                    patch.length2 += postcontext.length();
                    if (!patch.diffs.isEmpty() && patch.diffs.getLast().operation == Operation.EQUAL) {
                        patch.diffs.getLast().text += postcontext;
                    } else {
                        patch.diffs.add(new Diff(Operation.EQUAL, postcontext));
                    }
                }
                if (!empty) {
                    pointer.add(patch);
                }
            }
            bigpatch = pointer.hasNext() ? pointer.next() : null;
        }
    }

    /**
     * Take a list of patches and return a textual representation.
     * 
     * @param patches
     *            List of Patch objects.
     * @return Text representation of patches.
     */
    public String patchToText(List<Patch> patches) {
        StringBuilder text = new StringBuilder();
        for (Patch aPatch : patches) {
            text.append(aPatch);
        }
        return text.toString();
    }

    /**
     * Parse a textual representation of patches and return a List of Patch
     * objects.
     * 
     * @param textline
     *            Text representation of patches.
     * @return List of Patch objects.
     * @throws IllegalArgumentException
     *             If invalid input.
     */
    public List<Patch> patchFromText(String textline) throws IllegalArgumentException {
        List<Patch> patches = new LinkedList<Patch>();
        if (textline.length() == 0) { return patches; }
        List<String> textList = Arrays.asList(textline.split("\n"));
        LinkedList<String> text = new LinkedList<String>(textList);
        Patch patch;
        Pattern patchHeader = Pattern.compile("^@@ -(\\d+),?(\\d*) \\+(\\d+),?(\\d*) @@$");
        Matcher m;
        char sign;
        String line;
        while (!text.isEmpty()) {
            m = patchHeader.matcher(text.getFirst());
            if (!m.matches()) { throw new IllegalArgumentException("Invalid patch string: " + text.getFirst()); }
            patch = new Patch();
            patches.add(patch);
            patch.start1 = Integer.parseInt(m.group(1));
            if (m.group(2).length() == 0) {
                patch.start1--;
                patch.length1 = 1;
            } else if (m.group(2).equals("0")) {
                patch.length1 = 0;
            } else {
                patch.start1--;
                patch.length1 = Integer.parseInt(m.group(2));
            }

            patch.start2 = Integer.parseInt(m.group(3));
            if (m.group(4).length() == 0) {
                patch.start2--;
                patch.length2 = 1;
            } else if (m.group(4).equals("0")) {
                patch.length2 = 0;
            } else {
                patch.start2--;
                patch.length2 = Integer.parseInt(m.group(4));
            }
            text.removeFirst();

            while (!text.isEmpty()) {
                try {
                    sign = text.getFirst().charAt(0);
                } catch (IndexOutOfBoundsException e) {
                    // Blank line? Whatever.
                    text.removeFirst();
                    continue;
                }
                line = text.getFirst().substring(1);
                line = line.replace("+", "%2B"); // decode would change all "+"
                // to " "
                try {
                    line = URLDecoder.decode(line, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // Not likely on modern system.
                    throw new Error("This system does not support UTF-8.", e);
                } catch (IllegalArgumentException e) {
                    // Malformed URI sequence.
                    throw new IllegalArgumentException("Illegal escape in patchFromText: " + line, e);
                }
                if (sign == '-') {
                    // Deletion.
                    patch.diffs.add(new Diff(Operation.DELETE, line));
                } else if (sign == '+') {
                    // Insertion.
                    patch.diffs.add(new Diff(Operation.INSERT, line));
                } else if (sign == ' ') {
                    // Minor equality.
                    patch.diffs.add(new Diff(Operation.EQUAL, line));
                } else if (sign == '@') {
                    // Start of next patch.
                    break;
                } else {
                    // WTF?
                    throw new IllegalArgumentException("Invalid patch mode '" + sign + "' in: " + line);
                }
                text.removeFirst();
            }
        }
        return patches;
    }

    /**
     * Class representing one diff operation.
     */
    public static class Diff {
        public Operation operation;
        // One of: INSERT, DELETE or EQUAL.
        public String    text;

        // The text associated with this diff operation.

        /**
         * Constructor. Initializes the diff with the provided values.
         * 
         * @param operation
         *            One of INSERT, DELETE or EQUAL.
         * @param text
         *            The text being applied.
         */
        public Diff(Operation operation, String text) {
            // Construct a diff with the specified operation and text.
            this.operation = operation;
            this.text = text;
        }

        /**
         * Display a human-readable version of this Diff.
         * 
         * @return text version.
         */
        public String toString() {
            String prettyText = this.text.replace('\n', '\u00b6');
            return "Diff(" + this.operation + ",\"" + prettyText + "\")";
        }

        /**
         * Is this Diff equivalent to another Diff?
         * 
         * @param d
         *            Another Diff to compare against.
         * @return true or false.
         */
        public boolean equals(Object d) {
            if (d == null) return false;
            try {
                return (((Diff) d).operation == this.operation) && (((Diff) d).text.equals(this.text));
            } catch (ClassCastException e) {
                return false;
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((operation == null) ? 0 : operation.hashCode());
            result = prime * result + ((text == null) ? 0 : text.hashCode());
            return result;
        }

    }

    /**
     * Class representing one patch operation.
     */
    public static class Patch {
        public LinkedList<Diff> diffs;
        public int              start1;
        public int              start2;
        public int              length1;
        public int              length2;

        /**
         * Constructor. Initializes with an empty list of diffs.
         */
        public Patch() {
            this.diffs = new LinkedList<Diff>();
        }

        /**
         * Emmulate GNU diff's format. Header: @@ -382,8 +481,9 @@ Indicies are
         * printed as 1-based, not 0-based.
         * 
         * @return The GNU diff string.
         */
        public String toString() {
            String coords1, coords2;
            if (this.length1 == 0) {
                coords1 = this.start1 + ",0";
            } else if (this.length1 == 1) {
                coords1 = Integer.toString(this.start1 + 1);
            } else {
                coords1 = (this.start1 + 1) + "," + this.length1;
            }
            if (this.length2 == 0) {
                coords2 = this.start2 + ",0";
            } else if (this.length2 == 1) {
                coords2 = Integer.toString(this.start2 + 1);
            } else {
                coords2 = (this.start2 + 1) + "," + this.length2;
            }
            StringBuilder text = new StringBuilder();
            text.append("@@ -").append(coords1).append(" +").append(coords2).append(" @@\n");
            // Escape the body of the patch with %xx notation.
            for (Diff aDiff : this.diffs) {
                switch (aDiff.operation) {
                case INSERT:
                    text.append('+');
                    break;
                case DELETE:
                    text.append('-');
                    break;
                case EQUAL:
                    text.append(' ');
                    break;
                }
                try {
                    text.append(URLEncoder.encode(aDiff.text, "UTF-8").replace('+', ' ')).append("\n");
                } catch (UnsupportedEncodingException e) {
                    // Not likely on modern system.
                    throw new Error("This system does not support UTF-8.", e);
                }
            }
            return unescapeForEncodeUriCompatability(text.toString());
        }
    }

    /**
     * Unescape selected chars for compatability with JavaScript's encodeURI. In
     * speed critical applications this could be dropped since the receiving
     * application will certainly decode these fine. Note that this function is
     * case-sensitive. Thus "%3f" would not be unescaped. But this is ok because
     * it is only called with the output of URLEncoder.encode which returns
     * uppercase hex.
     * 
     * Example: "%3F" -> "?", "%24" -> "$", etc.
     * 
     * @param str
     *            The string to escape.
     * @return The escaped string.
     */
    private static String unescapeForEncodeUriCompatability(String str) {
        return str.replace("%21", "!").replace("%7E", "~").replace("%27", "'").replace("%28", "(").replace("%29", ")").replace("%3B", ";").replace("%2F", "/").replace("%3F", "?").replace("%3A", ":").replace("%40", "@").replace("%26", "&").replace("%3D", "=").replace("%2B", "+").replace("%24", "$").replace("%2C", ",").replace("%23", "#");
    }
}
