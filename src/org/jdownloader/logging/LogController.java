package org.jdownloader.logging;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jd.Launcher;
import jd.plugins.Plugin;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;
import org.appwork.utils.logging.LogFormatter;

public class LogController {
    private static final LogController INSTANCE    = new LogController();
    private HashMap<String, LogSink>   logSinks    = new HashMap<String, LogSink>();
    private final int                  maxSize;
    private final int                  maxLogs;
    private final long                 logTimeout;
    private Thread                     flushThread = null;
    private final File                 logFolder;
    private ConsoleHandler             consoleHandler;
    /**
     * GL = Generic Logger, returns a shared Logger for name JDownloader
     */
    public static LogSource            GL          = getInstance().getLogger("JDownloader");
    public static LogSource            TRASH       = new LogSource("Trash") {

                                                       @Override
                                                       public synchronized void log(LogRecord record) {
                                                           /* trash */
                                                       }

                                                       @Override
                                                       public String toString() {
                                                           return "Log > /dev/null!";
                                                       }

                                                   };

    /**
     * get the only existing instance of LogController. This is a singleton
     * 
     * @return
     */
    public static LogController getInstance() {
        return LogController.INSTANCE;
    }

    private LogController() {
        consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFormatter(new LogFormatter());
        maxSize = JsonConfig.create(LogConfig.class).getMaxLogFileSize();
        maxLogs = JsonConfig.create(LogConfig.class).getMaxLogFiles();
        logTimeout = JsonConfig.create(LogConfig.class).getLogFlushTimeout() * 1000l;
        logFolder = Application.getResource("logs/" + Launcher.startup + "/");
        if (!logFolder.exists()) logFolder.mkdirs();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                flushSinks();
            }

            @Override
            public String toString() {
                return "flushing logs to disk";
            }

        });
    }

    protected synchronized void startFlushThread() {
        if (flushThread != null && flushThread.isAlive()) return;
        flushThread = new Thread("LogFlushThread") {

            @Override
            public void run() {
                try {
                    while (Thread.currentThread() == flushThread) {
                        try {
                            sleep(logTimeout);
                        } catch (InterruptedException e) {
                            return;
                        }
                        if (Thread.currentThread() == flushThread) {
                            flushSinks();
                        }
                    }
                } finally {
                    synchronized (LogController.this) {
                        if (Thread.currentThread() == flushThread) flushThread = null;
                    }
                }
            }

        };
        flushThread.setDaemon(true);
        flushThread.start();
    }

    protected synchronized void flushSinks() {
        ArrayList<LogSink> logSinks2Flush = null;
        synchronized (logSinks) {
            logSinks2Flush = new ArrayList<LogSink>(logSinks.size());
            Iterator<LogSink> it = logSinks.values().iterator();
            while (it.hasNext()) {
                LogSink next = it.next();
                if (next.hasLogSources()) {
                    logSinks2Flush.add(next);
                } else {
                    next.close();
                    it.remove();
                }
            }
            if (logSinks.size() == 0) {
                flushThread = null;
            }
        }
        for (LogSink sink : logSinks2Flush) {
            try {
                sink.flushSources();
            } catch (final Throwable e) {
            }
        }
    }

    public Logger createLogger(Class<?> clazz) {
        Logger ret = Logger.getLogger(clazz.getName());
        if (ret.getHandlers().length == 0) {
            final ConsoleHandler cHandler = new ConsoleHandler();
            cHandler.setLevel(Level.ALL);
            cHandler.setFormatter(new LogFormatter());
            ret.addHandler(cHandler);
            try {
                Application.getResource("logs").mkdirs();
                Handler fileHandler = new FileHandler(Application.getResource("logs/" + ret.getName()).getAbsolutePath(), 1024 * 1024, 5, true);
                fileHandler.setFormatter(new LogFormatter());
                ret.addHandler(fileHandler);
            } catch (final Exception e) {
                Log.exception(e);
            }
        }
        ret.setLevel(Level.ALL);
        return ret;
    }

    /**
     * CL = Class Logger, returns a logger for calling Class
     * 
     * @return
     */
    public static LogSource CL() {
        return getInstance().getLogger(new Throwable().fillInStackTrace().getStackTrace()[1].getClassName());
    }

    public LogSource getLogger(Plugin loggerForPlugin) {
        return getInstance().getLogger(loggerForPlugin.getHost());
    }

    public LogSource getLogger(String name) {
        LogSink sink = null;
        synchronized (logSinks) {
            sink = logSinks.get(name);
            if (sink == null) {
                sink = new LogSink(name);
                if (consoleHandler != null) {
                    /* add ConsoleHandler to sink, it will add it to it's sources */
                    sink.addHandler(consoleHandler);
                }
                try {
                    Handler fileHandler = new FileHandler(new File(logFolder, name).getAbsolutePath(), maxSize, maxLogs, true);
                    sink.addHandler(fileHandler);
                    fileHandler.setLevel(Level.ALL);
                    fileHandler.setFormatter(new LogFormatter());
                } catch (Throwable e) {
                    if (GL != null) {
                        GL.log(e);
                    } else {
                        e.printStackTrace();
                    }
                }
                logSinks.put(name, sink);
                startFlushThread();
            }
        }
        LogSource source = new LogSource(name, -1);
        source.setInstantFlush(true);
        sink.addLogSource(source);
        return source;
    }

    public void removeConsoleHandler() {
        synchronized (logSinks) {
            if (consoleHandler == null) return;
            Iterator<LogSink> it = logSinks.values().iterator();
            while (it.hasNext()) {
                LogSink next = it.next();
                if (next.hasLogSources()) {
                    next.removeHandler(consoleHandler);
                } else {
                    next.close();
                    it.remove();
                }
            }
            consoleHandler = null;
        }
    }

    public static String getStackTrace(final Throwable thrown) {
        if (thrown == null) return null;
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        thrown.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }
}
