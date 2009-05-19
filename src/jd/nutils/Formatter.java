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

import jd.http.Encoding;

public class Formatter {

    /**
     * The format describing an http date.
     */
    public static SimpleDateFormat DATE_FORMAT;
    static {
        DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        DATE_FORMAT.setTimeZone(new SimpleTimeZone(0, "GMT"));
        DATE_FORMAT.setLenient(true);
    }

    /**
     * Formatiert Sekunden in das zeitformat stunden:minuten:sekunden
     * 
     * @param eta
     *            toURI().toURL();
     * @return formatierte Zeit
     */
    public static String formatSeconds(long eta) {
        long hours = eta / (60 * 60);
        eta -= hours * 60 * 60;
        long minutes = eta / 60;
        long seconds = eta - minutes * 60;
        if (hours == 0) { return Formatter.fillInteger(minutes, 2, "0") + ":" + Formatter.fillInteger(seconds, 2, "0"); }
        return Formatter.fillInteger(hours, 2, "0") + ":" + Formatter.fillInteger(minutes, 2, "0") + ":" + Formatter.fillInteger(seconds, 2, "0");
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
                return c.format(value) + " KB";
            case 2:
                return c.format(value) + " MB";
            case 3:
                return c.format(value) + " GB";
            case 4:
                return c.format(value) + " TB";
            }
        }
        return null;
    }

    public static String formatReadable(long value) {

        DecimalFormat c = new DecimalFormat("0.00");
        if (value >= (1024 * 1024 * 1024 * 1024l)) return c.format(value / (1024 * 1024 * 1024 * 1024.0)) + " TB";
        if (value >= (1024 * 1024 * 1024l)) return c.format(value / (1024 * 1024 * 1024.0)) + " GB";
        if (value >= (1024 * 1024l)) return c.format(value / (1024 * 1024.0)) + " MB";
        if (value >= 1024l) return c.format(value / 1024.0) + " KB";

        return value + " B";
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
     * 
     * @return HTTP date string representing the given time.
     */
    public static String formatTime(long time) {
        return DATE_FORMAT.format(new Date(time)).substring(0, 29);
    }

}
