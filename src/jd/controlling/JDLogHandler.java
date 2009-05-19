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

import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import jd.event.ControlEvent;
import jd.utils.JDUtilities;

public class JDLogHandler extends Handler {

    private static JDLogHandler HANDLER = null;
    private ArrayList<LogRecord> buffer;

    private JDLogHandler() {
        super();
        buffer = new ArrayList<LogRecord>();
    }

    public ArrayList<LogRecord> getBuffer() {
        return buffer;
    }

    public void close() {
    }

    public void flush() {
    }

    public void publish(LogRecord logRecord) {

        this.buffer.add(logRecord);
        if (JDUtilities.getController() != null) {
            JDUtilities.getController().fireControlEvent(ControlEvent.CONTROL_LOG_OCCURED, logRecord);
        }
    }

    public static JDLogHandler getHandler() {
        if (HANDLER == null) createHandler();
        return HANDLER;
    }

    private static void createHandler() {
        HANDLER = new JDLogHandler();

    }

}
