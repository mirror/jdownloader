package org.jdownloader.logging;

import java.util.HashMap;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkchecker.LinkCheckerThread;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.http.BrowserSettingsThread;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;

import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.logging2.LogSourceProvider;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionQueue;

public class LogController extends LogSourceProvider {
    private static final LogController        INSTANCE = new LogController();

    /**
     * GL = Generic Logger, returns a shared Logger for name JDownloader
     */
    public static LogSource                   GL       = getInstance().getLogger("JDownloader");
    public static LogSource                   TRASH    = new LogSource("Trash") {

                                                           @Override
                                                           public synchronized void log(LogRecord record) {
                                                               /* trash */
                                                           }

                                                           @Override
                                                           public String toString() {
                                                               return "Log > /dev/null!";
                                                           }

                                                       };

    private static HashMap<Thread, LogSource> map      = new HashMap<Thread, LogSource>();

    /**
     * get the only existing instance of LogController. This is a singleton
     * 
     * @return
     */
    public static LogController getInstance() {
        return LogController.INSTANCE;
    }

    private LogController() {
        super(System.currentTimeMillis());

    }

    public static LogSource CL(Class<?> clazz) {
        LogSource ret = getRebirthLogger();
        if (ret != null) return ret;
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
        LogSource ret = null;
        if (allowRebirthLogger && (ret = getRebirthLogger()) != null) return ret;
        return getInstance().getCurrentClassLogger();
    }

    public static LogSource getRebirthLogger() {
        Logger logger = null;
        Thread currentThread = Thread.currentThread();
        /* fetch logger from map if we have one set for current Thread */
        logger = map.get(currentThread);
        if (logger == null) {
            if (currentThread instanceof LinkCrawlerThread) {
                /* we are inside a LinkCrawlerThread, lets reuse the logger from decrypterPlugin */
                Object owner = ((LinkCrawlerThread) currentThread).getCurrentOwner();
                if (owner != null) {
                    if (owner instanceof PluginForDecrypt) logger = ((PluginForDecrypt) owner).getLogger();
                    if (owner instanceof PluginForHost) logger = ((PluginForHost) owner).getLogger();
                    if (owner instanceof PluginsC) logger = ((PluginsC) owner).getLogger();
                }
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
            } else if (currentThread instanceof BrowserSettingsThread) {
                /* we are inside a BrowserSettingsThread */
                BrowserSettingsThread bst = (BrowserSettingsThread) currentThread;
                logger = bst.getLogger();
            }
        }
        if (logger != null && logger instanceof LogSource) {
            LogSource ret = (LogSource) logger;
            return ret;
        }
        return null;
    }

    public static synchronized void setRebirthLogger(LogSource logger) {
        Thread currentThread = Thread.currentThread();
        HashMap<Thread, LogSource> newMap = new HashMap<Thread, LogSource>(map);
        if (logger == null) {
            newMap.remove(currentThread);
        } else {
            newMap.put(currentThread, logger);
        }
        map = newMap;
    }

    public LogSource getLogger(Plugin loggerForPlugin) {
        return getInstance().getLogger(loggerForPlugin.getHost());
    }

}
