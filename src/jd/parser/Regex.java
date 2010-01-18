//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.utils.StringUtil;

public class Regex {

    private Matcher matcher;

    public Regex(final Matcher matcher) {
        if (matcher != null) {
            this.matcher = matcher;
        }
    }

    /**
     * Regexp auf ein Objekt (Beim Objekt wird toString aufgerufen)
     * 
     * @param data
     * @param pattern
     */
    public Regex(final Object data, final Pattern pattern) {
        this(data.toString(), pattern);
    }

    /**
     * Regexp auf ein Objekt (Beim Objekt wird toString aufgerufen)
     * 
     * @param data
     * @param pattern
     */
    public Regex(final Object data, final String pattern) {
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
    public Regex(final Object data, final String pattern, final int flags) {
        this(data.toString(), pattern, flags);
    }

    public Regex(final String data, final Pattern pattern) {
        if (data != null && pattern != null) {
            matcher = pattern.matcher(data);
        }
    }

    public Regex(final String data, final String pattern) {
        if (data != null && pattern != null) {
            matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(data);
        }
    }

    public Regex(final String data, final String pattern, final int flags) {
        if (data != null && pattern != null) {
            matcher = Pattern.compile(pattern, flags).matcher(data);
        }
    }

    /**
     * Gibt die Anzahl der Treffer zurück
     * 
     * @return
     */
    public int count() {
        if (matcher == null) {
            return 0;
        } else {
            matcher.reset();
            int c = 0;
            final Matcher matchertmp = matcher;
            while (matchertmp.find()) {
                c++;
            }
            return c;
        }
    }

    public String getMatch(final int group) {
        if (matcher != null) {
            final Matcher matcher = this.matcher;
            matcher.reset();
            if (matcher.find()) { return matcher.group(group + 1); }
        }
        return null;
    }

    /**
     * Gibt den matcher aus
     * 
     * @return
     */
    public Matcher getMatcher() {
        if (matcher != null) {
            matcher.reset();
        }
        return matcher;
    }

    /**
     * Gibt alle Treffer eines Matches in einem 2D array aus
     * 
     * @return
     */
    public String[][] getMatches() {
        if (matcher == null) {
            return null;
        } else {
            final Matcher matcher = this.matcher;
            matcher.reset();
            final ArrayList<String[]> ar = new ArrayList<String[]>();
            while (matcher.find()) {
                int c = matcher.groupCount();
                int d = 1;
                String[] group;
                if (c == 0) {
                    group = new String[c + 1];
                    d = 0;
                } else {
                    group = new String[c];
                }

                for (int i = d; i <= c; i++) {
                    group[i - d] = matcher.group(i);
                }
                ar.add(group);
            }
            return (ar.size() == 0) ? new String[][] {} : ar.toArray(new String[][] {});
        }
    }

    public String[] getColumn(int x) {
        if (matcher == null) {
            return null;
        } else {
            x++;
            final Matcher matcher = this.matcher;
            matcher.reset();

            final ArrayList<String> ar = new ArrayList<String>();
            while (matcher.find()) {
                ar.add(matcher.group(x));
            }
            return ar.toArray(new String[ar.size()]);
        }
    }

    public boolean matches() {
        final Matcher matcher = this.matcher;
        if (matcher == null) {
            return false;
        } else {
            matcher.reset();
            return matcher.find();
        }
    }

    /**
     * Setzt den Matcher
     * 
     * @param matcher
     */
    public void setMatcher(final Matcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public String toString() {
        final StringBuilder ret = new StringBuilder();
        final String[][] matches = getMatches();
        final int matchesLength = matches.length;
        String[] match;
        int matchLength;
        for (int i = 0; i < matchesLength; i++) {
            match = matches[i];
            matchLength = match.length;
            for (int j = 0; j < matchLength; j++) {
                ret.append("match[");
                ret.append(i);
                ret.append("][");
                ret.append(j);
                ret.append("] = ");
                ret.append(match[j]);
                //ret.append(System.getProperty("line.separator"));
                ret.append(StringUtil.LINE_SEPARATOR);
            }
        }
        matcher.reset();
        return ret.toString();
    }

    public static long getMilliSeconds(final String wait) {
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

    public static long getMilliSeconds(final String expire, final String timeformat, final Locale l) {
        if (expire != null) {
            final SimpleDateFormat dateFormat = (l != null) ? new SimpleDateFormat(timeformat, l) : new SimpleDateFormat(timeformat);

            try {
                return (dateFormat.parse(expire).getTime());
            } catch (ParseException e) {
                // ("Could not format date " + expire + " with formater " +
                // timeformat + ": " + dateFormat.format(new Date()));
                e.printStackTrace();
                // JDLogger.exception(e);
            }
        }
        return -1;
    }

    public String getMatch(int entry, int group) {
        if (matcher != null) {
            final Matcher matcher = this.matcher;
            matcher.reset();
            // group++;
            entry++;
            int groupCount = 0;
            while (matcher.find()) {
                if (groupCount == group) { return matcher.group(entry); }
                groupCount++;
            }
        }
        return null;
    }

    public String[] getRow(final int y) {
        if (matcher != null) {
            final Matcher matcher = this.matcher;
            matcher.reset();
            int groupCount = 0;
            while (matcher.find()) {
                if (groupCount == y) {
                    final int c = matcher.groupCount();

                    final String[] group = new String[c];

                    for (int i = 1; i <= c; i++) {
                        group[i - 1] = matcher.group(i);
                    }
                    return group;
                }
                groupCount++;
            }
        }
        return null;
    }

    /**
     * Formatiert Zeitangaben 2h 40 min 2 sek
     * 
     * @param wait
     */
    public static int getMilliSeconds2(final String wait) {
        String minutes = new Regex(wait, "(\\d*?)[ ]*m").getMatch(0);
        String hours = new Regex(wait, "(\\d*?)[ ]*(h|st)").getMatch(0);
        String seconds = new Regex(wait, "(\\d*?)[ ]*se").getMatch(0);
        if (minutes == null) minutes = "0";
        if (hours == null) hours = "0";
        if (seconds == null) seconds = "0";
        return Integer.parseInt(hours) * 60 * 60 * 1000 + Integer.parseInt(minutes) * 60 * 1000 + Integer.parseInt(seconds) * 1000;

    }

    /**
     * Setzt vor alle Steuerzeichen ein \
     * 
     * @param pattern
     * @return
     */
    public static String escape(final String pattern) {
        final char[] specials = new char[] { '(', '[', '{', '\\', '^', '-', '$', '|', ']', '}', ')', '?', '*', '+', '.' };
        final int patternLength = pattern.length();
        final StringBuilder sb = new StringBuilder();
        sb.setLength(patternLength);
        char act;
        for (int i = 0; i < patternLength; i++) {
            act = pattern.charAt(i);
            for (char s : specials) {
                if (act == s) {
                    sb.append('\\');
                    break;
                }
            }
            sb.append(act);
        }
        return sb.toString().trim();
    }

    public static String[] getLines(final String arg) {
        if (arg == null) {
            return new String[] {};
        } else {
            final String[] temp = arg.split("[\r\n]{1,2}");
            final int tempLength = temp.length;
            final String[] output = new String[tempLength];
            for (int i = 0; i < tempLength; i++) {
                output[i] = temp[i].trim();
            }
            return output;
        }
    }

    /**
     * Gibt zu einem typischem Sizestring (12,34kb , 45 mb etc) die größe in
     * bytes zurück.
     * 
     * @param sizestring
     * @return
     */
    public static long getSize(final String string) {
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

    public static boolean matches(final Object str, final Pattern pat) {
        return new Regex(str, pat).matches();
    }

    public static boolean matches(final Object page, final String string) {
        return new Regex(page, string).matches();
    }

}
