package org.jdownloader.logging;

import java.text.DateFormat;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import org.appwork.utils.Exceptions;

public class LogFormatter extends SimpleFormatter {

    private final Date       dat                    = new Date();
    private final DateFormat longTimestamp          = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

    private int              lastThreadID;

    protected StringBuilder  formatterStringBuilder = null;

    public StringBuilder getFormatterStringBuilder() {
        return formatterStringBuilder;
    }

    public void setFormatterStringBuilder(StringBuilder formatterStringBuilder) {
        this.formatterStringBuilder = formatterStringBuilder;
    }

    @Override
    public synchronized String format(LogRecord record) {
        StringBuilder sb = formatterStringBuilder;
        if (sb == null) {
            /* create new local StringBuilder in case we don't have once set externally */
            sb = new StringBuilder();
        }
        // Minimize memory allocations here.
        dat.setTime(record.getMillis());
        String message = formatMessage(record);
        int th = record.getThreadID();
        if (th != lastThreadID) {
            sb.append("------------------------Thread: ");
            sb.append(th);
            /* This piece of code also appends the name of current thread into log */
            if (Thread.currentThread().getId() == th) {
                sb.append(":" + Thread.currentThread().getName());
            }
            sb.append("-----------------------\r\n");
        }
        lastThreadID = th;
        /* we have this line for easier logfile purifier :) */
        sb.append("--ID:" + th + "TS:" + record.getMillis() + "-");
        sb.append(longTimestamp.format(dat));
        sb.append(" - ");
        sb.append(" [");
        String tmp = null;
        if ((tmp = record.getSourceClassName()) != null) {
            sb.append(tmp);
        } else {
            sb.append(record.getLoggerName());
        }
        if ((tmp = record.getSourceMethodName()) != null) {
            sb.append('(');
            sb.append(tmp);
            sb.append(')');
        }
        sb.append("] ");
        sb.append("-> ");
        sb.append(message);
        sb.append("\r\n");
        if (record.getThrown() != null) {
            Exceptions.getStackTrace(sb, record.getThrown());
            sb.append("\r\n");
        }
        if (formatterStringBuilder == sb) return "";
        return sb.toString();
    }
}
