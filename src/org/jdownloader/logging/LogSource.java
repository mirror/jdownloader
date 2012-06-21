package org.jdownloader.logging;

import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.appwork.utils.Exceptions;
import org.appwork.utils.logging.ExceptionDefaultLogLevel;

public class LogSource extends Logger {

    private ArrayList<LogRecord> records           = new ArrayList<LogRecord>();
    private int                  maxLogRecordsInMemory;
    private int                  flushCounter      = 0;
    private int                  recordsCounter    = 0;
    private boolean              closed            = false;
    private boolean              allowTimeoutFlush = true;
    private boolean              instantFlush      = false;

    public boolean isAllowTimeoutFlush() {
        return allowTimeoutFlush;
    }

    public void setAllowTimeoutFlush(boolean allowTimeoutFlush) {
        this.allowTimeoutFlush = allowTimeoutFlush;
    }

    protected LogSource(String name, String resourceBundleName) {
        super(name, resourceBundleName);
    }

    /*
     * creates a LogCollector with given name
     * 
     * maxLogRecordsInMemory defines how many log records this logger will buffer in memory before logging to parent's handlers
     * 
     * <0 = unlimited in memory, manual flush needed
     * 
     * 0 = forward directly to parent's handlers
     * 
     * >0 = limited
     */
    public LogSource(String name, int maxLogRecordsInMemory) {
        this(name, (String) null);
        this.maxLogRecordsInMemory = maxLogRecordsInMemory;
        super.setUseParentHandlers(false);
        this.setLevel(Level.ALL);
    }

    @Override
    public void setUseParentHandlers(boolean useParentHandlers) {
        /* do not allow to change this */
    }

    public LogSource(String name) {
        this(name, -1);
    }

    public static void exception(Logger logger, Throwable e) {
        if (logger == null || e == null) return;
        if (logger instanceof LogSource) {
            ((LogSource) logger).log(e);
        } else {
            logger.severe(e.getMessage());
            logger.severe(Exceptions.getStackTrace(e));
        }
    }

    public void log(Throwable e) {
        if (e == null) {
            e = new NullPointerException("e is null");
        }
        Level lvl = null;
        if (e instanceof ExceptionDefaultLogLevel) {
            lvl = ((ExceptionDefaultLogLevel) e).getDefaultLogLevel();
        }
        if (lvl == null) {
            lvl = Level.SEVERE;
        }
        log(new LogRecord(lvl, Exceptions.getStackTrace(e)));
    }

    @Override
    public synchronized void log(LogRecord record) {
        if (closed || record == null) return;
        /* make sure we have gathered all information about current class/method */
        /* this will collect current class/method if net set yet */
        record.getSourceClassName();
        if (maxLogRecordsInMemory == 0 || instantFlush) {
            /* maxLogRecordsInMemory == 0, we want to use parent's handlers */
            Logger parent = this.getParent();
            if (parent != null) {
                for (Handler handler : parent.getHandlers()) {
                    synchronized (handler) {
                        handler.publish(record);
                    }
                }
            }
            return;
        } else if (maxLogRecordsInMemory > 0 && records.size() == maxLogRecordsInMemory) {
            /* maxLogRecordsInMemory >0 we have limited max records in memory */
            /* we flush in case we reached maxLogRecordsInMemory */
            flush();
        }
        if (records == null) {
            /* records will be null at first use or after a flush */
            records = new ArrayList<LogRecord>();
        }
        records.add(record);
        recordsCounter++;
        super.log(record);
    }

    public int getMaxLogRecordsInMemory() {
        return maxLogRecordsInMemory;
    }

    public synchronized void setMaxLogRecordsInMemory(int newMax) {
        if (maxLogRecordsInMemory == newMax) return;
        flush();
        maxLogRecordsInMemory = newMax;
    }

    public synchronized void flush() {
        if (closed) return;
        if (records == null || records.size() == 0) {
            records = null;
            return;
        }
        Logger parent = this.getParent();
        if (parent != null) {
            for (Handler handler : parent.getHandlers()) {
                if (handler instanceof ConsoleHandler) {
                    /* we dont want logRecords to appear twice on console */
                    continue;
                }
                synchronized (handler) {
                    for (LogRecord record : records) {
                        handler.publish(record);
                    }
                }
            }
            flushCounter++;
        }
        records = null;
    }

    public synchronized void close() {
        flush();
        closed = true;
    }

    protected boolean isClosed() {
        return closed;
    }

    public synchronized void clear() {
        records = null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        synchronized (this) {
            sb.append("Log:" + this.getName() + " Records:" + recordsCounter + " Flushed:" + flushCounter);
            if (records != null && records.size() > 0) {
                sb.append("\r\n");
                LogFormatter formatter = new LogFormatter();
                formatter.setFormatterStringBuilder(sb);
                for (LogRecord record : records) {
                    sb.append(formatter.format(record));
                }
            }
        }
        return sb.toString();
    }

    /**
     * @return the instantFlush
     */
    public boolean isInstantFlush() {
        return instantFlush;
    }

    /**
     * @param instantFlush
     *            the instantFlush to set
     */
    public void setInstantFlush(boolean instantFlush) {
        if (this.instantFlush == instantFlush) return;
        if (instantFlush) {
            flush();
        }
        this.instantFlush = instantFlush;
    }

}
