package org.jdownloader.logging;

import java.util.WeakHashMap;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkchecker.LinkCheckerThread;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.http.BrowserSettingsThread;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;

import org.appwork.utils.event.queue.QueueThread;
import org.appwork.utils.logging2.LogConsoleHandler;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSink;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.logging2.LogSourceProvider;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionQueue;

public class LogController extends LogSourceProvider {
    private static final LogController                     INSTANCE = new LogController();
    /**
     * GL = Generic Logger, returns a shared Logger for name JDownloader
     */
    public static LogSource                                TRASH    = new LogSource("Trash") {
                                                                        @Override
                                                                        public synchronized void log(LogRecord record) {
                                                                            /* trash */
                                                                        }

                                                                        @Override
                                                                        public String toString() {
                                                                            return "Log > /dev/null!";
                                                                        }
                                                                    };
    private static volatile WeakHashMap<Thread, LogSource> map      = new WeakHashMap<Thread, LogSource>();

    /**
     * get the only existing instance of org.appwork.utils.logging2.extmanager.LoggerFactory. This is a singleton
     *
     * @return
     */
    public static LogController getInstance() {
        return INSTANCE;
    }

    private LogController() {
        super(System.currentTimeMillis());
        LoggerFactory.I().setDelegate(this);
    }

    public static LogSource CL(Class<?> clazz) {
        LogSource ret = getRebirthLogger();
        if (ret != null) {
            return ret;
        }
        return getInstance().getClassLogger(clazz);
    }

    /**
     * CL = Class Logger, returns a logger for calling Class
     *
     * @return
     */
    public static LogSource CL() {
        return CL(true);
    }

    public static LogSource CL(boolean allowRebirthLogger) {
        final LogSource ret;
        if (allowRebirthLogger && (ret = getRebirthLogger()) != null) {
            return ret;
        } else {
            return getInstance().getCurrentClassLogger();
        }
    }

    public static LogSource getRebirthLogger() {
        LogInterface logger = null;
        Thread currentThread = Thread.currentThread();
        /* fetch logger from map if we have one set for current Thread */
        logger = map.get(currentThread);
        if (logger == null) {
            if (currentThread instanceof LinkCrawlerThread) {
                /* we are inside a LinkCrawlerThread, lets reuse the logger from decrypterPlugin */
                Object owner = ((LinkCrawlerThread) currentThread).getCurrentOwner();
                if (owner != null) {
                    if (owner instanceof PluginForDecrypt) {
                        logger = ((PluginForDecrypt) owner).getLogger();
                    }
                    if (owner instanceof PluginForHost) {
                        logger = ((PluginForHost) owner).getLogger();
                    }
                    if (owner instanceof PluginsC) {
                        logger = ((PluginsC) owner).getLogger();
                    }
                }
            } else if (currentThread instanceof SingleDownloadController) {
                /* we are inside a SingleDownloadController, lets reuse the logger from hosterPlugin */
                logger = ((SingleDownloadController) currentThread).getLogger();
            } else if (currentThread instanceof QueueThread && ((QueueThread) currentThread).getQueue() instanceof ExtractionQueue) {
                /* we are inside an ExtractionController */
                ExtractionController currentExtraction = ((ExtractionQueue) ((QueueThread) currentThread).getQueue()).getCurrentQueueEntry();
                if (currentExtraction != null) {
                    logger = currentExtraction.getLogger();
                }
            } else if (currentThread instanceof LinkCheckerThread) {
                /* we are inside a LinkCheckerThread */
                LinkCheckerThread lc = (LinkCheckerThread) currentThread;
                logger = lc.getLogger();
            } else if (currentThread instanceof BrowserSettingsThread) {
                /* we are inside a BrowserSettingsThread */
                BrowserSettingsThread bst = (BrowserSettingsThread) currentThread;
                logger = bst.getLogger();
            }
        }
        if (logger != null && logger instanceof LogSource) {
            final LogSource ret = (LogSource) logger;
            return ret;
        }
        return null;
    }

    public static LogInterface getRebirthLogger(final LogInterface fallbackLogger) {
        if (fallbackLogger == null) {
            throw new IllegalArgumentException("fallbackLogger is null");
        }
        final LogInterface ret = getRebirthLogger();
        if (ret == null) {
            return fallbackLogger;
        } else {
            return ret;
        }
    }

    public static synchronized void setRebirthLogger(LogSource logger) {
        final Thread currentThread = Thread.currentThread();
        if (logger == null && !map.containsKey(currentThread)) {
            return;
        } else {
            final WeakHashMap<Thread, LogSource> newMap = new WeakHashMap<Thread, LogSource>(map);
            if (logger == null) {
                newMap.remove(currentThread);
            } else {
                newMap.put(currentThread, logger);
            }
            map = newMap;
        }
    }

    @Override
    public LogSource getLogger(String name) {
        LogSource ret = super.getLogger(name);
        ret.setMaxSizeInMemory(512 * 1024);
        return ret;
    }

    public static LogSource getFastPluginLogger(String id) {
        if (LogController.getInstance().isInstantFlushDefault()) {
            return LogController.getInstance().getLogger(id);
        }
        LogSource ret = new LogSource(id) {
            LogSource               log      = null;
            final LogConsoleHandler cHandler = LogController.getInstance().getConsoleHandler();

            @Override
            public synchronized LogSink getLogSink() {
                if (log == null) {
                    log = LogController.getInstance().getLogger(getName());
                }
                return log.getLogSink();
            }

            @Override
            public synchronized Logger getParent() {
                if (log == null) {
                    log = LogController.getInstance().getLogger(getName());
                }
                return log.getParent();
            }

            @Override
            public synchronized void log(LogRecord record) {
                try {
                    if (cHandler != null) {
                        cHandler.publish(record);
                    }
                } finally {
                    super.log(record);
                }
            }

            @Override
            public synchronized void close() {
                try {
                    super.close();
                } finally {
                    if (log != null) {
                        log.close();
                    }
                }
            }

            @Override
            public synchronized void flush() {
                try {
                    super.flush();
                } finally {
                    if (log != null) {
                        log.flush();
                    }
                }
            }
        };
        ret.setMaxSizeInMemory(256 * 1024);
        ret.setAllowTimeoutFlush(false);
        return ret;
    }
}
