package org.jdownloader.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Scanner;

import jd.parser.Regex;

public class SubtitleConverter {
    /**
     * Converts the the time of the Google format to the SRT format.
     * 
     * @param time
     *            . The time from the Google XML.
     * @return The converted time as String.
     */
    private static String convertSubtitleTime(Double time) {
        String hour = "00";
        String minute = "00";
        String second = "00";
        String millisecond = "0";

        Integer itime = Integer.valueOf(time.intValue());

        // Hour
        Integer timeHour = Integer.valueOf(itime.intValue() / 3600);
        if (timeHour < 10) {
            hour = "0" + timeHour.toString();
        } else {
            hour = timeHour.toString();
        }

        // Minute
        Integer timeMinute = Integer.valueOf((itime.intValue() % 3600) / 60);
        if (timeMinute < 10) {
            minute = "0" + timeMinute.toString();
        } else {
            minute = timeMinute.toString();
        }

        // Second
        Integer timeSecond = Integer.valueOf(itime.intValue() % 60);
        if (timeSecond < 10) {
            second = "0" + timeSecond.toString();
        } else {
            second = timeSecond.toString();
        }

        // Millisecond
        millisecond = String.valueOf(time - itime).split("\\.")[1];
        if (millisecond.length() == 1) millisecond = millisecond + "00";
        if (millisecond.length() == 2) millisecond = millisecond + "0";
        if (millisecond.length() > 2) millisecond = millisecond.substring(0, 3);

        // Result
        String result = hour + ":" + minute + ":" + second + "," + millisecond;

        return result;
    }

    /**
     * Converts the Google Closed Captions subtitles to SRT subtitles. It runs after the completed download.
     * 
     * @param out
     * 
     * @param downloadlink
     *            . The finished link to the Google CC subtitle file.
     * @return The success of the conversion.
     */
    public static boolean convertGoogleCC2SRTSubtitles(final File in, File out) {

        BufferedWriter dest = null;
        FileOutputStream fos = null;
        try {
            try {

                dest = new BufferedWriter(new OutputStreamWriter(fos = new FileOutputStream(out), "UTF-8"));
            } catch (IOException e1) {
                return false;
            }

            final StringBuilder xml = new StringBuilder();
            int counter = 1;
            final String lineseparator = System.getProperty("line.separator");

            FileInputStream fis = null;
            try {
                Scanner scan = new Scanner(new InputStreamReader(fis = new FileInputStream(in), "UTF-8"));
                while (scan.hasNext()) {
                    xml.append(scan.nextLine() + lineseparator);
                }
            } catch (Exception e) {
                return false;
            } finally {
                try {
                    fis.close();
                } catch (final Throwable e) {
                }
            }

            String[][] matches = new Regex(xml.toString(), "<text start=\"(.*?)\".*?(dur=\"(.*?)\")?>(.*?)</text>").getMatches();

            try {
                String[] prevMatch = null;
                for (String[] match : matches) {
                    if (prevMatch != null) {
                        String[] lastMatch = prevMatch;
                        prevMatch = null;
                        dest.write(counter++ + lineseparator);
                        Double start = Double.valueOf(lastMatch[0]);
                        Double end = Double.valueOf(match[0]);
                        dest.write(convertSubtitleTime(start) + " --> " + convertSubtitleTime(end) + lineseparator);
                        String text = lastMatch[3].trim();
                        text = text.replaceAll(lineseparator, " ");
                        text = text.replaceAll("&amp;", "&");
                        text = text.replaceAll("&quot;", "\"");
                        text = text.replaceAll("&#39;", "'");
                        dest.write(text + lineseparator + lineseparator);
                    }
                    if (match[1] == null) {
                        /* no end timestamp */
                        prevMatch = match;
                        continue;
                    }
                    /* we have start/end timestamps */
                    dest.write(counter++ + lineseparator);
                    Double start = Double.valueOf(match[0]);
                    Double end = start + Double.valueOf(match[2]);
                    dest.write(convertSubtitleTime(start) + " --> " + convertSubtitleTime(end) + lineseparator);
                    String text = match[3].trim();
                    text = text.replaceAll(lineseparator, " ");
                    text = text.replaceAll("&amp;", "&");
                    text = text.replaceAll("&quot;", "\"");
                    text = text.replaceAll("&#39;", "'");
                    dest.write(text + lineseparator + lineseparator);
                }
            } catch (Exception e) {
                return false;
            }
        } finally {
            try {
                dest.close();
            } catch (Throwable e) {
            }
            try {
                fos.close();
            } catch (Throwable e) {
            }
        }
        in.delete();
        return true;
    }
}
