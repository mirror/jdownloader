package jd.controlling;

import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jd.JDInitFlags;

import org.appwork.utils.logging.FileLogFormatter;

public class JDPluginLogger extends Logger {

    private ArrayList<LogRecord>       records   = new ArrayList<LogRecord>();

    public static final JDPluginLogger Trash     = new JDPluginLogger("TRASH", true);
    private final FileLogFormatter     formatter = new FileLogFormatter();

    public JDPluginLogger(String name) {
        this(name, false);
    }

    public JDPluginLogger(final String name, final boolean trash) {
        this(name, null);
        this.setUseParentHandlers(false);
        this.setLevel(Level.ALL);
        if (trash) return;
        if (JDInitFlags.SWITCH_FORCELOG) {
            for (Handler handler : JDLogger.getLogger().getHandlers()) {
                addHandler(handler);
            }
        }
        this.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                synchronized (records) {
                    records.add(record);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }

        });
    }

    protected JDPluginLogger(String name, String resourceBundleName) {
        super(name, resourceBundleName);
    }

    public void clear() {
        synchronized (records) {
            records.clear();
        }
    }

    public void logInto(Logger logger) {
        if (JDInitFlags.SWITCH_FORCELOG) {
            /* the main logger already contains all logs */
            return;
        }
        if (logger == null) return;
        synchronized (records) {
            for (LogRecord record : records) {
                logger.log(record);
            }
        }
    }

    @Override
    public String toString() {
        if (records.size() == 0) return "empty logger";
        StringBuilder sb = new StringBuilder();
        synchronized (records) {
            for (LogRecord record : records) {
                sb.append(formatter.format(record));
            }
        }
        return sb.toString();
    }

}
