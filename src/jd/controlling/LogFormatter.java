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

package jd.controlling;

import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Mit dieser Klasse können die Logmeldungen anders dargestellt werden. Der Code
 * wurde hauptsächlich aus SimpleFormatter übernommen. Lediglich die Format
 * Methode wurde ein wenig umgestellt.
 */
public class LogFormatter extends SimpleFormatter {

    private final Date       dat           = new Date();
    private final DateFormat longTimestamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    /**
     * Line separator string. This is the value of the line.separator property
     * at the moment that the SimpleFormatter was created.
     */
    private final String     lineSeparator = System.getProperty("line.separator");
    private int              lastThreadID;

    @Override
    public synchronized String format(LogRecord record) {
        /* clear StringBuilder buffer */
        final StringBuilder sb = new StringBuilder();

        // Minimize memory allocations here.
        dat.setTime(record.getMillis());

        String message = formatMessage(record);
        int th = record.getThreadID();
        if (th != lastThreadID) {
            sb.append("\r\n------------------------  Thread: ");
            sb.append(th);
            sb.append("  -----------------------\r\n");
        }
        lastThreadID = th;
        if (JDLogger.getLogger().getLevel() == Level.ALL) {
            sb.append(record.getThreadID());
            sb.append(' ');

            sb.append(longTimestamp.format(dat));
            sb.append(" - ");
            sb.append(record.getLevel().getName());
            sb.append(" [");
            if (record.getSourceClassName() != null) {
                sb.append(record.getSourceClassName());
            } else {
                sb.append(record.getLoggerName());
            }
            if (record.getSourceMethodName() != null) {
                sb.append('(');
                sb.append(record.getSourceMethodName());
                sb.append(')');
            }

            sb.append("] ");

            sb.append("-> ");
        } else {
            sb.append(longTimestamp.format(dat));
            sb.append(" - ");
            if (record.getSourceClassName() != null) {
                sb.append(record.getSourceClassName().substring(record.getSourceClassName().lastIndexOf(".") + 1));
            } else {
                sb.append(record.getLoggerName());
            }
            sb.append("-> ");
        }
        sb.append(message);
        sb.append(lineSeparator);
        if (record.getThrown() != null) {
            sb.append(JDLogger.getStackTrace(record.getThrown()));
        }
        return sb.toString();
    }

}
