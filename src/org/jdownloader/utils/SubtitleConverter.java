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

/**
 * Parse the Google XML subtitles into the SRT-format
 *
 * @author coalado, Max
 *
 */
public class SubtitleConverter {

    private final static String     LINE_SEPERATOR = System.getProperty("line.separator");
    private final static String[][] REPLACE        = { { LINE_SEPERATOR, " " }, { "&amp;", "&" }, { "&quot;", "\"" }, { "&#39;", "'" } };

    /**
     * Converts the the time of the Google format to the SRT format.
     *
     * @param time
     *            . The time from the Google XML.
     * @return The converted time as String.
     */
    private static String convertSubtitleTime(double time) {

        // its important to use (int) to cut off the milliseconds instead of rounding to the nearest int.
        int itime = (int) time;

        String hour = leadingZero(itime / 3600, 2);
        String minute = leadingZero((itime % 3600) / 60, 2);
        String second = leadingZero(itime % 60, 2);
        String millisecond = leadingZero((int) ((time - itime) * 1000), 3);

        // Result
        String result = hour + ":" + minute + ":" + second + "," + millisecond;

        return result;
    }

    /**
     *
     * @param number
     *            number that shell be converted
     * @param digits
     *            count of total digits the number should have.
     * @return A number String with leading zeros
     */
    private static String leadingZero(int number, int digits) {

        int numberDigits = String.valueOf(number).length();

        assert numberDigits <= digits : "The number is bigger then expected!";

        String stringNumber = String.format("%0" + digits + "d", number);

        return stringNumber;
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

            FileInputStream fis = null;
            try {
                Scanner scan = new Scanner(new InputStreamReader(fis = new FileInputStream(in), "UTF-8"));
                while (scan.hasNext()) {
                    xml.append(scan.nextLine() + LINE_SEPERATOR);
                }
                scan.close();
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

                        writeNewLine(dest, prevMatch[0], match[0], prevMatch[3], counter++);
                        prevMatch = null;
                    }
                    if (match[1] == null) {
                        /* no end time stamp */
                        prevMatch = match;
                        continue;
                    }
                    /* we have start/end time stamps */
                    writeNewLine(dest, match[0], match[2], match[3], counter++);
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

    /**
     * Writes a subtitle line in the SRT format
     *
     * @param dest
     *            the function will use this writer
     * @param from
     *            subtitle start point
     * @param duration
     *            subtitle duration
     * @param text
     *            text of the subtitle
     * @param counter
     *            number of the subtitle text
     * @throws IOException
     *             in case of a problem with the writer
     */
    private static void writeNewLine(BufferedWriter dest, String from, String duration, String text, int counter) throws IOException {

        dest.write(counter + LINE_SEPERATOR);
        double start = Double.valueOf(from);
        double end = start + Double.valueOf(duration);

        // assert start <= end : "The subtitles can only proceed parallel to our time line.";

        dest.write(convertSubtitleTime(start) + " --> " + convertSubtitleTime(end) + LINE_SEPERATOR);

        text = text.trim();

        for (String[] replaceString : REPLACE) {
            text = text.replaceAll(replaceString[0], replaceString[1]);
        }

        dest.write(text + LINE_SEPERATOR + LINE_SEPERATOR);
    }
}