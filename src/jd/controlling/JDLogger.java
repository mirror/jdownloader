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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import jd.utils.JDUtilities;

/**
 * The logging class for jDownloader. The logger will put the log to the
 * JDownloader.log file located in the JDownloader home directory and also to
 * the console. The console output will be removed, if you start JDownloader
 * without -DEBUG.
 * 
 * The logger is a singleton and not instantiateable. You will get the logger
 * with JDLogger.getLogger.
 */
public final class JDLogger {
    private JDLogger() {
    }

    private static Logger LOGGER = null;
    public static final String LOGGER_NAME = "JDownloader";
    private static ConsoleHandler console;
    private static FileHandler filehandler;
    private static String logpath;

    /**
     * Liefert die Klasse zurück, mit der Nachrichten ausgegeben werden können
     * Falls dieser Logger nicht existiert, wird ein neuer erstellt
     * 
     * @return LogKlasse
     */
    public static Logger getLogger() {
        if (LOGGER == null) {
            LOGGER = Logger.getLogger(LOGGER_NAME);
            final Formatter formatter = new LogFormatter();
            LOGGER.setUseParentHandlers(false);

            console = new ConsoleHandler();
            console.setLevel(Level.ALL);
            console.setFormatter(formatter);
            LOGGER.addHandler(console);

            LOGGER.setLevel(Level.ALL);

            try {
                logpath = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/JDownloader.log";
                filehandler = new FileHandler(logpath);
                filehandler.setFormatter(formatter);
                LOGGER.addHandler(filehandler);
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return LOGGER;
    }

    /**
     * Adds a message with the time to the log.
     * 
     * @param The
     *            message.
     */
    public static void timestamp(final String msg) {
        getLogger().warning(jd.nutils.Formatter.formatMilliseconds(System.currentTimeMillis()) + " : " + msg);
    }

    /**
     * Adds a exception message to the log with the logging level SERVE.
     * 
     * @param The
     *            exception.
     */
    public static void exception(final Throwable e) {
        exception(Level.SEVERE, e);
    }

    /**
     * Removes the consoleoutput from the logger.
     */
    public static void removeConsoleHandler() {
        if (console != null) {
            getLogger().removeHandler(console);
        }
        System.err.println("Removed Consolehandler. Start with -debug to see console output");
    }

    /**
     * Adds a haeder in the log.
     * 
     * @param The
     *            name of the header.
     */
    public static void addHeader(final String string) {
        getLogger().info("\r\n\r\n--------------------------------------" + string + "-----------------------------------");
    }

    /**
     * Adds a exception message to the log.
     * 
     * @param The
     *            log level for the exception.
     * @param The
     *            exception.
     */
    public static void exception(final Level level, final Throwable e) {
        getLogger().log(level, level.getName() + " Exception occurred", e);
    }

    /**
     * Prints the position form the caller.
     */
    public static void quickLog() {
        System.out.println("Footstep: " + new Exception().getStackTrace()[1]);
    }

    /**
     * Adda a warning message to the log.
     * 
     * @param The
     *            toString will be logged.
     */
    static public void warning(final Object o) {
        getLogger().warning(o.toString());
    }

    /**
     * Converts a exception to the stacktrace.
     * 
     * @param The
     *            exception.
     * @return The stacktrace of the exceptions.
     */
    public static String getStackTrace(final Throwable thrown) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        thrown.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    /**
     * Reads and formats the log from the log file.
     * 
     * @return The log file.
     */
    public static String getLog() {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new FileReader(logpath));
            String input;

            while ((input = in.readLine()) != null) {
                sb.append(input + "\n");
            }

            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }
}