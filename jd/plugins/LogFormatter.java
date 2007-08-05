package jd.plugins;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Mit dieser Klasse können die Logmeldungen anders dargestellt werden.
 * Der Code wurde hauptsächlich aus SimpleFormatter übernommen.
 * Lediglich die Format Methode wurde ein wenig umgestellt.
 * 
 * @author astaldo
 *
 */
public class LogFormatter extends SimpleFormatter{

    Date dat = new Date();
    private MessageFormat formatter;
    private final static String format = "{0,date} {0,time}";
    private Object args[] = new Object[1];
    // Line separator string.  This is the value of the line.separator
    // property at the moment that the SimpleFormatter was created.
    private String lineSeparator = System.getProperty("line.separator");

    @Override
    public synchronized String format(LogRecord record) {

        StringBuffer sb = new StringBuffer();

        // Minimize memory allocations here.
        dat.setTime(record.getMillis());
        args[0] = dat;

        StringBuffer text = new StringBuffer();
        if (formatter == null) {
            formatter = new MessageFormat(format);
        }
        formatter.format(args, text, null);
        sb.append(text);
        sb.append(" - ");
        sb.append(record.getLevel().getLocalizedName());
        sb.append(" [");
        if (record.getSourceClassName() != null) {
            sb.append(record.getSourceClassName());
        }
        else {
            sb.append(record.getLoggerName());
        }
        if (record.getSourceMethodName() != null) {
            sb.append("(");
            sb.append(record.getSourceMethodName());
            sb.append(")");
        }
        sb.append("] ");
        String message = formatMessage(record);
        sb.append("-> ");
        sb.append(message);
        sb.append(lineSeparator);
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            }
            catch (Exception ex) {}
        }
        return sb.toString();
    }
}
