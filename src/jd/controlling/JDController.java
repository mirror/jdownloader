//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.controlling;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;

import jd.Main;
import jd.config.DatabaseConnector;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.nutils.io.JDIO;
import jd.plugins.DownloadLink;
import jd.plugins.PluginsC;
import jd.utils.JDUtilities;

import org.appwork.utils.event.Eventsender;
import org.jdownloader.container.D;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.update.RestartController;

/**
 * Im JDController wird das ganze App gesteuert. Events werden deligiert.
 * 
 * @author JD-Team/astaldo
 */
public class JDController {

    public static JDController getInstance() {
        return INSTANCE;
    }

    private class EventSender extends Eventsender<ControlListener, ControlEvent> {

        protected static final long MAX_EVENT_TIME = 10000;
        private ControlListener     currentListener;
        private ControlEvent        event;
        private long                eventStart     = 0;
        public boolean              waitFlag       = true;
        private Thread              watchDog;
        private Thread              runDog;
        private final Object        LOCK           = new Object();

        public Object getLOCK() {
            return LOCK;
        }

        public void handleEvent(final ControlEvent event) {
            currentListener = null;
            try {
                fireEvent(event);
            } catch (final Throwable e) {
                JDLogger.exception(e);
            }
        }

        public EventSender() {
            runDog = new Thread("EventSender:runDog") {
                @Override
                public void run() {
                    eventStart = 0;
                    while (true) {
                        synchronized (LOCK) {
                            while (waitFlag) {
                                try {
                                    LOCK.wait();
                                } catch (final Exception e) {
                                    JDLogger.exception(e);
                                }
                            }
                        }
                        try {
                            synchronized (eventQueue) {
                                if (eventQueue.size() > 0) {
                                    event = eventQueue.remove(0);
                                } else {
                                    eventStart = 0;
                                    event = null;
                                    waitFlag = true;
                                }
                            }
                            if (event == null) continue;
                            handleEvent(event);
                        } catch (final Exception e) {
                            JDLogger.exception(e);
                            eventStart = 0;
                        }
                    }
                }
            };
            runDog.start();
            watchDog = new Thread("EventSender:watchDog") {
                @Override
                public void run() {
                    while (true) {
                        if (eventStart > 0 && System.currentTimeMillis() - eventStart > MAX_EVENT_TIME) {
                            LOGGER.finer("WATCHDOG: Execution Limit reached");
                            LOGGER.finer("ControlListener: " + currentListener);
                            LOGGER.finer("Event: " + event);
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (final InterruptedException e) {
                            JDLogger.exception(e);
                            return;
                        }
                    }
                }

            };
            watchDog.start();
        }

        @Override
        protected void fireEvent(final ControlListener listener, final ControlEvent event) {
            if (Thread.currentThread() == runDog) {
                /* only runDog should be watched by watchDog :p */
                eventStart = System.currentTimeMillis();
                try {
                    currentListener = listener;
                    this.event = event;
                    listener.controlEvent(event);
                } catch (final Exception e) {
                    JDLogger.exception(e);
                }
                eventStart = 0;
            } else {
                try {
                    listener.controlEvent(event);
                } catch (final Exception e) {
                    JDLogger.exception(e);
                }
            }
        }

    }

    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #fireControlEvent(ControlEvent)} ein Event losgeschickt wird.
     */

    private final ArrayList<ControlEvent> eventQueue   = new ArrayList<ControlEvent>();

    private EventSender                   eventSender  = null;

    /**
     * Der Logger
     */
    private static final Logger           LOGGER       = JDLogger.getLogger();

    /**
     * Der Download Watchdog verwaltet die Downloads
     */

    private static ArrayList<String>      delayMap     = new ArrayList<String>();
    private static JDController           INSTANCE     = new JDController();

    private static final Object           SHUTDOWNLOCK = new Object();

    /**
     * Private constructor. Use singleton method instead!
     */
    private JDController() {
        eventSender = new EventSender();
    }

    /**
     * Fügt einen Listener hinzu
     * 
     * @param listener
     *            Ein neuer Listener
     */
    public void addControlListener(final ControlListener listener) {
        eventSender.addListener(listener);
    }

    private String callService(final String service, final String key) throws Exception {
        LOGGER.finer("Call " + service);
        final Browser br = new Browser();
        br.postPage(service, "jd=1&srcType=plain&data=" + key);
        LOGGER.info("Call re: " + br.toString());
        if (!br.getHttpConnection().isOK() || !br.containsHTML("<rc>")) {
            return null;
        } else {
            final String dlcKey = br.getRegex("<rc>(.*?)</rc>").getMatch(0);
            if (dlcKey.trim().length() < 80) return null;
            return dlcKey;
        }
    }

    public String encryptDLC(PluginsC plg, String xml) {
        if (xml == null || plg == null) return null;
        final String[] encrypt = plg.encrypt(xml);
        if (encrypt == null) {
            LOGGER.severe("Container Encryption failed.");
            return null;
        }
        final String key = encrypt[1];
        xml = encrypt[0];
        final String service = "http://service.jdownloader.org/dlcrypt/service.php";
        try {
            final String dlcKey = callService(service, key);
            if (dlcKey == null) return null;
            return xml + dlcKey;
        } catch (final Exception e) {
            JDLogger.exception(e);
        }
        return null;
    }

    /**
     * Beendet das Programm
     */
    public void exit() {
        new Thread(new Runnable() {
            public void run() {
                RestartController.getInstance().exit();
            }
        }).start();
    }

    /**
     * quickmode to choose between normal shutdown or quick one
     * 
     * quickmode: no events are thrown
     * 
     * (eg shutdown by os)
     * 
     * we maybe dont have enough time to wait for all addons/plugins to finish,
     * saving the database is the most important thing to do
     * 
     * @param quickmode
     */
    public void prepareShutdown(final boolean quickmode) {
        synchronized (SHUTDOWNLOCK) {
            if (DatabaseConnector.isDatabaseShutdown()) return;
            LOGGER.info("Stop all running downloads");
            DownloadWatchDog.getInstance().stopDownloads();
            if (!quickmode) {
                LOGGER.info("Call Exit event");
                fireControlEventDirect(new ControlEvent(this, ControlEvent.CONTROL_SYSTEM_EXIT, this));
            }
            LOGGER.info("Save Downloadlist");
            JDUtilities.getDownloadController().saveDownloadLinks();
            LOGGER.info("Save Passwordlist");
            PasswordListController.getInstance().saveSync();
            LOGGER.info("Save HTACCESSlist");

            if (!quickmode) {
                LOGGER.info("Wait for delayExit");
                waitDelayExit();
            }
            LOGGER.info("Shutdown Database");
            LOGGER.info("Release Single Instance Lock");
            try {
                /*
                 * try catch errors in case when lock has not been aquired (eg
                 * firewall prevent junique server creation)
                 */
                if (Main.SINGLE_INSTANCE_CONTROLLER != null) Main.SINGLE_INSTANCE_CONTROLLER.exit();
            } catch (final Exception e) {
            }
            fireControlEventDirect(new ControlEvent(this, ControlEvent.CONTROL_SYSTEM_SHUTDOWN_PREPARED, this));
        }
    }

    /** syncs all data to database */
    public void syncDatabase() {
        if (DatabaseConnector.isDatabaseShutdown()) return;
        LOGGER.info("Sync Downloadlist");
        JDUtilities.getDownloadController().saveDownloadLinks();
        LOGGER.info("Sync Passwordlist");
        PasswordListController.getInstance().saveSync();

    }

    /**
     * hiermit kann ein Thread den Exit von JD verzögern (zb. speichern von db
     * sachen) gibt eine ID zurück, mit welcher wieder der request freigegeben
     * werden kann
     */
    public static String requestDelayExit(String name) {
        if (name == null) name = "unknown";
        synchronized (delayMap) {
            String id = "ID: " + name + " TIME: " + System.currentTimeMillis();
            while (delayMap.contains(id)) {
                try {
                    Thread.sleep(50);
                } catch (final InterruptedException e) {
                }
                id = "ID: " + name + " TIME: " + System.currentTimeMillis();
            }
            delayMap.add(id);
            return id;
        }
    }

    /**
     * hiermit signalisiert ein Thread das es nun okay ist zu beenden benötigt
     * eine gültige ID
     */
    public static void releaseDelayExit(final String id) {
        synchronized (delayMap) {
            if (!delayMap.remove(id)) {
                JDLogger.getLogger().severe(id + " not found in delayMap!");
            }
        }
    }

    /**
     * verzögert den exit, sofern delayExit requests vorliegen, max 10 seks
     */
    private void waitDelayExit() {
        long maxdelay = 10000;
        while (maxdelay > 0) {
            if (delayMap.size() <= 0) return;
            try {
                Thread.sleep(200);
            } catch (final InterruptedException e) {
            }
            maxdelay -= 200;
        }
        LOGGER.severe("Unable to satisfy all delayExit requests! " + delayMap);
    }

    /**
     * Verteilt Ein Event an alle Listener
     * 
     * @param controlEvent
     *            ein abzuschickendes Event
     */
    public void fireControlEvent(final ControlEvent controlEvent) {
        if (controlEvent == null) return;
        try {
            synchronized (eventQueue) {
                eventQueue.add(controlEvent);
                synchronized (eventSender.getLOCK()) {
                    if (eventSender.waitFlag) {
                        eventSender.waitFlag = false;
                        eventSender.getLOCK().notify();
                    }
                }
            }
        } catch (final Exception e) {
        }
    }

    public void fireControlEventDirect(final ControlEvent controlEvent) {
        if (controlEvent == null) return;
        try {
            eventSender.handleEvent(controlEvent);
        } catch (final Exception e) {
        }
    }

    public void fireControlEvent(final int controlID, final Object param) {
        final ControlEvent c = new ControlEvent(this, controlID, param);
        fireControlEvent(c);
    }

    public ArrayList<DownloadLink> getContainerLinks(final File file) {
        LinkCrawler lc = new LinkCrawler();
        lc.setFilter(LinkFilterController.getInstance());
        lc.crawl("file://" + file.getAbsolutePath());
        lc.waitForCrawling();
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (CrawledLink link : lc.getCrawledLinks()) {
            if (link.getDownloadLink() == null) continue;
            ret.add(link.getDownloadLink());
        }
        return ret;
    }

    /**
     * Emtfernt einen Listener
     * 
     * @param listener
     *            Der zu entfernende Listener
     */
    public synchronized void removeControlListener(final ControlListener listener) {
        eventSender.removeListener(listener);
    }

    /**
     * Saves a list of given links in a DLC.
     * 
     * @param file
     *            Path the DLC file
     * @param links
     *            The links ehich should saved
     */
    public void saveDLC(File file, final ArrayList<DownloadLink> links) {
        if (!file.getAbsolutePath().endsWith("dlc")) {
            file = new File(file.getAbsolutePath() + ".dlc");
        }
        String xml = null;
        PluginsC plg = null;

        xml = new D().createContainerString(links);

        if (xml != null) {
            final String cipher = encryptDLC(plg, xml);
            if (cipher != null) {
                JDIO.writeLocalFile(file, cipher);
                return;
            }
        }
        LOGGER.severe("Container creation failed");
        UserIO.getInstance().requestMessageDialog("Container encryption failed");
    }

}