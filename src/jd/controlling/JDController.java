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
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UIInterface;
import jd.http.Browser;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;
import jd.update.FileUpdate;
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
     * Es läuft kein Download
     */
    public static final int DOWNLOAD_NOT_RUNNING = 3;
    /**
     * Der Download läuft
     */
    public static final int DOWNLOAD_RUNNING = 2;
    /**
     * Der Download wird gerade abgebrochen.
     */
    public static final int DOWNLOAD_TERMINATION_IN_PROGRESS = 0;
    /**
     * Der Controller wurd fertig initialisiert
     */
    public static final int INIT_STATUS_COMPLETE = 0;

    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #fireControlEvent(ControlEvent)} ein Event losgeschickt wird.
     */
    private transient ArrayList<ControlListener> controlListener = new ArrayList<ControlListener>();
    private transient ArrayList<ControlListener> removeList = new ArrayList<ControlListener>();

    /**
     * Hier kann de Status des Downloads gespeichert werden.
     */
    private int downloadStatus;

    private ArrayList<ControlEvent> eventQueue = new ArrayList<ControlEvent>();;

    private EventSender eventSender = null;

    private ArrayList<DownloadLink> finishedLinks = new ArrayList<DownloadLink>();

    private int initStatus = -1;

    private DownloadLink lastDownloadFinished;

    /**
     * Der Logger
     */
    private Logger logger = JDLogger.getLogger();

    /**
     * Schnittstelle zur Benutzeroberfläche
     */
    private UIInterface uiInterface;

    private ArrayList<FileUpdate> waitingUpdates = new ArrayList<FileUpdate>();

    /**
     * Der Download Watchdog verwaltet die Downloads
     */
    private DownloadWatchDog watchdog;

    private Integer StartStopSync = new Integer(0);
    private static JDController INSTANCE;

    public JDController() {
        downloadStatus = DOWNLOAD_NOT_RUNNING;
        eventSender = getEventSender();
        INSTANCE = this;
        JDUtilities.setController(this);

    }

    /**
     * Fügt einen Listener hinzu
     * 
     * @param listener
     *            Ein neuer Listener
     */
    public synchronized void addControlListener(ControlListener listener) {
        synchronized (controlListener) {
            synchronized (removeList) {
                if (removeList.contains(listener)) removeList.remove(listener);
            }
            if (!controlListener.contains(listener)) controlListener.add(listener);
        }
    }

    /**
     * Fügt einen Downloadlink der Finishedliste hinzu.
     * 
     * @param lastDownloadFinished
     */
    private void addToFinished(DownloadLink lastDownloadFinished) {
        synchronized (finishedLinks) {
            finishedLinks.add(lastDownloadFinished);
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

    @SuppressWarnings("unchecked")
    public void controlEvent(ControlEvent event) {
        if (event == null) {
            logger.warning("event= NULL");
            return;
        }
        switch (event.getID()) {
        case ControlEvent.CONTROL_INIT_COMPLETE:
            watchdog = DownloadWatchDog.getInstance();
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

        case ControlEvent.CONTROL_LOG_OCCURED:

            break;
        case ControlEvent.CONTROL_SYSTEM_EXIT:

            break;
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            // Nur Hostpluginevents auswerten
            if (!(event.getSource() instanceof PluginForHost)) { return; }
            lastDownloadFinished = ((SingleDownloadController) event.getParameter()).getDownloadLink();
            addToFinished(lastDownloadFinished);

            // Prüfen ob das Paket fertig ist und entfernt werden soll
            if (lastDownloadFinished.getFilePackage().getRemainingLinks() == 0) {
                Interaction.handleInteraction(Interaction.INTERACTION_DOWNLOAD_PACKAGE_FINISHED, this);

                if (JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION) == 2) {
                    JDUtilities.getDownloadController().removePackage(lastDownloadFinished.getFilePackage());
                    break;
                }
            }

            // Prüfen ob der Link entfernt werden soll
            if (lastDownloadFinished.getLinkStatus().hasStatus(LinkStatus.FINISHED) && JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION) == 0) {
                lastDownloadFinished.getFilePackage().remove(lastDownloadFinished);

            }

            break;
        case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:
            if (uiInterface == null) return;
            if (event.getParameter() != null && event.getParameter() instanceof ArrayList && ((ArrayList) event.getParameter()).size() > 0) {
                ArrayList<DownloadLink> links = (ArrayList<DownloadLink>) event.getParameter();
                uiInterface.addLinksToGrabber(links, false);
            }
            break;
        case ControlEvent.CONTROL_DISTRIBUTE_FINISHED_HIDEGRABBER:
            if (event.getParameter() != null && event.getParameter() instanceof ArrayList && ((ArrayList) event.getParameter()).size() > 0) {
                ArrayList<DownloadLink> links = (ArrayList<DownloadLink>) event.getParameter();
                uiInterface.addLinksToGrabber(links, true);
            }
            break;
        case ControlEvent.CONTROL_DISTRIBUTE_FINISHED_HIDEGRABBER_START:
            if (event.getParameter() != null && event.getParameter() instanceof ArrayList && ((ArrayList) event.getParameter()).size() > 0) {
                ArrayList<DownloadLink> links = (ArrayList<DownloadLink>) event.getParameter();
                uiInterface.addLinksToGrabber(links, true);
                if (getDownloadStatus() == JDController.DOWNLOAD_NOT_RUNNING) {
                    toggleStartStop();
                }
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
        prepareShutdown();
        System.exit(0);
    }

    public void prepareShutdown() {
        logger.info("Stop all runing downloads");
        stopDownloads();
        logger.info("Save Downloadlist");
        JDUtilities.getDownloadController().saveDownloadLinksSyncnonThread();
        logger.info("Save Accountlist");
        AccountController.getInstance().saveSyncnonThread();
        logger.info("Save Passwordlist");
        PasswordListController.getInstance().saveSync();
        logger.info("Save HTACCESSlist");
        HTACCESSController.getInstance().saveSync();
        logger.info("Call Exit event");
        fireControlEventDirect(new ControlEvent(this, ControlEvent.CONTROL_SYSTEM_EXIT, this));
        logger.info("Call Exit interactions");
        Interaction.handleInteraction(Interaction.INTERACTION_EXIT, null);
        logger.info("Shutdown Database");
        JDUtilities.getDatabaseConnector().shutdownDatabase();
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
                        cl.controlEvent(controlEvent);
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

    /**
     * Gibt den Status (ID) der downloads zurück
     * 
     * @return
     */
    public int getDownloadStatus() {
        if (watchdog == null || watchdog.isAborted() && downloadStatus == DOWNLOAD_RUNNING) {
            setDownloadStatus(DOWNLOAD_NOT_RUNNING);
        }
        return downloadStatus;
    }

    private EventSender getEventSender() {
        if (this.eventSender != null && this.eventSender.isAlive()) return this.eventSender;
        EventSender th = new EventSender();
        th.start();
        return th;
    }

    /**
     * Gibt alle in dieser Session beendeten Downloadlinks zurück. unabhängig
     * davon ob sie noch in der dl liste stehen oder nicht
     * 
     * @return
     */
    public ArrayList<DownloadLink> getFinishedLinks() {
        synchronized (finishedLinks) {
            return finishedLinks;
        }
    }

    public int getForbiddenReconnectDownloadNum() {
        boolean allowinterrupt = SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty("PARAM_DOWNLOAD_AUTORESUME_ON_RECONNECT", true);
        int ret = 0;
        ArrayList<DownloadLink> links = DownloadWatchDog.getInstance().getRunningDownloads();
        for (DownloadLink link : links) {
            if (link.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                if (!(link.getPlugin().isResumable() && allowinterrupt)) ret++;
            }
        }
        return ret;
    }

    public int getInitStatus() {
        return initStatus;
    }

    /**
     * 
     * @return Die zuletzte fertiggestellte datei
     */
    public DownloadLink getLastFinishedDownloadLink() {
        return lastDownloadFinished;
    }

    /**
     * 
     * @return Die zuletzte fertiggestellte datei
     */
    public String getLastFinishedFile() {
        if (lastDownloadFinished == null) { return ""; }
        return lastDownloadFinished.getFileOutput();
    }

    /**
     * Der Zurückgegeben ArrayList darf nur gelesen werden!!
     * 
     * @return
     */
    public ArrayList<FilePackage> getPackages() {
        return JDUtilities.getDownloadController().getPackages();
    }

    /**
     * @return gibt das globale speedmeter zurück
     */
    public int getSpeedMeter() {
        if (getWatchdog() == null || !getWatchdog().isAlive()) { return 0; }
        return getWatchdog().getTotalSpeed();
    }

    /**
     * Gibt das verwendete UIinterface zurpck
     * 
     * @return aktuelles uiInterface
     */
    public UIInterface getUiInterface() {
        return uiInterface;
    }

    public ArrayList<FileUpdate> getWaitingUpdates() {
        return waitingUpdates;

    }

    public DownloadWatchDog getWatchdog() {
        return watchdog;
    }

    public boolean isContainerFile(File file) {
        ArrayList<CPluginWrapper> pluginsForContainer = CPluginWrapper.getCWrapper();
        CPluginWrapper pContainer;
        for (int i = 0; i < pluginsForContainer.size(); i++) {
            pContainer = pluginsForContainer.get(i);
            if (pContainer.canHandle(file.getName())) { return true; }
        }
        return false;
    }

    public ArrayList<DownloadLink> getContainerLinks(final File file) {
        ArrayList<CPluginWrapper> pluginsForContainer = CPluginWrapper.getCWrapper();
        ArrayList<DownloadLink> downloadLinks = new ArrayList<DownloadLink>();
        PluginsC pContainer;
        CPluginWrapper wrapper;
        ProgressController progress = new ProgressController("Containerloader", pluginsForContainer.size());
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
        progress.finalize();
        return downloadLinks;
    }

    public boolean isPaused() {
        if (watchdog == null) return false;
        return watchdog.isPaused();
    }

    public void pauseDownloads(boolean value) {
        if (watchdog == null) return;
        watchdog.pause(value);
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

    /**
     * Setzt de Status aller Links zurück die nicht gerade geladen werden.
     */
    public void resetAllLinks() {
        ArrayList<FilePackage> packages = JDUtilities.getDownloadController().getPackages();
        synchronized (packages) {
            ArrayList<DownloadLink> al = new ArrayList<DownloadLink>();
            Iterator<FilePackage> iterator = packages.iterator();
            FilePackage fp = null;
            DownloadLink nextDownloadLink;
            while (iterator.hasNext()) {
                fp = iterator.next();
                Iterator<DownloadLink> it2 = fp.getDownloadLinkList().iterator();
                while (it2.hasNext()) {
                    nextDownloadLink = it2.next();
                    if (!nextDownloadLink.getLinkStatus().isPluginActive()) {
                        nextDownloadLink.getLinkStatus().setStatus(LinkStatus.TODO);
                        nextDownloadLink.getLinkStatus().setStatusText("");
                        nextDownloadLink.getLinkStatus().reset();
                        // nextDownloadLink.setEndOfWaittime(0);
                        nextDownloadLink.getPlugin().resetPluginGlobals();
                        al.add(nextDownloadLink);
                    }

                }
            }
            DownloadController.getInstance().fireDownloadLinkUpdate(al);
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
    public void loadContainerFile(final File file, final boolean hideGrabber, final boolean startDownload) {
        System.out.println("load container");
        new Thread() {
            @Override
            public void run() {
                ArrayList<CPluginWrapper> pluginsForContainer = CPluginWrapper.getCWrapper();
                ArrayList<DownloadLink> downloadLinks = new ArrayList<DownloadLink>();
                CPluginWrapper wrapper;
                ProgressController progress = new ProgressController("Containerloader", pluginsForContainer.size());
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
                        JDUtilities.getGUI().showHTMLDialog(JDL.L("container.message.title", "DownloadLinkContainer loaded"), String.format(html, JDIO.getFileExtension(file).toLowerCase(), JDL.L("container.message.title", "DownloadLinkContainer loaded"), JDL.L("container.message.uploaded", "Brought to you by"), uploader, JDL.L("container.message.created", "Created with"), app, JDL.L("container.message.comment", "Comment"), comment, JDL.L("container.message.password", "Password"), password));

                    }
                    // schickt die Links zuerst mal zum Linkgrabber
                    uiInterface.addLinksToGrabber(downloadLinks, hideGrabber);
                    if (startDownload && getDownloadStatus() == JDController.DOWNLOAD_NOT_RUNNING) {
                        toggleStartStop();
                    }
                }
                progress.finalize();
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
            // Nur Falls Die Meldung nicht deaktiviert wurde
            {
                if (getUiInterface().showConfirmDialog(JDL.L("sys.dlc.success", "DLC encryption successfull. Run Testdecrypt now?"))) {
                    loadContainerFile(file);
                    return;
                }
            }
            return;
        }
        logger.severe("Container creation failed");
        getUiInterface().showMessageDialog("Container encryption failed");
    }

    /**
     * Setzt den Downloadstatus. Status Ids aus JDController.** sollten
     * verwendet werden
     * 
     * @param downloadStatus
     */
    public void setDownloadStatus(int downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public void setInitStatus(int initStatus) {
        this.initStatus = initStatus;
    }

    /**
     * Setzt das UIINterface
     * 
     * @param uiInterface
     */
    public void setUiInterface(UIInterface uiInterface) {
        this.uiInterface = uiInterface;
    }

    public void setWaitingUpdates(ArrayList<FileUpdate> files) {
        waitingUpdates = files;
    }

    /**
     * Startet den Downloadvorgang. Dies eFUnkton sendet das startdownload event
     * und aktiviert die ersten downloads.
     */
    public boolean startDownloads() {
        if (getDownloadStatus() == DOWNLOAD_TERMINATION_IN_PROGRESS) return false;
        synchronized (StartStopSync) {
            if (getDownloadStatus() == DOWNLOAD_NOT_RUNNING) {
                setDownloadStatus(DOWNLOAD_RUNNING);
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_START, this));
                logger.info("StartDownloads");
                watchdog = DownloadWatchDog.getInstance();
                watchdog.start();
                return true;
            }
            return false;
        }
    }

    /**
     * Bricht den Download ab und blockiert bis er abgebrochen wurde.
     */
    public boolean stopDownloads() {
        if (getDownloadStatus() == DOWNLOAD_TERMINATION_IN_PROGRESS) return false;
        synchronized (StartStopSync) {
            if (getDownloadStatus() == DOWNLOAD_RUNNING) {
                setDownloadStatus(DOWNLOAD_TERMINATION_IN_PROGRESS);
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_TERMINATION_ACTIVE, this));

                watchdog.abort();
                ArrayList<FilePackage> packages = JDUtilities.getDownloadController().getPackages();
                synchronized (packages) {
                    for (FilePackage fp : packages) {
                        for (DownloadLink link : fp.getDownloadLinkList()) {
                            if (link.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE)) {
                                link.getLinkStatus().removeStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                                link.setEnabled(true);
                            }
                        }
                    }
                }
                logger.info("termination broadcast");
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_TERMINATION_INACTIVE, this));
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_STOP, this));
                setDownloadStatus(DOWNLOAD_NOT_RUNNING);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Startet den download wenn er angehalten ist und hält ihn an wenn er läuft
     */
    public void toggleStartStop() {
        if (!startDownloads()) {
            stopDownloads();
        }
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

    public DownloadLink getDownloadLinkByFileOutput(File file, Integer Linkstatus) {
        // synchronized (packages) {
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
                    if (new File(nextDownloadLink.getFileOutput()).getAbsoluteFile().equals(file.getAbsoluteFile())) {
                        if (Linkstatus != null) {
                            if (nextDownloadLink.getLinkStatus().hasStatus(Linkstatus)) return nextDownloadLink;
                        } else
                            return nextDownloadLink;
                    }
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
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        return ret;
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
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        return ret;
    }

    public void distributeLinks(String data) {
        new DistributeData(data).start();
    }

}
