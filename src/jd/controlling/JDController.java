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
import jd.gui.swing.jdgui.views.settings.panels.JSonWrapper;
import jd.http.Browser;
import jd.nutils.JDFlags;
import jd.nutils.io.JDIO;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;
import org.appwork.utils.event.Eventsender;
import org.jdownloader.update.RestartController;

/**
 * Im JDController wird das ganze App gesteuert. Events werden deligiert.
 * 
 * @author JD-Team/astaldo
 */
public class JDController implements ControlListener {

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
            try {
                /*
                 * the last one to call is the JDController itself
                 */
                controlEvent(event);
            } catch (final Exception e) {
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

    private final ArrayList<ControlEvent> eventQueue       = new ArrayList<ControlEvent>();

    private EventSender                   eventSender      = null;

    /**
     * Der Logger
     */
    private static final Logger           LOGGER           = JDLogger.getLogger();

    private boolean                       alreadyAutostart = false;

    /**
     * Der Download Watchdog verwaltet die Downloads
     */

    private static ArrayList<String>      delayMap         = new ArrayList<String>();
    private static JDController           INSTANCE         = new JDController();

    private static final Object           SHUTDOWNLOCK     = new Object();

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

    /**
     * Hier werden ControlEvent ausgewertet
     * 
     * @param event
     */
    public void controlEvent(final ControlEvent event) {
        if (event == null) {
            LOGGER.warning("event= NULL");
            return;
        }
        switch (event.getEventID()) {
        case ControlEvent.CONTROL_INIT_COMPLETE:
            DownloadWatchDog.getInstance();
            break;
        case ControlEvent.CONTROL_ON_FILEOUTPUT:
            final File[] list = (File[]) event.getParameter();
            for (final File file : list) {
                if (isContainerFile(file)) {
                    if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_RELOADCONTAINER, true)) {
                        loadContainerFile(file);
                    }
                }
            }
            break;
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            // Nur Hostpluginevents auswerten
            if (!(event.getCaller() instanceof PluginForHost)) return;
            final DownloadLink lastDownloadFinished = ((SingleDownloadController) event.getParameter()).getDownloadLink();

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
        final String[] encrypt = JDUtilities.encrypt(xml, "dlc");
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
            JDUtilities.getDownloadController().saveDownloadLinksSyncnonThread();
            LOGGER.info("Save Accountlist");
            AccountController.getInstance().saveSyncnonThread();
            LOGGER.info("Sync FavIconController");
            FavIconController.getInstance().saveSyncnonThread();
            LOGGER.info("Save Passwordlist");
            PasswordListController.getInstance().saveSync();
            LOGGER.info("Save HTACCESSlist");
            HTACCESSController.getInstance().saveSync();
            if (!quickmode) {
                LOGGER.info("Wait for delayExit");
                waitDelayExit();
            }
            LOGGER.info("Shutdown Database");
            JDUtilities.getDatabaseConnector().shutdownDatabase();
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
        JDUtilities.getDownloadController().saveDownloadLinksSyncnonThread();
        LOGGER.info("Sync Accountlist");
        AccountController.getInstance().saveSyncnonThread();
        LOGGER.info("Sync FavIconController");
        FavIconController.getInstance().saveSyncnonThread();
        LOGGER.info("Sync Passwordlist");
        PasswordListController.getInstance().saveSync();
        LOGGER.info("Sync HTACCESSlist");
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

    public int getForbiddenReconnectDownloadNum() {
        final boolean allowinterrupt = JSonWrapper.get("DOWNLOAD").getBooleanProperty("PARAM_DOWNLOAD_AUTORESUME_ON_RECONNECT", true);

        int ret = 0;
        final ArrayList<DownloadLink> links = DownloadWatchDog.getInstance().getRunningDownloads();
        for (final DownloadLink link : links) {
            if (link.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                if (!(link.getTransferStatus().supportsResume() && allowinterrupt)) ret++;
            }
        }
        return ret;
    }

    /**
     * Der Zurückgegeben ArrayList darf nur gelesen werden!!
     * 
     * @return
     */
    public ArrayList<FilePackage> getPackages() {
        return JDUtilities.getDownloadController().getPackages();
    }

    public static boolean isContainerFile(final File file) {
        final ArrayList<CPluginWrapper> pluginsForContainer = CPluginWrapper.getCWrapper();
        for (final CPluginWrapper pContainer : pluginsForContainer) {
            if (pContainer.canHandle(file.getName())) return true;
        }
        return false;
    }

    public ArrayList<DownloadLink> getContainerLinks(final File file) {
        final ArrayList<CPluginWrapper> pluginsForContainer = CPluginWrapper.getCWrapper();
        ArrayList<DownloadLink> downloadLinks = new ArrayList<DownloadLink>();
        PluginsC pContainer;
        final ProgressController progress = new ProgressController("Containerloader", pluginsForContainer.size(), null);
        LOGGER.info("load Container: " + file);
        for (final CPluginWrapper wrapper : pluginsForContainer) {
            progress.setStatusText("Containerplugin: " + wrapper.getHost());
            if (wrapper.canHandle(file.getName())) {
                // es muss jeweils eine neue plugininstanz erzeugt
                // werden
                pContainer = (PluginsC) wrapper.getNewPluginInstance();
                try {
                    progress.setSource(pContainer);
                    pContainer.initContainer(file.getAbsolutePath());
                    final ArrayList<DownloadLink> links = pContainer.getContainedDownloadlinks();
                    if (links == null || links.size() == 0) {
                        LOGGER.severe("Container Decryption failed (1)");
                    } else {
                        downloadLinks = links;
                        break;
                    }
                } catch (final Throwable e) {
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
    public synchronized void removeControlListener(final ControlListener listener) {
        eventSender.removeListener(listener);
    }

    public static void loadContainerFile(final File file) {
        loadContainerFile(file, false, false);
    }

    /**
     * Hiermit wird eine Containerdatei geöffnet. Dazu wird zuerst ein passendes
     * Plugin gesucht und danach alle DownloadLinks interpretiert
     * 
     * @param file
     *            Die Containerdatei
     */
    public static void loadContainerFile(final File file, final boolean hideGrabber, final boolean autostart) {
        System.out.println("load container");
        new Thread() {
            @Override
            public void run() {
                final ArrayList<CPluginWrapper> pluginsForContainer = CPluginWrapper.getCWrapper();
                ArrayList<DownloadLink> downloadLinks = new ArrayList<DownloadLink>();
                final ProgressController progress = new ProgressController("Containerloader", pluginsForContainer.size(), null);
                LOGGER.info("load Container: " + file);
                for (final CPluginWrapper wrapper : pluginsForContainer) {
                    progress.setStatusText("Containerplugin: " + wrapper.getHost());
                    if (wrapper.canHandle(file.getName())) {
                        // es muss jeweils eine neue plugininstanz erzeugt
                        // werden
                        final PluginsC pContainer = (PluginsC) wrapper.getNewPluginInstance();
                        try {
                            progress.setSource(pContainer);
                            pContainer.initContainer(file.getAbsolutePath());
                            final ArrayList<DownloadLink> links = pContainer.getContainedDownloadlinks();
                            if (links == null || links.size() == 0) {
                                LOGGER.severe("Container Decryption failed (1)");
                            } else {
                                downloadLinks = links;
                                break;
                            }
                        } catch (final Throwable e) {
                            JDLogger.exception(e);
                        }
                    }
                    progress.increase(1);
                }
                progress.setStatusText(downloadLinks.size() + " links found");
                if (downloadLinks.size() > 0) {
                    if (SubConfiguration.getConfig("GUI").getBooleanProperty(Configuration.PARAM_SHOW_CONTAINER_ONLOAD_OVERVIEW, false)) {
                        final String html = "<style>p { font-size:9px;margin:1px; padding:0px;}div {font-family:Geneva, Arial, Helvetica, sans-serif; width:400px;background-color:#ffffff; padding:2px;}h1 { vertical-align:top; text-align:left;font-size:10px; margin:0px; display:block;font-weight:bold; padding:0px;}</style><div> <div align='center'> <p><img src='http://jdownloader.org/img/%s.gif'> </p> </div> <h1>%s</h1><hr> <table width='100%%' border='0' cellspacing='5'> <tr> <td><p>%s</p></td> <td style='width:100%%'><p>%s</p></td> </tr> <tr> <td><p>%s</p></td> <td style='width:100%%'><p>%s</p></td> </tr> <tr> <td><p>%s</p></td> <td style='width:100%%'><p>%s</p></td> </tr> <tr> <td><p>%s</p></td> <td style='width:100%%'><p>%s</p></td> </tr> </table> </div>";
                        String app;
                        String uploader;
                        FilePackage filePackage = downloadLinks.get(0).getFilePackage();
                        if (filePackage.getProperty("header", null) != null) {
                            final HashMap<String, String> header = filePackage.getGenericProperty("header", new HashMap<String, String>());
                            uploader = header.get("tribute");
                            app = header.get("generator.app") + " v." + header.get("generator.version") + " (" + header.get("generator.url") + ")";
                        } else {
                            app = "n.A.";
                            uploader = "n.A";
                        }
                        final String comment = filePackage.getComment();
                        final String password = filePackage.getPassword();
                        JDFlags.hasAllFlags(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN | UserIO.STYLE_HTML, JDL.L("container.message.title", "DownloadLinkContainer loaded"), String.format(html, JDIO.getFileExtension(file).toLowerCase(), JDL.L("container.message.title", "DownloadLinkContainer loaded"), JDL.L("container.message.uploaded", "Brought to you by"), uploader, JDL.L("container.message.created", "Created with"), app, JDL.L("container.message.comment", "Comment"), comment, JDL.L("container.message.password", "Password"), password)), UserIO.RETURN_OK);

                    }
                    // schickt die Links zuerst mal zum Linkgrabber
                    LinkGrabberController.getInstance().addLinks(downloadLinks, hideGrabber, autostart);
                }
                progress.doFinalize();
            }
        }.start();
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

        final String xml = JDUtilities.createContainerString(links, "dlc");
        final String cipher = encryptDLC(xml);
        if (cipher != null) {
            final SubConfiguration cfg = SubConfiguration.getConfig("DLCrypt");
            JDIO.writeLocalFile(file, cipher);
            if (cfg.getBooleanProperty("SHOW_INFO_AFTER_CREATE", false)) {
                // Nur Falls Die Meldung nicht deaktiviert wurde {
                if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, JDL.L("sys.dlc.success", "DLC encryption successfull. Run Testdecrypt now?")), UserIO.RETURN_OK)) {
                    loadContainerFile(file);
                    return;
                }
            }
            return;
        }
        LOGGER.severe("Container creation failed");
        UserIO.getInstance().requestMessageDialog("Container encryption failed");
    }

    public synchronized void autostartDownloadsonStartup() {
        if (alreadyAutostart == true) return;
        alreadyAutostart = true;
        new Thread("Autostart counter") {
            @Override
            public void run() {
                final ProgressController pc = new ProgressController(JDL.L("gui.autostart", "Autostart downloads in few seconds..."), null);
                pc.getBroadcaster().addListener(new ProgressControllerListener() {
                    public void onProgressControllerEvent(final ProgressControllerEvent event) {
                        pc.setStatusText("Autostart aborted!");
                    }
                });
                pc.doFinalize(10 * 1000l);
                while (!pc.isFinished()) {
                    try {
                        Thread.sleep(1000);
                    } catch (final InterruptedException e) {
                        break;
                    }
                }
                if (!pc.isAbort()) DownloadWatchDog.getInstance().startDownloads();
            }
        }.start();
    }

    public DownloadLink getDownloadLinkByFileOutput(final File file, final Integer linkstatus) {
        final ArrayList<DownloadLink> links = JDUtilities.getDownloadController().getAllDownloadLinks();
        try {
            for (final DownloadLink nextDownloadLink : links) {
                if (new File(nextDownloadLink.getFileOutput()).getAbsoluteFile().equals(file.getAbsoluteFile())) {
                    if (linkstatus != null) {
                        if (nextDownloadLink.getLinkStatus().hasStatus(linkstatus)) return nextDownloadLink;
                    } else {
                        return nextDownloadLink;
                    }
                }
            }
        } catch (final Exception e) {
            JDLogger.exception(e);
        }
        return null;
    }

    public ArrayList<DownloadLink> getDownloadLinksByNamePattern(final String matcher) {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final ArrayList<FilePackage> packages = JDUtilities.getDownloadController().getPackages();
        try {
            for (final FilePackage fp : packages) {
                for (final DownloadLink nextDownloadLink : fp.getDownloadLinkList()) {
                    final String name = new File(nextDownloadLink.getFileOutput()).getName();
                    if (new Regex(name, matcher, Pattern.CASE_INSENSITIVE).matches()) {
                        ret.add(nextDownloadLink);
                    }
                }
            }
            return ret;
        } catch (final Exception e) {
            JDLogger.exception(e);
        }
        return null;
    }

    public ArrayList<DownloadLink> getDownloadLinksByPathPattern(final String matcher) {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final ArrayList<FilePackage> packages = JDUtilities.getDownloadController().getPackages();
        try {
            for (final FilePackage fp : packages) {
                for (final DownloadLink nextDownloadLink : fp.getDownloadLinkList()) {
                    final String path = nextDownloadLink.getFileOutput();
                    if (new Regex(path, matcher, Pattern.CASE_INSENSITIVE).matches()) {
                        ret.add(nextDownloadLink);
                    }
                }
            }
            return ret;
        } catch (final Exception e) {
            JDLogger.exception(e);
        }
        return null;
    }

    public static void distributeLinks(final String data) {
        new DistributeData(data).start();
    }

}
