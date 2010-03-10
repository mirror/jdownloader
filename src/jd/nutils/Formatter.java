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

package jd.nutils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;

import jd.nutils.encoding.Encoding;

public class Formatter {

    /**
     * The format describing an http date.
     */
    private static SimpleDateFormat DATE_FORMAT = null;

    /**
     * Formatiert Sekunden in das zeitformat stunden:minuten:sekunden returns
     * "~" vor values <0
     * 
     * @param eta
     * @return formatierte Zeit
     */
    public static String formatSeconds(long eta) {
        return formatSeconds(eta, true);
    }

    public static String formatSeconds(long eta, boolean showsec) {
        if (eta < 0) return "~";
        long days = eta / (24 * 60 * 60);
        eta -= days * 24 * 60 * 60;
        long hours = eta / (60 * 60);
        eta -= hours * 60 * 60;
        long minutes = eta / 60;
        long seconds = eta - minutes * 60;
        StringBuilder ret = new StringBuilder();
        if (days != 0) ret.append(days).append('d');
        if (hours != 0 || ret.length() != 0) {
            if (ret.length() != 0) ret.append(':');
            ret.append(hours).append('h');
        }
        if (minutes != 0 || ret.length() != 0) {
            if (ret.length() != 0) ret.append(':');
            ret.append(Formatter.fillInteger(minutes, 2, "0")).append('m');
        }
        if (showsec || ret.length() != 0) {
            if (ret.length() != 0) ret.append(':');
            ret.append(Formatter.fillInteger(seconds, 2, "0")).append('s');
        }
        return ret.toString();
    }

    /**
     * FOIrmatiert im format hours:minutes:seconds.ms
     * 
     * @param ms
     */
    public static String formatMilliseconds(long ms) {
        return formatSeconds(ms / 1000) + "." + Formatter.fillInteger(ms % 1000, 3, "0");
    }

    public static String formatFilesize(double value, int size) {
        if (value > 1024 && size < 5) {
            return formatFilesize(value / 1024.0, ++size);
        } else {
            DecimalFormat c = new DecimalFormat("0.00");
            switch (size) {
            case 0:
                return c.format(value) + " B";
            case 1:
                return c.format(value) + " KiB";
            case 2:
                return c.format(value) + " MiB";
            case 3:
                return c.format(value) + " GiB";
            case 4:
                return c.format(value) + " TiB";
            }
        }
        return null;
    }

    public static String formatReadable(long fileSize) {
        if (fileSize < 0) fileSize = 0;
        DecimalFormat c = new DecimalFormat("0.00");
        if (fileSize >= (1024 * 1024 * 1024 * 1024l)) return c.format(fileSize / (1024 * 1024 * 1024 * 1024.0)) + " TiB";
        if (fileSize >= (1024 * 1024 * 1024l)) return c.format(fileSize / (1024 * 1024 * 1024.0)) + " GiB";
        if (fileSize >= (1024 * 1024l)) return c.format(fileSize / (1024 * 1024.0)) + " MiB";
        if (fileSize >= 1024l) return c.format(fileSize / 1024.0) + " KiB";
        return fileSize + " B";
    }

    public static String fillString(String binaryString, String pre, String post, int length) {
        while (binaryString.length() < length) {
            if (binaryString.length() < length) {
                binaryString = pre + binaryString;
            }
            if (binaryString.length() < length) {
                binaryString = binaryString + post;
            }
        }
        return binaryString;
    }

    /**
     * Hängt an i solange fill vorne an bis die zechenlänge von i gleich num ist
     * 
     * @param i
     * @param num
     * @param fill
     * @return aufgefüllte Zeichenkette
     */
    public static String fillInteger(long i, int num, String fill) {
        String ret = "" + i;
        while (ret.length() < num) {
            ret = fill + ret;
        }
        return ret;
    }

    /**
     * GIbt den Integer der sich in src befindet zurück. alle nicht
     * integerzeichen werden ausgefiltert
     * 
     * @param src
     * @return Integer in src
     */
    public static int filterInt(String src) {
        try {
            return Integer.parseInt(Encoding.filterString(src, "1234567890"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static long filterLong(String src) {
        try {
            return Long.parseLong(Encoding.filterString(src, "1234567890"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Returns a string containing an HTTP-formatted date.
     * 
     * @param time
     *            The date to format (current time in msec).
     * @return HTTP date string representing the given time.
     */
    public static String formatTime(long time) {
        if (DATE_FORMAT == null) {
            DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            DATE_FORMAT.setTimeZone(new SimpleTimeZone(0, "GMT"));
            DATE_FORMAT.setLenient(true);
        }
        return DATE_FORMAT.format(new Date(time)).substring(0, 29);
    }

    /**
     * Extracts the Revision from rev. $Revision: 6506 $
     * 
     * @param rev
     * @return
     */
    public static String getRevision(String rev) {
        try {
            int start = rev.indexOf("Revision: ") + 10;
            return rev.substring(start, rev.indexOf(" ", start + 1));
        } catch (Exception e) {
            return "-1";
        }
    }

}
