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
@Deprecated
public class SimpleMatches {

    /**
     * Schreibt alle treffer von pattern in source in den übergebenen vector
     * Als Rückgabe erhält man einen 2D-Vector
     * 
     * @param source
     *            Quelltext
     * @param pattern
     *            Ein RegEx Pattern
     * @return Treffer
     * @deprecated Regexp.getMatches() verwenden
     */
    @Deprecated
    public static ArrayList<ArrayList<String>> getAllSimpleMatches(Object source, Pattern pattern) {
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
     * Schreibt alle Treffer von pattern in source in den übergebenen Vector.
     * Als Rückgabe erhält man einen 2D-Vector
     * 
     * @param source
     *            Quelltext
     * @param pattern
     *            als Pattern wird ein Normaler String mit ° als Wildcard
     *            verwendet.
     * @return Treffer
     * @deprecated Regexp.getMatches() verwenden
     */
    @Deprecated
    public static ArrayList<ArrayList<String>> getAllSimpleMatches(Object source, String pattern) {
        return SimpleMatches.getAllSimpleMatches(source.toString(), Pattern.compile(SimpleMatches.getPattern(pattern), Pattern.DOTALL));
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
    public static String getPattern(String str) {
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
    @Deprecated
    public static String getSimpleMatch(Object source, String pattern, int id) {

        String[] res = SimpleMatches.getSimpleMatches(source.toString(), pattern);
        if (res != null && res.length > id) { return res[id]; }
        return null;
    }

    /**
     * public static String[] getMatches(String source, String pattern) Gibt
     * alle treffer in source nach dem pattern zurück. Platzhalter ist nur !!
     * °
     * 
     * @param source
     * @param pattern
     *            als Pattern wird ein Normaler String mit ° als Wildcard
     *            verwendet.
     * @return Alle TReffer
     * @deprecated Regexp.getMatches() verwenden
     */
    @Deprecated
    public static String[] getSimpleMatches(Object source, String pattern) {
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

}
