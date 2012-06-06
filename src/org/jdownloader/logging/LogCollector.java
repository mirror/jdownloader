package org.jdownloader.logging;

import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogCollector extends Logger {

    private ArrayList<LogRecord> records        = new ArrayList<LogRecord>();
    private int                  maxLogRecordsInMemory;
    private int                  flushCounter   = 0;
    private int                  recordsCounter = 0;
    private boolean              closed         = false;

    protected LogCollector(String name, String resourceBundleName) {
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
    public LogCollector(String name, int maxLogRecordsInMemory) {
        this(name, (String) null);
        this.maxLogRecordsInMemory = maxLogRecordsInMemory;
        this.setUseParentHandlers(true);
    }

    public LogCollector(String name) {
        this(name, -1);
    }

    @Override
    public synchronized void log(LogRecord record) {
        if (closed || record == null) return;
        /* make sure we have gathered all information about current class/method */
        /* this will collect current class/method if net set yet */
        record.getSourceClassName();
        if (maxLogRecordsInMemory == 0) {
            /* maxLogRecordsInMemory == 0, we want to use parent's handlers */
            super.log(record);
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
    }

    public synchronized void flush() {
        if (closed) return;
        if (records == null) return;
        Logger parent = this.getParent();
        if (parent != null) {
            for (Handler handler : parent.getHandlers()) {
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
}
