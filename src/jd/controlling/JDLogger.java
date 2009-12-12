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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class JDLogger {
    /**
     * Don't let anyone instantiate this class.
     */
    protected JDLogger() {
    }

    private static Logger LOGGER = null;
    public static final String LOGGER_NAME = "java_downloader";
    private static ConsoleHandler console;

    /**
     * Liefert die Klasse zurück, mit der Nachrichten ausgegeben werden können
     * Falls dieser Logger nicht existiert, wird ein neuer erstellt
     * 
     * @return LogKlasse
     */
    public static Logger getLogger() {

        if (LOGGER == null) {

            LOGGER = Logger.getLogger(LOGGER_NAME);
            Formatter formatter = new LogFormatter();
            LOGGER.setUseParentHandlers(false);

            console = new ConsoleHandler();
            console.setLevel(Level.ALL);
            console.setFormatter(formatter);
            LOGGER.addHandler(console);

            LOGGER.setLevel(Level.ALL);
            LOGGER.addHandler(JDLogHandler.getHandler());
            JDLogHandler.getHandler().setFormatter(formatter);

        }
        return LOGGER;
    }

    public static void timestamp(String msg) {
        getLogger().warning(jd.nutils.Formatter.formatMilliseconds(System.currentTimeMillis()) + " : " + msg);
    }

    public static void exception(Throwable e) {
        exception(Level.SEVERE, e);
    }

    public static void removeConsoleHandler() {
        if (console != null) getLogger().removeHandler(console);
        System.err.println("Removed Consolehandler. Start with -debug to see console output");
    }

    public static void addHeader(String string) {
        getLogger().info("\r\n\r\n--------------------------------------" + string + "-----------------------------------");
    }

    public static void exception(Level level, Throwable e) {
        getLogger().log(level, level.getName() + " Exception occurred", e);
    }

    public static void quickLog() {
        System.out.println("Footstep: " + new Exception().getStackTrace()[1]);
    }

    static public void warning(Object o) {
        getLogger().warning(o.toString());
    }

    /**
     * Returns a StackTrace of an Exception
     * 
     * @param thrown
     * @return
     */
    public static String getStackTrace(Throwable thrown) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        thrown.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    /**
     * Retuns al log entries as string.. filtered with loglevel level
     * 
     * @param all
     * @return
     */
    public static String getLog(Level level) {
        Level tmp = getLogger().getLevel();
        getLogger().setLevel(level);
        try {
            ArrayList<LogRecord> buff = JDLogHandler.getHandler().getBuffer();
            StringBuilder sb = new StringBuilder();
            for (LogRecord lr : buff) {
                sb.append(JDLogHandler.getHandler().getFormatter().format(lr));
            }
            return sb.toString();
        } finally {
            getLogger().setLevel(tmp);
        }
    }

}
