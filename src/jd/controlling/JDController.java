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
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jd.CPluginWrapper;
import jd.Main;
import jd.config.Configuration;
import jd.config.DatabaseConnector;
import jd.config.SubConfiguration;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.nutils.JDFlags;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

/**
 * Im Controller wird das ganze App gesteuert. Evebnts werden deligiert.
 * 
 * @author JD-Team/astaldo
 * 
 */

public class JDController implements ControlListener {

    public static JDController getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JDController();
        }
        return INSTANCE;
    }

    private class EventSender extends Thread {

        protected static final long MAX_EVENT_TIME = 10000;
        private ControlListener currentListener;
        private ControlEvent event;
        private long eventStart = 0;
        public boolean waitFlag = true;
        private Thread watchDog;

        public EventSender() {
            super("EventSender");
            watchDog = new Thread("EventSenderWatchDog") {
                @Override
                public void run() {
                    while (true) {
                        if (eventStart > 0 && System.currentTimeMillis() - eventStart > MAX_EVENT_TIME) {
                            logger.finer("WATCHDOG: Execution Limit reached");
                            logger.finer("ControlListener: " + currentListener);
                            logger.finer("Event: " + event);
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            JDLogger.exception(e);
                            return;
                        }
                    }
                }

            };
            watchDog.start();
        }

        @Override
        public void run() {
            while (true) {
                synchronized (this) {
                    while (waitFlag) {
                        try {
                            wait();
                        } catch (Exception e) {
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
                            waitFlag = true;
                            // JDUtilities.getLogger().severe("PAUSE");
                        }
                    }
                    if (event == null || waitFlag) continue;
                    eventStart = System.currentTimeMillis();
                    currentListener = JDController.this;
                    try {
                        controlEvent(event);
                    } catch (Exception e) {
                        JDLogger.exception(e);
                    }
                    eventStart = 0;
                    synchronized (controlListener) {
                        if (controlListener.size() > 0) {
                            for (ControlListener cl : controlListener) {
                                eventStart = System.currentTimeMillis();
                                try {
                                    cl.controlEvent(event);
                                } catch (Exception e) {
                                    JDLogger.exception(e);
                                }
                                eventStart = 0;
                            }
                        }
                        synchronized (removeList) {
                            controlListener.removeAll(removeList);
                            removeList.clear();
                        }
                    }
                    // JDUtilities.getLogger().severe("THREAD2");

                } catch (Exception e) {
                    JDLogger.exception(e);
                    eventStart = 0;
                }
            }

        }

    }

    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #fireControlEvent(ControlEvent)} ein Event losgeschickt wird.
     */
    private transient ArrayList<ControlListener> controlListener = new ArrayList<ControlListener>();
    private transient ArrayList<ControlListener> removeList = new ArrayList<ControlListener>();

    private ArrayList<ControlEvent> eventQueue = new ArrayList<ControlEvent>();

    private EventSender eventSender = null;

    private DownloadLink lastDownloadFinished;

    /**
     * Der Logger
     */
    private Logger logger = JDLogger.getLogger();

    private boolean alreadyAutostart = false;

    /**
     * Der Download Watchdog verwaltet die Downloads
     */

    private static ArrayList<String> delayMap = new ArrayList<String>();
    private static JDController INSTANCE;

    private static final Object SHUTDOWNLOCK = new Object();

    public JDController() {
        eventSender = getEventSender();
        JDUtilities.setController(this);
    }

    /**
     * Fügt einen Listener hinzu
     * 
     * @param listener
     *            Ein neuer Listener
     */
    public void addControlListener(ControlListener listener) {
        if (listener == null) throw new NullPointerException();
        synchronized (controlListener) {
            synchronized (removeList) {
                if (removeList.contains(listener)) removeList.remove(listener);
            }
            if (!controlListener.contains(listener)) controlListener.add(listener);
        }
    }

    private String callService(String service, String key) throws Exception {
        logger.finer("Call " + service);
        Browser br = new Browser();
        br.postPage(service, "jd=1&srcType=plain&data=" + key);
        logger.info("Call re: " + br.toString());
        if (!br.getHttpConnection().isOK() || !br.containsHTML("<rc>")) {
            return null;
        } else {
            String dlcKey = br.getRegex("<rc>(.*?)</rc>").getMatch(0);
            if (dlcKey.trim().length() < 80) return null;
            return dlcKey;
        }
    }

    /**
     * Hier werden ControlEvent ausgewertet
     * 
     * @param event
     */
    public void controlEvent(ControlEvent event) {
        if (event == null) {
            logger.warning("event= NULL");
            return;
        }
        switch (event.getID()) {
        case ControlEvent.CONTROL_INIT_COMPLETE:
            DownloadWatchDog.getInstance();
            break;
        case ControlEvent.CONTROL_ON_FILEOUTPUT:
            File[] list = (File[]) event.getParameter();
            for (File file : list) {
                if (isContainerFile(file)) {
                    if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_RELOADCONTAINER, true)) {
                        loadContainerFile(file);
                    }
                }
            }
            break;
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            // Nur Hostpluginevents auswerten
            if (!(event.getSource() instanceof PluginForHost)) { return; }
            lastDownloadFinished = ((SingleDownloadController) event.getParameter()).getDownloadLink();

            // Prüfen ob das Paket fertig ist und entfernt werden soll
            if (lastDownloadFinished.getFilePackage().getRemainingLinks() == 0) {
                if (JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION, 3) == 2) {
                    JDUtilities.getDownloadController().removePackage(lastDownloadFinished.getFilePackage());
                    break;
                }
            }

            // Prüfen ob der Link entfernt werden soll
            if (lastDownloadFinished.getLinkStatus().isFinished() && JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION, 3) == 0) {
                lastDownloadFinished.getFilePackage().remove(lastDownloadFinished);
            }

            break;
        }
    }

    public String encryptDLC(String xml) {
        String[] encrypt = JDUtilities.encrypt(xml, "dlc");
        if (encrypt == null) {
            logger.severe("Container Encryption failed.");
            return null;
        }
        String key = encrypt[1];
        xml = encrypt[0];
        String service = "http://service.jdownloader.org/dlcrypt/service.php";
        try {
            String dlcKey = callService(service, key);
            if (dlcKey == null) return null;
            return xml + dlcKey;
        } catch (Exception e) {
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
                prepareShutdown(false);
                System.exit(0);
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
    public void prepareShutdown(boolean quickmode) {
        synchronized (SHUTDOWNLOCK) {
            if (DatabaseConnector.isDatabaseShutdown()) return;
            logger.info("Stop all running downloads");
            DownloadWatchDog.getInstance().stopDownloads();
            if (!quickmode) {
                logger.info("Call Exit event");
                fireControlEventDirect(new ControlEvent(this, ControlEvent.CONTROL_SYSTEM_EXIT, this));
            }
            logger.info("Save Downloadlist");
            JDUtilities.getDownloadController().saveDownloadLinksSyncnonThread();
            logger.info("Save Accountlist");
            AccountController.getInstance().saveSyncnonThread();
            logger.info("Save Passwordlist");
            PasswordListController.getInstance().saveSync();
            logger.info("Save HTACCESSlist");
            HTACCESSController.getInstance().saveSync();
            if (!quickmode) {
                logger.info("Wait for delayExit");
                waitDelayExit();
            }
            logger.info("Shutdown Database");
            JDUtilities.getDatabaseConnector().shutdownDatabase();
            logger.info("Release JUnique Lock");
            try {
                /*
                 * try catch errors in case when lock has not been aquired (eg
                 * firewall prevent junique server creation)
                 */
                if (Main.SINGLE_INSTANCE_CONTROLLER != null) Main.SINGLE_INSTANCE_CONTROLLER.exit();
            } catch (Exception e) {
            }
            fireControlEventDirect(new ControlEvent(this, ControlEvent.CONTROL_SYSTEM_SHUTDOWN_PREPARED, this));
        }
    }

    /** syncs all data to database */
    public void syncDatabase() {
        if (DatabaseConnector.isDatabaseShutdown()) return;
        logger.info("Sync Downloadlist");
        JDUtilities.getDownloadController().saveDownloadLinksSyncnonThread();
        logger.info("Sync Accountlist");
        AccountController.getInstance().saveSyncnonThread();
        logger.info("Sync Passwordlist");
        PasswordListController.getInstance().saveSync();
        logger.info("Sync HTACCESSlist");
        HTACCESSController.getInstance().saveSync();
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
                } catch (InterruptedException e) {
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
    public static void releaseDelayExit(String id) {
        synchronized (delayMap) {
            if (!delayMap.remove(id)) {
                JDLogger.getLogger().severe(id + " not found in delayMap!");
            }
        }
    }

    /*
     * verzögert den exit, sofern delayExit requests vorliegen, max 10 seks
     */
    private void waitDelayExit() {
        long maxdelay = 10000;
        while (maxdelay > 0) {
            if (delayMap.size() <= 0) return;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            maxdelay -= 200;
        }
        logger.severe("Unable to satisfy all delayExit requests! " + delayMap);
    }

    /**
     * Verteilt Ein Event an alle Listener
     * 
     * @param controlEvent
     *            ein abzuschickendes Event
     */
    public void fireControlEvent(ControlEvent controlEvent) {
        if (controlEvent == null) return;
        try {
            synchronized (eventQueue) {
                eventQueue.add(controlEvent);
                synchronized (eventSender) {
                    if (eventSender.waitFlag) {
                        eventSender.waitFlag = false;
                        eventSender.notify();
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public void fireControlEventDirect(ControlEvent controlEvent) {
        if (controlEvent == null) return;
        try {
            synchronized (controlListener) {
                synchronized (removeList) {
                    controlListener.removeAll(removeList);
                    removeList.clear();
                }
                if (controlListener.size() > 0) {
                    for (ControlListener cl : controlListener) {
                        try {
                            cl.controlEvent(controlEvent);
                        } catch (Exception e) {
                            JDLogger.exception(e);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public void fireControlEvent(int controlID, Object param) {
        ControlEvent c = new ControlEvent(this, controlID, param);
        fireControlEvent(c);
    }

    private EventSender getEventSender() {
        if (this.eventSender != null && this.eventSender.isAlive()) return this.eventSender;
        EventSender th = new EventSender();
        th.start();
        return th;
    }

    public int getForbiddenReconnectDownloadNum() {
        boolean allowinterrupt = SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty("PARAM_DOWNLOAD_AUTORESUME_ON_RECONNECT", true);

        int ret = 0;
        ArrayList<DownloadLink> links = DownloadWatchDog.getInstance().getRunningDownloads();
        for (DownloadLink link : links) {
            if (link.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                if (!(link.getTransferStatus().supportsResume() && allowinterrupt)) ret++;
            }
        }
        return ret;
    }

    /**
     * 
     * @return Die zuletzte fertiggestellte datei
     */
    public DownloadLink getLastFinishedDownloadLink() {
        return lastDownloadFinished;
    }

    /**
     * Der Zurückgegeben ArrayList darf nur gelesen werden!!
     * 
     * @return
     */
    public ArrayList<FilePackage> getPackages() {
        return JDUtilities.getDownloadController().getPackages();
    }

    public boolean isContainerFile(File file) {
        ArrayList<CPluginWrapper> pluginsForContainer = CPluginWrapper.getCWrapper();
        CPluginWrapper pContainer;
        for (int i = 0; i < pluginsForContainer.size(); i++) {
            pContainer = pluginsForContainer.get(i);
            if (pContainer.canHandle(file.getName())) return true;
        }
        return false;
    }

    public ArrayList<DownloadLink> getContainerLinks(final File file) {
        ArrayList<CPluginWrapper> pluginsForContainer = CPluginWrapper.getCWrapper();
        ArrayList<DownloadLink> downloadLinks = new ArrayList<DownloadLink>();
        PluginsC pContainer;
        CPluginWrapper wrapper;
        ProgressController progress = new ProgressController("Containerloader", pluginsForContainer.size(), null);
        logger.info("load Container: " + file);
        for (int i = 0; i < pluginsForContainer.size(); i++) {
            wrapper = pluginsForContainer.get(i);
            progress.setStatusText("Containerplugin: " + wrapper.getHost());
            if (wrapper.canHandle(file.getName())) {
                // es muss jeweils eine neue plugininstanz erzeugt
                // werden
                pContainer = (PluginsC) wrapper.getNewPluginInstance();
                try {
                    progress.setSource(pContainer);
                    pContainer.initContainer(file.getAbsolutePath());
                    ArrayList<DownloadLink> links = pContainer.getContainedDownloadlinks();
                    if (links == null || links.size() == 0) {
                        logger.severe("Container Decryption failed (1)");
                    } else {
                        downloadLinks = links;
                        break;
                    }
                } catch (Throwable e) {
                    JDLogger.exception(e);
                }
            }
            progress.increase(1);
        }
        progress.setStatusText(downloadLinks.size() + " links found");
        progress.doFinalize();
        return downloadLinks;
    }

    /**
     * Emtfernt einen Listener
     * 
     * @param listener
     *            Der zu entfernende Listener
     */
    public synchronized void removeControlListener(ControlListener listener) {
        synchronized (removeList) {
            if (!removeList.contains(listener)) removeList.add(listener);
        }
    }

    public void loadContainerFile(final File file) {
        loadContainerFile(file, false, false);
    }

    /**
     * Hiermit wird eine Containerdatei geöffnet. Dazu wird zuerst ein passendes
     * Plugin gesucht und danach alle DownloadLinks interpretiert
     * 
     * @param file
     *            Die Containerdatei
     */
    public void loadContainerFile(final File file, final boolean hideGrabber, final boolean autostart) {
        System.out.println("load container");
        new Thread() {
            @Override
            public void run() {
                ArrayList<CPluginWrapper> pluginsForContainer = CPluginWrapper.getCWrapper();
                ArrayList<DownloadLink> downloadLinks = new ArrayList<DownloadLink>();
                CPluginWrapper wrapper;
                ProgressController progress = new ProgressController("Containerloader", pluginsForContainer.size(), null);
                logger.info("load Container: " + file);
                for (int i = 0; i < pluginsForContainer.size(); i++) {
                    wrapper = pluginsForContainer.get(i);
                    progress.setStatusText("Containerplugin: " + wrapper.getHost());
                    if (wrapper.canHandle(file.getName())) {
                        // es muss jeweils eine neue plugininstanz erzeugt
                        // werden
                        PluginsC pContainer = (PluginsC) wrapper.getNewPluginInstance();
                        try {
                            progress.setSource(pContainer);
                            pContainer.initContainer(file.getAbsolutePath());
                            ArrayList<DownloadLink> links = pContainer.getContainedDownloadlinks();
                            if (links == null || links.size() == 0) {
                                logger.severe("Container Decryption failed (1)");
                            } else {
                                downloadLinks = links;
                                break;
                            }
                        } catch (Exception e) {
                            JDLogger.exception(e);
                        }
                    }
                    progress.increase(1);
                }
                progress.setStatusText(downloadLinks.size() + " links found");
                if (downloadLinks.size() > 0) {
                    if (SubConfiguration.getConfig("GUI").getBooleanProperty(Configuration.PARAM_SHOW_CONTAINER_ONLOAD_OVERVIEW, false)) {
                        String html = "<style>p { font-size:9px;margin:1px; padding:0px;}div {font-family:Geneva, Arial, Helvetica, sans-serif; width:400px;background-color:#ffffff; padding:2px;}h1 { vertical-align:top; text-align:left;font-size:10px; margin:0px; display:block;font-weight:bold; padding:0px;}</style><div> <div align='center'> <p><img src='http://jdownloader.org/img/%s.gif'> </p> </div> <h1>%s</h1><hr> <table width='100%%' border='0' cellspacing='5'> <tr> <td><p>%s</p></td> <td style='width:100%%'><p>%s</p></td> </tr> <tr> <td><p>%s</p></td> <td style='width:100%%'><p>%s</p></td> </tr> <tr> <td><p>%s</p></td> <td style='width:100%%'><p>%s</p></td> </tr> <tr> <td><p>%s</p></td> <td style='width:100%%'><p>%s</p></td> </tr> </table> </div>";
                        String app;
                        String uploader;
                        if (downloadLinks.get(0).getFilePackage().getProperty("header", null) != null) {
                            HashMap<String, String> header = downloadLinks.get(0).getFilePackage().getGenericProperty("header", new HashMap<String, String>());
                            uploader = header.get("tribute");
                            app = header.get("generator.app") + " v." + header.get("generator.version") + " (" + header.get("generator.url") + ")";
                        } else {
                            app = "n.A.";
                            uploader = "n.A";
                        }
                        String comment = downloadLinks.get(0).getFilePackage().getComment();
                        String password = downloadLinks.get(0).getFilePackage().getPassword();
                        JDFlags.hasAllFlags(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN | UserIO.STYLE_HTML, JDL.L("container.message.title", "DownloadLinkContainer loaded"), String.format(html, JDIO.getFileExtension(file).toLowerCase(), JDL.L("container.message.title", "DownloadLinkContainer loaded"), JDL.L("container.message.uploaded", "Brought to you by"), uploader, JDL.L("container.message.created", "Created with"), app, JDL.L("container.message.comment", "Comment"), comment, JDL.L("container.message.password", "Password"), password)), UserIO.RETURN_OK);

                    }
                    // schickt die Links zuerst mal zum Linkgrabber
                    LinkGrabberController.getInstance().addLinks(downloadLinks, hideGrabber, autostart);
                }
                progress.doFinalize();
            }
        }.start();
    }

    public void saveDLC(File file, ArrayList<DownloadLink> links) {
        String xml = JDUtilities.createContainerString(links, "dlc");
        String cipher = encryptDLC(xml);
        if (cipher != null) {
            SubConfiguration cfg = SubConfiguration.getConfig("DLCrypt");
            JDIO.writeLocalFile(file, cipher);
            if (cfg.getBooleanProperty("SHOW_INFO_AFTER_CREATE", false))
            // Nur Falls Die Meldung nicht deaktiviert wurde {
                if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, JDL.L("sys.dlc.success", "DLC encryption successfull. Run Testdecrypt now?")), UserIO.RETURN_OK)) {
                    loadContainerFile(file);
                    return;
                }
            return;
        }
        logger.severe("Container creation failed");
        UserIO.getInstance().requestMessageDialog("Container encryption failed");
    }

    /**
     * Gibt alle Downloadlinks die zu dem übergebenem Hosterplugin gehören
     * zurück.
     * 
     * @param pluginForHost
     */
    public ArrayList<DownloadLink> getDownloadLinks(PluginForHost pluginForHost) {
        ArrayList<DownloadLink> al = new ArrayList<DownloadLink>();
        ArrayList<FilePackage> packages = JDUtilities.getDownloadController().getPackages();
        synchronized (packages) {
            DownloadLink nextDownloadLink;
            for (FilePackage fp : packages) {
                Iterator<DownloadLink> it2 = fp.getDownloadLinkList().iterator();
                while (it2.hasNext()) {
                    nextDownloadLink = it2.next();
                    if (nextDownloadLink.getPlugin().getClass() == pluginForHost.getClass()) al.add(nextDownloadLink);
                }
            }
        }
        return al;
    }

    public synchronized void autostartDownloadsonStartup() {
        if (alreadyAutostart == true) return;
        alreadyAutostart = true;
        new Thread() {
            @Override
            public void run() {
                this.setName("Autostart counter");
                final ProgressController pc = new ProgressController(JDL.L("gui.autostart", "Autostart downloads in few seconds..."), null);
                pc.getBroadcaster().addListener(new ProgressControllerListener() {
                    public void onProgressControllerEvent(ProgressControllerEvent event) {
                        pc.setStatusText("Autostart aborted!");
                    }
                });
                pc.doFinalize(10 * 1000l);
                while (!pc.isFinished()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                if (!pc.isAbort()) DownloadWatchDog.getInstance().startDownloads();
            }
        }.start();
    }

    public DownloadLink getDownloadLinkByFileOutput(File file, Integer Linkstatus) {
        ArrayList<DownloadLink> links = JDUtilities.getDownloadController().getAllDownloadLinks();
        try {
            for (DownloadLink nextDownloadLink : links) {
                if (new File(nextDownloadLink.getFileOutput()).getAbsoluteFile().equals(file.getAbsoluteFile())) {
                    if (Linkstatus != null) {
                        if (nextDownloadLink.getLinkStatus().hasStatus(Linkstatus)) return nextDownloadLink;
                    } else
                        return nextDownloadLink;
                }
            }
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        return null;
    }

    public ArrayList<DownloadLink> getDownloadLinksByNamePattern(String matcher) {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        ArrayList<FilePackage> packages = JDUtilities.getDownloadController().getPackages();
        try {
            Iterator<FilePackage> iterator = packages.iterator();
            FilePackage fp = null;
            DownloadLink nextDownloadLink;
            while (iterator.hasNext()) {
                fp = iterator.next();
                Iterator<DownloadLink> it2 = fp.getDownloadLinkList().iterator();
                while (it2.hasNext()) {
                    nextDownloadLink = it2.next();
                    String name = new File(nextDownloadLink.getFileOutput()).getName();
                    if (new Regex(name, matcher, Pattern.CASE_INSENSITIVE).matches()) {
                        ret.add(nextDownloadLink);
                    }

                }
            }
            return ret;
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        return null;
    }

    public ArrayList<DownloadLink> getDownloadLinksByPathPattern(String matcher) {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        ArrayList<FilePackage> packages = JDUtilities.getDownloadController().getPackages();
        try {
            Iterator<FilePackage> iterator = packages.iterator();
            FilePackage fp = null;
            DownloadLink nextDownloadLink;
            while (iterator.hasNext()) {
                fp = iterator.next();
                Iterator<DownloadLink> it2 = fp.getDownloadLinkList().iterator();
                while (it2.hasNext()) {
                    nextDownloadLink = it2.next();
                    String path = nextDownloadLink.getFileOutput();
                    if (new Regex(path, matcher, Pattern.CASE_INSENSITIVE).matches()) {
                        ret.add(nextDownloadLink);
                    }

                }
            }
            return ret;
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        return null;
    }

    public static void distributeLinks(String data) {
        new DistributeData(data).start();
    }

}
