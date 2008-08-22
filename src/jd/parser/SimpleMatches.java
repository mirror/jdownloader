//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.parser;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.http.Encoding;

/**
 * Bitte Regexp Klasse verwenden
 * 
 * @author dwd
 * 
 */
public class SimpleMatches {

    /**
     * Regexp.count() verwenden
     * 
     * Zählt, wie oft das Pattern des Plugins in dem übergebenen Text vorkommt
     * 
     * @param data
     *            Der zu durchsuchende Text
     * @param pattern
     *            Das Pattern, daß im Text gefunden werden soll
     * @return Anzahl der Treffer
     */
    public @Deprecated
    static int countOccurences(String data, Pattern pattern) {
        int position = 0;
        int occurences = 0;
        if (pattern != null) {
            Matcher matcher = pattern.matcher(data);
            while (matcher.find(position)) {
                occurences++;
                position = matcher.start() + matcher.group().length();
            }
        }
        return occurences;
    }

    /**
     * Regexp.getMatches() verwenden
     * 
     * Schreibt alle treffer von pattern in source in den übergebenen vector Als
     * Rückgabe erhält man einen 2D-Vector
     * 
     * @param source
     *            Quelltext
     * @param pattern
     *            Ein RegEx Pattern
     * @return Treffer
     */
    public @Deprecated
    static ArrayList<ArrayList<String>> getAllSimpleMatches(Object source, Pattern pattern) {
        ArrayList<ArrayList<String>> ret = new ArrayList<ArrayList<String>>();
        ArrayList<String> entry;
        String tmp;
        for (Matcher r = pattern.matcher(source.toString()); r.find();) {
            entry = new ArrayList<String>();
            for (int x = 1; x <= r.groupCount(); x++) {
                tmp = r.group(x).trim();
                entry.add(Encoding.UTF8Decode(tmp));
            }
            ret.add(entry);
        }
        return ret;
    }

    /**
     * Regexp.getMatches(int group) verwenden
     * 
     * @param source
     * @param pattern
     * @param id
     * @return
     */
    public @Deprecated
    static ArrayList<String> getAllSimpleMatches(Object source, Pattern pattern, int id) {

        ArrayList<String> ret = new ArrayList<String>();
        for (Matcher r = pattern.matcher(source.toString()); r.find();) {
            if (id <= r.groupCount()) {
                ret.add(r.group(id).trim());
            }
        }
        return ret;
    }

    /**
     * Regexp.getMatches() verwenden
     * 
     * Schreibt alle Treffer von pattern in source in den übergebenen Vector.
     * Als Rückgabe erhält man einen 2D-Vector
     * 
     * @param source
     *            Quelltext
     * @param pattern
     *            als Pattern wird ein Normaler String mit ° als Wildcard
     *            verwendet.
     * @return Treffer
     */
    public @Deprecated
    static ArrayList<ArrayList<String>> getAllSimpleMatches(Object source, String pattern) {
        return SimpleMatches.getAllSimpleMatches(source.toString(), Pattern.compile(SimpleMatches.getPattern(pattern), Pattern.DOTALL));
    }

    /**
     * Regexp.getMatches(int group) verwenden
     * 
     * Gibt von allen treffer von pattern in source jeweils den id-ten Match
     * einem vector zurück. Als pattern kommt ein Simplepattern zum einsatz
     * 
     * @param source
     * @param pattern
     * @param id
     * @return Matchlist
     */
    public @Deprecated
    static ArrayList<String> getAllSimpleMatches(Object source, String pattern, int id) {
        pattern = SimpleMatches.getPattern(pattern);
        ArrayList<String> ret = new ArrayList<String>();
        for (Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(source.toString()); r.find();) {
            if (id <= r.groupCount()) {
                ret.add(r.group(id).trim());
            }
        }
        return ret;
    }

    /**
     * Regexp.getFirstMatch(int group) verwenden
     * 
     * Findet ein einzelnes Vorkommen und liefert den vollständigen Treffer oder
     * eine Untergruppe zurück
     * 
     * @param data
     *            Der zu durchsuchende Text
     * @param pattern
     *            Das Muster, nach dem gesucht werden soll
     * @param group
     *            Die Gruppe, die zurückgegeben werden soll. 0 ist der
     *            vollständige Treffer.
     * @return Der Treffer
     */
    public @Deprecated
    static String getFirstMatch(String data, Pattern pattern, int group) {
        String hit = null;
        if (data == null) { return null; }
        if (pattern != null) {
            Matcher matcher = pattern.matcher(data);
            if (matcher.find() && group <= matcher.groupCount()) {
                hit = matcher.group(group);
            }
        }
        return hit;
    }

    /**
     * Das ergibt eigentlich wenig Sinn und ist nicht Standartkonform
     * 
     * public static String getPattern(String str) Gibt ein Regex pattern
     * zurück. ° dient als Platzhalter!
     * 
     * @param str
     * @return REgEx Pattern
     */
    public @Deprecated
    static String getPattern(String str) {
        String allowed = "QWERTZUIOPÜASDFGHJKLÖÄYXCVBNMqwertzuiopasdfghjklyxcvbnm 1234567890";
        String ret = "";
        int i;
        for (i = 0; i < str.length(); i++) {
            char letter = str.charAt(i);
            // 176 == °
            if (letter == 176) {
                ret += "(.*?)";
            } else if (allowed.indexOf(letter) == -1) {
                ret += "\\" + letter;
            } else {
                ret += letter;
            }
        }
        return ret;
    }

    /**
     * Durchsucht source mit pattern ach treffern und gibt Treffer id zurück.
     * Bei dem Pattern muss es sich um einen String Handeln der ° als
     * Platzhalter verwendet. Alle newline Chars in source müssen mit einem
     * °-PLatzhalter belegt werden
     * 
     * @param source
     * @param pattern
     * @param id
     * @return String Match
     */
    public @Deprecated
    static String getSimpleMatch(Object source, String pattern, int id) {

        String[] res = SimpleMatches.getSimpleMatches(source.toString(), pattern);
        if (res != null && res.length > id) { return res[id]; }
        return null;
    }

    /**
     * Regexp.getMatches(int x)[y] verwenden
     * 
     * Gibt über die simplepattern alle den x/y ten treffer aus dem 2D-matches
     * array zurück
     * 
     * @param source
     * @param pattern
     * @param x
     * @param y
     * @return treffer an der stelle x/y im 2d treffer array
     */
    public @Deprecated
    static String getSimpleMatch(String source, String pattern, int x, int y) {
        ArrayList<ArrayList<String>> ret = SimpleMatches.getAllSimpleMatches(source, pattern);
        if (ret.get(x) != null && ret.get(x).get(y) != null) { return ret.get(x).get(y); }
        return null;
    }

    /**
     * Regexp.getMatches() verwenden
     * 
     * public static String[] getMatches(String source, String pattern) Gibt
     * alle treffer in source nach dem pattern zurück. Platzhalter ist nur !! °
     * 
     * @param source
     * @param pattern
     *            als Pattern wird ein Normaler String mit ° als Wildcard
     *            verwendet.
     * @return Alle TReffer
     */
    public @Deprecated
    static String[] getSimpleMatches(Object source, String pattern) {
        // DEBUG.trace("pattern: "+STRING.getPattern(pattern));
        if (source == null || pattern == null) { return null; }
        Matcher rr = Pattern.compile(SimpleMatches.getPattern(pattern), Pattern.DOTALL).matcher(source.toString());
        if (!rr.find()) {
            // Keine treffer
        }
        try {
            String[] ret = new String[rr.groupCount()];
            for (int i = 1; i <= rr.groupCount(); i++) {
                ret[i - 1] = rr.group(i);
            }
            return ret;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /**
     * Gibt die matches ohne Dublikate als arraylist aus
     * 
     * @param data
     * @param pattern
     * @return StringArray mit den Matches
     */
    public @Deprecated
    static String[] getUniqueMatches(String data, Pattern pattern) {
        ArrayList<String> set = new ArrayList<String>();
        Matcher m = pattern.matcher(data);
        while (m.find()) {
            if (!set.contains(m.group())) {
                set.add(m.group());
            }
        }
        return set.toArray(new String[set.size()]);
    }

}
