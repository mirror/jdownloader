//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.controlling;

import java.util.regex.Pattern;

import jd.config.SubConfiguration;
import jd.parser.Regex;
import jd.plugins.hoster.DirectHTTP;

public class LinkGrabberPackager {
    public static final Pattern pat0 = Pattern.compile("(.*)(\\.|_|-)pa?r?t?\\.?[0-9]+.rar$", Pattern.CASE_INSENSITIVE);
    public static final Pattern pat1 = Pattern.compile("(.*)(\\.|_|-)part\\.?[0]*[1].rar$", Pattern.CASE_INSENSITIVE);
    public static final Pattern pat3 = Pattern.compile("(.*)\\.rar$", Pattern.CASE_INSENSITIVE);
    public static final Pattern pat4 = Pattern.compile("(.*)\\.r\\d+$", Pattern.CASE_INSENSITIVE);
    public static final Pattern pat5 = Pattern.compile("(.*)(\\.|_|-)\\d+$", Pattern.CASE_INSENSITIVE);
    public static final Pattern[] rarPats = new Pattern[] { pat0, pat1, pat3, pat4, pat5 };

    public static final Pattern pat6 = Pattern.compile("(.*)\\.zip$", Pattern.CASE_INSENSITIVE);
    public static final Pattern pat7 = Pattern.compile("(.*)\\.z\\d+$", Pattern.CASE_INSENSITIVE);
    public static final Pattern pat8 = Pattern.compile("(?is).*\\.7z\\.[\\d]+$", Pattern.CASE_INSENSITIVE);
    public static final Pattern pat9 = Pattern.compile("(.*)\\.a.$", Pattern.CASE_INSENSITIVE);
    public static final Pattern[] zipPats = new Pattern[] { pat6, pat7, pat8, pat9 };

    public static final Pattern pat10 = Pattern.compile("(.*)\\._((_[a-z]{1})|([a-z]{2}))(\\.|$)");
    public static final Pattern pat11 = Pattern.compile("(.*)(\\.|_|-)[\\d]+(" + DirectHTTP.ENDINGS + "$)", Pattern.CASE_INSENSITIVE);
    public static final Pattern[] ffsjPats = new Pattern[] { pat10, pat11 };

    public static final Pattern pat12 = Pattern.compile("(CD\\d+)", Pattern.CASE_INSENSITIVE);
    public static final Pattern pat13 = Pattern.compile("(part\\d+)", Pattern.CASE_INSENSITIVE);

    public static final Pattern pat14 = Pattern.compile("(.+)\\.+$");
    public static final Pattern pat15 = Pattern.compile("(.+)-+$");
    public static final Pattern pat16 = Pattern.compile("(.+)_+$");

    public static final Pattern pat17 = Pattern.compile("(.+)\\.\\d+\\.xtm$");

    public static String cleanFileName(String name) {
        /** remove rar extensions */
        String before = name;
        for (Pattern Pat : rarPats) {
            name = getNameMatch(name, Pat);
            if (!before.equalsIgnoreCase(name)) break;
        }
        /**
         * remove 7zip/zip and hjmerge extensions
         */
        before = name;
        for (Pattern Pat : zipPats) {
            name = getNameMatch(name, Pat);
            if (!before.equalsIgnoreCase(name)) break;
        }
        /* xtremsplit */
        name = getNameMatch(name, pat17);

        /**
         * FFSJ splitted files
         * 
         * */
        before = name;
        for (Pattern Pat : ffsjPats) {
            name = getNameMatch(name, Pat);
            if (!before.equalsIgnoreCase(name)) break;
        }

        /**
         * remove CDx,Partx
         */
        String tmpname = cutNameMatch(name, pat12);
        if (tmpname.length() > 3) name = tmpname;
        tmpname = cutNameMatch(name, pat13);
        if (tmpname.length() > 3) name = tmpname;

        /* remove extension */
        int lastPoint = name.lastIndexOf(".");
        if (lastPoint <= 0) lastPoint = name.lastIndexOf("_");
        if (lastPoint > 0) {
            if ((name.length() - lastPoint + 1) <= 3) {
                name = name.substring(0, lastPoint);
            }
        }
        /* remove ending ., - , _ */
        name = getNameMatch(name, pat14);
        name = getNameMatch(name, pat15);
        name = getNameMatch(name, pat16);

        /* if enabled, replace dots and _ with spaces */
        if (SubConfiguration.getConfig(LinkGrabberController.CONFIG).getBooleanProperty(LinkGrabberController.PARAM_REPLACECHARS, false)) {
            name = name.replaceAll("_", " ");
            name = name.replaceAll("\\.", " ");
        }
        return name.trim();
    }

    private static String getNameMatch(String name, Pattern pattern) {
        String match = new Regex(name, pattern).getMatch(0);
        if (match != null) return match;
        return name;
    }

    public static int comparepackages(String a, String b) {
        int c = 0;
        String aa = a.toLowerCase();
        String bb = b.toLowerCase();
        for (int i = 0; i < Math.min(aa.length(), bb.length()); i++) {
            if (aa.charAt(i) == bb.charAt(i)) {
                c++;
            }
        }
        if (Math.min(aa.length(), bb.length()) == 0) { return 0; }
        return c * 100 / Math.max(aa.length(), bb.length());
    }

    private static String cutNameMatch(String name, Pattern pattern) {
        if (name == null) return null;
        String match = new Regex(name, pattern).getMatch(0);
        if (match != null) {
            int firstpos = name.indexOf(match);
            String tmp = name.substring(0, firstpos);
            int lastpos = name.indexOf(match) + match.length();
            if (!(lastpos > name.length())) tmp = tmp + name.substring(lastpos);
            name = tmp;
            /* remove seq. dots */
            name = name.replaceAll("\\.{2,}", ".");
            name = name.replaceAll("\\.{2,}", ".");
        }
        return name;
    }

}
