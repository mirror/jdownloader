package org.jdownloader.logging;

import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;
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
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkchecker.LinkCheckerThread;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionQueue;

public class LogController {
    private static final LogController INSTANCE    = new LogController();
    private HashMap<String, LogSink>   logSinks    = new HashMap<String, LogSink>();
    private final int                  maxSize;
    private final int                  maxLogs;
    private final long                 logTimeout;
    private Thread                     flushThread = null;
    private final File                 logFolder;
    private ConsoleHandler             consoleHandler;
    private final boolean              instantFlushDefault;
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
        instantFlushDefault = JsonConfig.create(LogConfig.class).isDebugModeEnabled();
        File llogFolder = Application.getResource("logs/" + Launcher.startup + "_" + new SimpleDateFormat("HH.mm").format(new Date(Launcher.startup)) + "/");
        if (llogFolder.exists()) llogFolder = Application.getResource("logs/" + Launcher.startup + "_" + new SimpleDateFormat("HH.mm.ss").format(new Date(Launcher.startup)) + "/");
        if (!llogFolder.exists()) llogFolder.mkdirs();
        logFolder = llogFolder;
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

    public static LogSource CL(Class<?> clazz) {
        LogSource ret = getRebirthLogger();
        if (ret != null) return ret;
        return getInstance().getLogger(clazz.getSimpleName());
    }

    /**
     * CL = Class Logger, returns a logger for calling Class
     * 
     * @return
     */
    public static LogSource CL() {
        LogSource ret = getRebirthLogger();
        if (ret != null) return ret;
        return getInstance().getLogger(new Throwable().fillInStackTrace().getStackTrace()[1].getClassName());
    }

    public static LogSource getRebirthLogger() {
        Logger logger = null;
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof LinkCrawlerThread) {
            /* we are inside a LinkCrawlerThread, lets reuse the logger from decrypterPlugin */
            PluginForDecrypt plugin = ((LinkCrawlerThread) currentThread).getCurrentPlugin();
            if (plugin != null) logger = plugin.getLogger();
        } else if (currentThread instanceof SingleDownloadController) {
            /* we are inside a SingleDownloadController, lets reuse the logger from hosterPlugin */
            logger = ((SingleDownloadController) currentThread).getLogger();
        } else if (currentThread instanceof ExtractionQueue) {
            /* we are inside an ExtractionController */
            ExtractionController currentExtraction = ((ExtractionQueue) currentThread).getCurrentQueueEntry();
            if (currentExtraction != null) logger = currentExtraction.getLogger();
        } else if (currentThread instanceof LinkCheckerThread) {
            /* we are inside a LinkCheckerThread */
            LinkCheckerThread lc = (LinkCheckerThread) currentThread;
            logger = lc.getLogger();
        }
        if (logger != null && logger instanceof LogSource) {
            LogSource ret = (LogSource) logger;
            return ret;
        }
        return null;
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
        source.setInstantFlush(instantFlushDefault);
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

}
