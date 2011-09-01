package jd.controlling.linkcollector;

import java.util.regex.Pattern;

import jd.plugins.hoster.DirectHTTP;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Regex;
import org.jdownloader.settings.GeneralSettings;

public class LinknameCleaner {
    public static final Pattern   pat0        = Pattern.compile("(.*)(\\.|_|-)pa?r?t?\\.?[0-9]+.(rar|exe)$", Pattern.CASE_INSENSITIVE);
    public static final Pattern   pat1        = Pattern.compile("(.*)(\\.|_|-)part\\.?[0]*[1].(rar|exe)$", Pattern.CASE_INSENSITIVE);
    public static final Pattern   pat3        = Pattern.compile("(.*)\\.rar$", Pattern.CASE_INSENSITIVE);
    public static final Pattern   pat4        = Pattern.compile("(.*)\\.r\\d+$", Pattern.CASE_INSENSITIVE);
    public static final Pattern   pat5        = Pattern.compile("(.*)(\\.|_|-)\\d+$", Pattern.CASE_INSENSITIVE);
    public static final Pattern[] rarPats     = new Pattern[] { pat0, pat1, pat3, pat4, pat5 };

    public static final Pattern   pat6        = Pattern.compile("(.*)\\.zip$", Pattern.CASE_INSENSITIVE);
    public static final Pattern   pat7        = Pattern.compile("(.*)\\.z\\d+$", Pattern.CASE_INSENSITIVE);
    public static final Pattern   pat8        = Pattern.compile("(?is).*\\.7z\\.[\\d]+$", Pattern.CASE_INSENSITIVE);
    public static final Pattern   pat9        = Pattern.compile("(.*)\\.a.$", Pattern.CASE_INSENSITIVE);
    public static final Pattern[] zipPats     = new Pattern[] { pat6, pat7, pat8, pat9 };

    public static final Pattern   pat10       = Pattern.compile("(.*)\\._((_[a-z]{1})|([a-z]{2}))(\\.|$)");
    public static final Pattern   pat11       = Pattern.compile("(.*)(\\.|_|-)[\\d]+(" + DirectHTTP.ENDINGS + "$)", Pattern.CASE_INSENSITIVE);
    public static final Pattern[] ffsjPats    = new Pattern[] { pat10, pat11 };

    public static final Pattern   pat12       = Pattern.compile("(CD\\d+)", Pattern.CASE_INSENSITIVE);
    public static final Pattern   pat13       = Pattern.compile("(part\\d+)", Pattern.CASE_INSENSITIVE);

    public static final Pattern   pat14       = Pattern.compile("(.+)\\.+$");
    public static final Pattern   pat15       = Pattern.compile("(.+)-+$");
    public static final Pattern   pat16       = Pattern.compile("(.+)_+$");

    public static final Pattern   pat17       = Pattern.compile("(.+)\\.\\d+\\.xtm$");

    public static final Pattern   pat18       = Pattern.compile("(.*)\\.isz$", Pattern.CASE_INSENSITIVE);
    public static final Pattern   pat19       = Pattern.compile("(.*)\\.i\\d{2}$", Pattern.CASE_INSENSITIVE);
    public static final Pattern[] iszPats     = new Pattern[] { pat18, pat19 };
    private static boolean        nameCleanup = false;                                                                                         ;

    static {
        nameCleanup = JsonConfig.create(GeneralSettings.class).isCleanUpFilenames();
    }

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
        /**
         * remove isz extensions
         */
        before = name;
        for (Pattern Pat : iszPats) {
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
            int extLength = (name.length() - (lastPoint + 1));
            if (extLength <= 3) {
                name = name.substring(0, lastPoint);
            }
        }
        /* remove ending ., - , _ */
        name = getNameMatch(name, pat14);
        name = getNameMatch(name, pat15);
        name = getNameMatch(name, pat16);

        /* if enabled, replace dots and _ with spaces */
        if (nameCleanup) {
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
