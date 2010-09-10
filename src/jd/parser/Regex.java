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

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appwork.utils.formatter.SizeFormatter;

/**
 * TODO: Remove with next major update and change to
 * {@link org.appwork.utils.Regex}
 */
public class Regex extends org.appwork.utils.Regex {

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

        if (org.appwork.utils.Regex.matches(wait, Pattern.compile("(h|st)", Pattern.CASE_INSENSITIVE))) {
            res *= 60 * 60 * 1000l;
        } else if (org.appwork.utils.Regex.matches(wait, Pattern.compile("(m)", Pattern.CASE_INSENSITIVE))) {
            res *= 60 * 1000l;
        } else {
            res *= 1000l;
        }
        return Math.round(res);
    }

    public static long getMilliSeconds(final String expire, final String timeformat, final Locale l) {
        if (expire != null) {
            final SimpleDateFormat dateFormat = l != null ? new SimpleDateFormat(timeformat, l) : new SimpleDateFormat(timeformat);
            try {
                return dateFormat.parse(expire).getTime();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public static long getSize(final String string) {
        return SizeFormatter.getSize(string);
    }

    public Regex(final Matcher matcher) {
        super(matcher);
    }

    public Regex(final Object data, final Pattern pattern) {
        super(data, pattern);
    }

    public Regex(final Object data, final String pattern) {
        super(data, pattern);
    }

    public Regex(final Object data, final String pattern, final int flags) {
        super(data, pattern, flags);
    }

    public Regex(final String data, final Pattern pattern) {
        super(data, pattern);
    }

    public Regex(final String data, final String pattern) {
        super(data, pattern);
    }

    public Regex(final String data, final String pattern, final int flags) {
        super(data, pattern, flags);
    }

}
