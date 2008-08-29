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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.utils.JDUtilities;

/**
 * Die Regexklasse Diese Klasse wird in JD zum Parsen über Regluar Expressions
 * verwendet. Andere Konstrukte wie getBetween und Co sollten wenn möglich nicht
 * verwendet werden. Zum Parsen wird der Klasse der Text (Heuhaufen) und das
 * Pattern übergeben. Das Pattern kann mehrmals im Text vorkommen und kann
 * selbst mehrere "Platzhalter" enhalten. daraus ergibt sich ein Tabellenartiges
 * 2-D Trefferfeld. Die Reihen dieser Tabelle sind dabei Die TrefferGruppen.
 * 
 * Beispiel: Sind 3 Platzhalter im Pattern (..) So hat jede Reihe 3 Einträge. Es
 * gibt soviele Reihen wie das Pattern im Text vorkommt.
 * 
 * Der Zugriff auf die Treffer erfolgt über Reihen und Spalten.
 * 
 * _______X | | | Y
 * 
 * Der Erste Treffer, bzw die erste Treffergruppe haben immer den Index 0. Mit
 * Index -1 wird jeweils der Komplette Match angesprochen. Beispiel:
 * "http://(.*?)\\.(.*?)" auf a href="http://www.google.de" Index 0 z.B.
 * getMatch(0,0); gibt www.google zurück Index 1 getMatch(1,0); gibt de zurück
 * index -1 getMatch(-1,0); gibt "http://www.google.de" zurück.
 */
public class Regex {
    public static String[] getLines(String arg) {
        if (arg == null) { return new String[] {}; }
        return arg.split("[\r|\n]{1,2}");
    }

    /**
     * Gibt zu einem typischem Sizestring (12,34kb , 45 mb etc) die größe in
     * bytes zurück.
     * 
     * @param sizestring
     * @return
     */
    public static long getSize(String string) {

        String[][] matches = new Regex(string, Pattern.compile("([\\d]+)[\\.|\\,|\\:]([\\d]+)", Pattern.CASE_INSENSITIVE)).getMatches();

        if (matches == null || matches.length == 0) {
            matches = new Regex(string, Pattern.compile("([\\d]+)", Pattern.CASE_INSENSITIVE)).getMatches();

        }
        if (matches == null || matches.length == 0) { return -1; }

        double res = 0;
        if (matches[0].length == 1) {
            res = Double.parseDouble(matches[0][0]);
        }
        if (matches[0].length == 2) {
            res = Double.parseDouble(matches[0][0] + "." + matches[0][1]);
        }
        if (Regex.matches(string, Pattern.compile("(gb|gbyte|gig)", Pattern.CASE_INSENSITIVE))) {
            res *= 1024 * 1024 * 1024;
        } else if (Regex.matches(string, Pattern.compile("(mb|mbyte|megabyte)", Pattern.CASE_INSENSITIVE))) {
            res *= 1024 * 1024;
        } else if (Regex.matches(string, Pattern.compile("(kb|kbyte|kilobyte)", Pattern.CASE_INSENSITIVE))) {
            res *= 1024;
        }

        return Math.round(res);
    }

    public static boolean matches(Object str, Pattern pat) {

        return new Regex(str, pat).matches();
    }

    public static boolean matches(Object page, String string) {

        return new Regex(page, string).matches();
    }

    private Matcher matcher;

    public Regex(Matcher matcher) {
        if (matcher == null) { return; }
        this.matcher = matcher;
    }

    /**
     * Regexp auf ein Objekt (Beim Objekt wird toString aufgerufen)
     * 
     * @param data
     * @param pattern
     */
    public Regex(Object data, Pattern pattern) {
        this(data.toString(), pattern);
    }

    /**
     * Regexp auf ein Objekt (Beim Objekt wird toString aufgerufen)
     * 
     * @param data
     * @param pattern
     */
    public Regex(Object data, String pattern) {
        this(data.toString(), pattern);
    }

    /**
     * Regexp auf ein Objekt (Beim Objekt wird toString aufgerufen)
     * 
     * @param data
     * @param pattern
     * @param flags
     *            flags für den Pattern z.B. Pattern.CASE_INSENSITIVE
     */
    public Regex(Object data, String pattern, int flags) {
        this(data.toString(), pattern, flags);
    }

    public Regex(String data, Pattern pattern) {
        if (data == null || pattern == null) { return; }
        matcher = pattern.matcher(data);
    }

    public Regex(String data, String pattern) {
        if (data == null || pattern == null) { return; }
        matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(data);

    }

    public Regex(String data, String pattern, int flags) {
        if (data == null || pattern == null) { return; }
        matcher = Pattern.compile(pattern, flags).matcher(data);
    }

    /**
     * gibt die Anzahl der Treffer zurück
     */
    public int count() {
        if (matcher == null) { return 0; }
        matcher.reset();
        int c = 0;
        Matcher matchertmp = matcher;
        while (matchertmp.find()) {
            c++;
        }
        return c;
    }

    public String getMatch(int group) {
        if (matcher == null) { return null; }
        Matcher matchertmp = matcher;
        matcher.reset();
        if (matchertmp.find()) { return matchertmp.group(group + 1); }

        return null;

    }

    /**
     * gibt den matcher aus
     */
    public Matcher getMatcher() {
        matcher.reset();
        return matcher;
    }

    /**
     * Gibt alle Treffer eines Matches in einem 2D array aus
     */
    public String[][] getMatches() {
        if (matcher == null) { return null; }
        Matcher matchertmp = matcher;
        matcher.reset();
        ArrayList<String[]> ar = new ArrayList<String[]>();
        while (matchertmp.find()) {
            int c = matchertmp.groupCount();
            int d = 1;
            String[] group;
            if (c == 0) {
                group = new String[c + 1];
                d = 0;
            } else {
                group = new String[c];
            }

            for (int i = d; i <= c; i++) {
                group[i - d] = matchertmp.group(i);
            }
            ar.add(group);
        }
        return ar.toArray(new String[][] {});
    }

    /**
     * Die RegexKlasse kann man sich wie einen 2D parser vorstellen. Ein Pattern
     * kann beliebig viele PLatzhalter haben und werden von 0-...[x]
     * durchnummeriert. x entspricht der Spaltennummer Gleichzeitig kann ein
     * Pattern im Text mehrfach vorkommen. ergebnisse werden von 0-...[y]
     * durchnummeriert. y entspricht der Reihe. getColomn(x) gibt dabei für
     * jeden Treffer den Wert des x. Platzhalters zurück
     * 
     */
    public String[] getColumn(int x) {
        if (matcher == null) { return null; }
        x++;
        Matcher matchertmp = matcher;
        matcher.reset();

        ArrayList<String> ar = new ArrayList<String>();
        while (matchertmp.find()) {
            ar.add(matchertmp.group(x));
        }
        return ar.toArray(new String[ar.size()]);
    }

    public boolean matches() {
        matcher.reset();
        return matcher.find();
    }

    /**
     * setzt den matcher
     */
    public void setMatcher(Matcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public String toString() {
        String ret = "";
        String[][] match = getMatches();
        for (int i = 0; i < match.length; i++) {
            for (int j = 0; j < match.length; j++) {
                ret += "match[" + i + "][" + j + "]=" + match[i][j] + System.getProperty("line.separator");
            }
        }
        matcher.reset();
        return ret;
    }

    public static long getMilliSeconds(String wait) {
        String[][] matches = new Regex(wait, "([\\d]+) ?[\\.|\\,|\\:] ?([\\d]+)").getMatches();
        if (matches == null || matches.length == 0) {
            matches = new Regex(wait, Pattern.compile("([\\d]+)")).getMatches();

        }

        if (matches == null || matches.length == 0) { return -1; }

        double res = 0;
        if (matches[0].length == 1) {
            res = Double.parseDouble(matches[0][0]);
        }
        if (matches[0].length == 2) {
            res = Double.parseDouble(matches[0][0] + "." + matches[0][1]);
        }

        if (Regex.matches(wait, Pattern.compile("(h|st)", Pattern.CASE_INSENSITIVE))) {
            res *= 60 * 60 * 1000l;
        } else if (Regex.matches(wait, Pattern.compile("(m)", Pattern.CASE_INSENSITIVE))) {
            res *= 60 * 1000l;
        } else {
            res *= 1000l;
        }

        return Math.round(res);

    }

    public static long getMilliSeconds(String expire, String timeformat, Locale l) {
        SimpleDateFormat dateFormat;

        if (l != null) {
            dateFormat = new SimpleDateFormat(timeformat, l);
        } else {
            dateFormat = new SimpleDateFormat(timeformat);
        }
        if (expire == null) { return -1; }

        Date date;
        try {
            date = dateFormat.parse(expire);
            return (date.getTime());
        } catch (ParseException e) {
            JDUtilities.getLogger().severe("Could not format date "+expire+" with formater "+timeformat+": "+dateFormat.format(new Date()));
            
            e.printStackTrace();
        }
        return -1;

    }

    public String getMatch(int entry, int group) {
        if (matcher == null) { return null; }
        Matcher matchertmp = matcher;
        matcher.reset();
        // group++;
        entry++;
        int groupCount = 0;
        while (matchertmp.find()) {
            if (groupCount == group) { return matchertmp.group(entry); }

            groupCount++;
        }
        return null;
    }

    public String[] getRow(int y) {
        if (matcher == null) { return null; }
        Matcher matchertmp = matcher;
        matcher.reset();
        int groupCount = 0;
        while (matchertmp.find()) {
            if (groupCount == y) {
                int c = matchertmp.groupCount();

                String[] group = new String[c];

                for (int i = 1; i <= c; i++) {
                    group[i - 1] = matchertmp.group(i);
                }
                return group;
            }
            groupCount++;
        }
        return null;
    }
}
