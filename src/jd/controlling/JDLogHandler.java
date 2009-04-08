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
