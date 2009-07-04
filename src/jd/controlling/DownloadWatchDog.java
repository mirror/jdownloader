//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.interaction.Interaction;
import jd.controlling.reconnect.Reconnecter;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

/**
 * Dieser Controller verwaltet die downloads. Während StartDownloads.java für
 * die Steuerung eines einzelnen Downloads zuständig ist, ist DownloadWatchdog
 * für die Verwaltung und steuerung der ganzen Download Liste zuständig
 * 
 * @author JD-Team
 * 
 */
public class DownloadWatchDog implements ControlListener, DownloadControllerListener {

    private boolean aborted = false;

    private boolean aborting;

    private HashMap<DownloadLink, SingleDownloadController> DownloadControllers = new HashMap<DownloadLink, SingleDownloadController>();

    private static final Object nostopMark = new Object();
    private static final Object hiddenstopMark = new Object();
    private Object stopMark = nostopMark;

    private HashMap<Class<?>, Integer> activeHosts = new HashMap<Class<?>, Integer>();

    private Logger logger = JDLogger.getLogger();

    private boolean paused = false;

    private int totalSpeed = 0;

    private Thread watchDogThread = null;

    private DownloadController dlc = null;

    private Integer activeDownloads = new Integer(0);

    private static DownloadWatchDog INSTANCE;

    public synchronized static DownloadWatchDog getInstance() {
        if (INSTANCE == null) INSTANCE = new DownloadWatchDog();
        return INSTANCE;
    }

    public int getActiveDownloads() {
        return activeDownloads;
    }

    private DownloadWatchDog() {
        dlc = DownloadController.getInstance();
        dlc.addListener(this);
    }

    public void start() {
        stopMark = nostopMark;
        /* ursprünglichen speed wiederherstellen */
        if (SubConfiguration.getConfig("DOWNLOAD").getProperty("MAXSPEEDBEFOREPAUSE", null) != null) {
            logger.info("Restoring old speedlimit");
            SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty("MAXSPEEDBEFOREPAUSE", 0));
            SubConfiguration.getConfig("DOWNLOAD").setProperty("MAXSPEEDBEFOREPAUSE", null);
            SubConfiguration.getConfig("DOWNLOAD").save();
        }
        startWatchDogThread();
    }

    public void setStopMark(Object entry) {
        synchronized (stopMark) {
            if (entry == null) entry = nostopMark;
            if (stopMark instanceof DownloadLink) {
                DownloadController.getInstance().fireDownloadLinkUpdate(stopMark);
            } else if (stopMark instanceof FilePackage) {
                DownloadController.getInstance().fireDownloadLinkUpdate(((FilePackage) stopMark).get(0));
            }
            stopMark = entry;
            if (entry instanceof DownloadLink) {
                DownloadController.getInstance().fireDownloadLinkUpdate(entry);
            } else if (entry instanceof FilePackage) {
                DownloadController.getInstance().fireDownloadLinkUpdate(((FilePackage) entry).get(0));
            }
        }
    }

    public void toggleStopMark(Object entry) {
        synchronized (stopMark) {
            if (entry == null || entry == stopMark) {
                setStopMark(nostopMark);
                return;
            }
            setStopMark(entry);
        }
    }

    public Object getStopMark() {
        return stopMark;
    }

    public boolean isStopMark(Object item) {
        return stopMark == item;
    }

    /**
     * Bricht den Watchdog ab. Alle laufenden downloads werden beendet und die
     * downloadliste zurückgesetzt. Diese Funktion blockiert bis alle Downloads
     * erfolgreich abgeborhcen wurden.
     */
    void abort() {
        logger.finer("Breche alle activeLinks ab");
        aborting = true;
        aborted = true;
        ProgressController progress = new ProgressController("Termination", activeDownloads);
        progress.setStatusText("Stopping all downloads " + activeDownloads);
        ArrayList<DownloadLink> al = new ArrayList<DownloadLink>();

        ArrayList<SingleDownloadController> cons = new ArrayList<SingleDownloadController>(DownloadControllers.values());
        for (SingleDownloadController singleDownloadController : cons) {
            al.add(singleDownloadController.abortDownload().getDownloadLink());
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(al);
        boolean check = true;
        // Warteschleife bis alle activelinks abgebrochen wurden
        logger.finer("Warten bis alle activeLinks abgebrochen wurden.");

        while (true) {
            progress.setStatusText("Stopping all downloads " + activeDownloads);
            check = true;
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>(DownloadControllers.keySet());
            for (DownloadLink link : links) {
                if (link.getLinkStatus().isPluginActive()) {
                    check = false;
                    break;
                }
            }
            if (check) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            cons = new ArrayList<SingleDownloadController>(DownloadControllers.values());
            for (SingleDownloadController singleDownloadController : cons) {
                al.add(singleDownloadController.abortDownload().getDownloadLink());
            }
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(al);
        progress.finalize();
        logger.finer("Stopped Downloads");
        clearDownloadListStatus();
        aborting = false;
    }

    /**
     * Setzt den Status der Downloadliste zurück. zB. bei einem Abbruch
     */
    private void clearDownloadListStatus() {
        ArrayList<DownloadLink> links = new ArrayList<DownloadLink>(DownloadControllers.keySet());
        for (DownloadLink link : links) {
            this.deactivateDownload(link);
        }

        PluginForHost.resetStatics();
        ArrayList<FilePackage> fps;
        fps = dlc.getPackages();
        synchronized (fps) {
            for (FilePackage filePackage : fps) {
                links = filePackage.getDownloadLinkList();
                for (int i = 0; i < links.size(); i++) {
                    if (!links.get(i).getLinkStatus().isFinished()) {
                        links.get(i).getLinkStatus().setStatusText(null);
                        links.get(i).setAborted(false);
                        links.get(i).getLinkStatus().setStatus(LinkStatus.TODO);
                        links.get(i).getLinkStatus().resetWaitTime();
                    }
                }
            }
        }
        DownloadController.getInstance().fireGlobalUpdate();
    }

    public ArrayList<DownloadLink> getRunningDownloads() {
        return new ArrayList<DownloadLink>(DownloadControllers.keySet());
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_PLUGIN_INACTIVE && event.getSource() instanceof PluginForHost) {
            this.deactivateDownload(((SingleDownloadController) event.getParameter()).getDownloadLink());
        }
    }

    public int ActiveDownloadControllers() {
        return DownloadControllers.keySet().size();
    }

    /**
     * Zählt die Downloads die bereits über das Hostplugin laufen
     * 
     * @param plugin
     * @return Anzahl der downloads über das plugin
     */
    private int activeDownloadsbyHosts(PluginForHost plugin) {
        synchronized (this.activeHosts) {
            if (activeHosts.containsKey(plugin.getClass())) { return activeHosts.get(plugin.getClass()); }
        }
        return 0;
    }

    private void activateDownload(DownloadLink link, SingleDownloadController con) {
        synchronized (DownloadControllers) {
            if (DownloadControllers.containsKey(link)) return;
            DownloadControllers.put(link, con);
            synchronized (this.activeDownloads) {
                this.activeDownloads++;
            }
        }
        Class<?> cl = link.getPlugin().getClass();
        synchronized (this.activeHosts) {
            if (activeHosts.containsKey(cl)) {
                int count = activeHosts.get(cl);
                activeHosts.put(cl, count + 1);
            } else {
                activeHosts.put(cl, 1);
            }
        }
    }

    private void deactivateDownload(DownloadLink link) {
        synchronized (DownloadControllers) {
            if (!DownloadControllers.containsKey(link)) {
                logger.severe("Link not in ControllerList!");
                return;
            }
            DownloadControllers.remove(link);
            synchronized (this.activeDownloads) {
                this.activeDownloads--;
            }
        }
        Class<?> cl = link.getPlugin().getClass();
        synchronized (this.activeHosts) {
            if (activeHosts.containsKey(cl)) {
                int count = activeHosts.get(cl);
                if (count - 1 < 0) {
                    logger.severe("WatchDog Counter MissMatch!!");
                    activeHosts.remove(cl);
                } else
                    activeHosts.put(cl, count - 1);
            } else
                logger.severe("WatchDog Counter MissMatch!!");
        }
    }

    /**
     * Liefert den nächsten DownloadLink
     * 
     * @return Der nächste DownloadLink oder null
     */
    public DownloadLink getNextDownloadLink() {
        if (this.reachedStopMark()) return null;
        DownloadLink nextDownloadLink = null;
        DownloadLink returnDownloadLink = null;
        try {
            for (FilePackage filePackage : dlc.getPackages()) {
                for (Iterator<DownloadLink> it2 = filePackage.getDownloadLinkList().iterator(); it2.hasNext();) {
                    nextDownloadLink = it2.next();
                    // Setzt die Wartezeit zurück
                    if (!nextDownloadLink.getLinkStatus().isPluginActive() && nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                        nextDownloadLink.reset();
                        nextDownloadLink.getLinkStatus().setStatus(LinkStatus.TODO);
                    }
                    if (nextDownloadLink.isEnabled() && !nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE)) {
                        if (nextDownloadLink.getPlugin().ignoreHosterWaittime() || nextDownloadLink.getPlugin().getRemainingHosterWaittime() <= 0) {
                            if (!isDownloadLinkActive(nextDownloadLink)) {
                                // if (!nextDownloadLink.isAborted()) {
                                if (!nextDownloadLink.getLinkStatus().isPluginActive()) {

                                    if (nextDownloadLink.getLinkStatus().isStatus(LinkStatus.TODO)) {

                                        int maxPerHost = getSimultanDownloadNumPerHost();

                                        if (activeDownloadsbyHosts(nextDownloadLink.getPlugin()) < (nextDownloadLink.getPlugin()).getMaxSimultanDownloadNum(nextDownloadLink) && activeDownloadsbyHosts(nextDownloadLink.getPlugin()) < maxPerHost && nextDownloadLink.getPlugin().getWrapper().usePlugin()) {
                                            if (returnDownloadLink == null) {
                                                returnDownloadLink = nextDownloadLink;
                                            } else {
                                                if (nextDownloadLink.getPriority() > returnDownloadLink.getPriority()) returnDownloadLink = nextDownloadLink;
                                            }
                                        }

                                    }
                                }
                            }
                            // }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.
            // SEVERE,"Exception occurred",e);
            // Fängt concurrentmodification Exceptions ab
        }
        return returnDownloadLink;
    }

    /**
     * Gibt die Configeinstellung zurück, wieviele simultane Downloads der user
     * erlaubt hat
     * 
     * @return
     */
    public int getSimultanDownloadNum() {
        return SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2);
    }

    /**
     * Gibt die Configeinstellung zurück, wieviele simultane Downloads der user
     * pro Hoster erlaubt hat
     * 
     * @return
     */
    public int getSimultanDownloadNumPerHost() {
        if (SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN_PER_HOST, 0) == 0) return Integer.MAX_VALUE;
        return SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN_PER_HOST, 0);
    }

    /**
     * @return the totalSpeed
     */
    public int getTotalSpeed() {
        return totalSpeed;
    }

    public boolean isAborted() {
        if (watchDogThread == null) return false;
        return !watchDogThread.isAlive();
    }

    public boolean isAlive() {
        if (watchDogThread == null) return false;
        return watchDogThread.isAlive();
    }

    boolean isDownloadLinkActive(DownloadLink nextDownloadLink) {
        synchronized (DownloadControllers) {
            return DownloadControllers.containsKey(nextDownloadLink);
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public void pause(boolean value) {
        if (paused == value) return;
        paused = value;
        if (value) {
            SubConfiguration.getConfig("DOWNLOAD").setProperty("MAXSPEEDBEFOREPAUSE", SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0));
            SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_PAUSE_SPEED, 10));
            logger.info("Pause enabled: Reducing downloadspeed to " + SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_PAUSE_SPEED, 10) + " kb/s");
        } else {
            SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty("MAXSPEEDBEFOREPAUSE", 0));
            SubConfiguration.getConfig("DOWNLOAD").setProperty("MAXSPEEDBEFOREPAUSE", null);
            logger.info("Pause disabled: Switch back to old downloadspeed");
        }
        SubConfiguration.getConfig("DOWNLOAD").save();
    }

    public boolean newDLStartAllowed() {
        if (paused || Reconnecter.isReconnecting() || aborting || aborted || Reconnecter.preferReconnect()) return false;
        return true;
    }

    private synchronized void startWatchDogThread() {
        if (this.watchDogThread == null || !this.watchDogThread.isAlive()) {
            /**
             * Workaround, due to activeDownloads bug.
             */
            synchronized (this.activeDownloads) {
                this.activeDownloads = 0;
            }
            watchDogThread = new Thread() {
                @Override
                public void run() {
                    JDUtilities.getController().addControlListener(INSTANCE);
                    this.setName("DownloadWatchDog");
                    ArrayList<DownloadLink> links;
                    ArrayList<DownloadLink> updates = new ArrayList<DownloadLink>();
                    ArrayList<FilePackage> fps;
                    DownloadLink link;
                    LinkStatus linkStatus;
                    boolean hasInProgressLinks;
                    boolean hasTempDisabledLinks;
                    aborted = false;
                    aborting = false;
                    int stopCounter = 5;
                    int currentTotalSpeed = 0;
                    int inProgress = 0;
                    ArrayList<DownloadLink> removes = new ArrayList<DownloadLink>();
                    while (aborted != true) {

                        Reconnecter.hasWaittimeLinks = false;
                        hasInProgressLinks = false;
                        hasTempDisabledLinks = false;

                        fps = dlc.getPackages();
                        currentTotalSpeed = 0;
                        inProgress = 0;
                        updates.clear();
                        try {

                            for (FilePackage filePackage : fps) {
                                links = filePackage.getDownloadLinkList();

                                for (int i = 0; i < links.size(); i++) {
                                    link = links.get(i);
                                    linkStatus = link.getLinkStatus();
                              
                                    // Link mit Temp Unavailable in der Queue
                                    if (link.isEnabled() && linkStatus.hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) && !linkStatus.hasStatus(LinkStatus.PLUGIN_IN_PROGRESS)) {
                                        if (linkStatus.getRemainingWaittime() == 0) {
                                            linkStatus.reset();
                                        } else if (linkStatus.getRemainingWaittime() > 0) {
                                            hasTempDisabledLinks = true;
                                            updates.add(link);
                                        }
                                    }

                                    // Link mit Wartezeit in der queue
                                    if (link.isEnabled() && linkStatus.hasStatus(LinkStatus.ERROR_IP_BLOCKED) && !linkStatus.hasStatus(LinkStatus.PLUGIN_IN_PROGRESS)) {
                                        if (linkStatus.getRemainingWaittime() == 0) {
                                            linkStatus.reset();
                                        } else if (linkStatus.getRemainingWaittime() > 0) {
                                            Reconnecter.hasWaittimeLinks = true;
                                            updates.add(link);
                                        }
                                    }
                                    /* Link mit HosterWartezeit */
                                    if (link.isEnabled() && link.getPlugin().getRemainingHosterWaittime() > 0 && !linkStatus.hasStatus(LinkStatus.PLUGIN_IN_PROGRESS)) {
                                        Reconnecter.hasWaittimeLinks = true;
                                        updates.add(link);
                                    }
                                    // Laufende DownloadLinks
                                    if (link.isEnabled() && linkStatus.isPluginActive()) {
                                        hasInProgressLinks = true;
                                    }
                                    // Laufende und sich im Download befindenten
                                    // Downloads
                                    if (link.isEnabled() && linkStatus.hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                                        inProgress++;
                                        currentTotalSpeed += link.getDownloadSpeed();
                                    }

                                }
                            }
                            if (removes.size() > 0) {
                                for (DownloadLink dl : removes) {
                                    dl.getFilePackage().remove(dl);
                                }
                                removes.clear();
                            }

                            Reconnecter.doReconnectIfRequested(false);
                            if (inProgress > 0) {
                                fps = dlc.getPackages();

                                for (FilePackage filePackage : fps) {

                                    Iterator<DownloadLink> iter = filePackage.getDownloadLinkList().iterator();
                                    int maxspeed = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0) * 1024;
                                    if (maxspeed == 0) {
                                        maxspeed = Integer.MAX_VALUE;
                                    }
                                    int overhead = maxspeed - currentTotalSpeed;

                                    totalSpeed = currentTotalSpeed;

                                    DownloadLink element;
                                    while (iter.hasNext()) {
                                        element = iter.next();
                                        if (element.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {

                                            element.setSpeedLimit(element.getDownloadSpeed() + overhead / inProgress);

                                        }
                                    }
                                }
                            } else {
                                totalSpeed = 0;
                            }
                            if (updates.size() > 0) {
                                DownloadController.getInstance().fireDownloadLinkUpdate(updates);
                            }
                            int ret = 0;
                            if (Interaction.areInteractionsInProgress() && activeDownloads < getSimultanDownloadNum()) {
                                if (!reachedStopMark()) ret = setDownloadActive();
                            }
                            if (ret == 0) {
                                if (!hasTempDisabledLinks && !hasInProgressLinks && !Reconnecter.hasWaittimeLinks && getNextDownloadLink() == null && activeDownloads == 0) {
                                    /*
                                     * nur runterzählen falls auch erlaubt war
                                     * nen download zu starten
                                     */
                                    if (newDLStartAllowed()) {
                                        stopCounter--;
                                        logger.info(stopCounter + "rounds left to start new downloads");
                                    }
                                    if (stopCounter == 0) {
                                        totalSpeed = 0;
                                        break;
                                    }

                                }
                            } else {
                                stopCounter = 5;
                            }
                        } catch (Exception e) {
                            JDLogger.exception(e);
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                    }
                    aborted = true;
                    while (aborting) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                    }
                    JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, this));
                    JDUtilities.getController().removeControlListener(INSTANCE);
                    Interaction.handleInteraction(Interaction.INTERACTION_ALL_DOWNLOADS_FINISHED, this);
                    pause(false);
                }
            };
            watchDogThread.start();
        }
    }

    /**
     * Aktiviert solange neue Downloads, bis die Maxmalanzahl erreicht ist oder
     * die Liste zueende ist
     * 
     * @return
     */
    private int setDownloadActive() {
        DownloadLink dlink;
        int ret = 0;
        if (!newDLStartAllowed()) return ret;
        while (activeDownloads < getSimultanDownloadNum()) {
            dlink = getNextDownloadLink();
            if (dlink == null) {
                break;
            }
            if (dlink != getNextDownloadLink()) {
                break;
            }
            if (reachedStopMark()) return ret;
            startDownloadThread(dlink);
            ret++;
        }
        return ret;
    }

    /**
     * @param totalSpeed
     *            the totalSpeed to set
     */
    public void setTotalSpeed(int totalSpeed) {
        this.totalSpeed = totalSpeed;
    }

    private boolean reachedStopMark() {
        synchronized (stopMark) {
            if (stopMark == hiddenstopMark) return true;
            if (stopMark instanceof DownloadLink) {
                if (((DownloadLink) stopMark).isEnabled() && (((DownloadLink) stopMark).getLinkStatus().isPluginActive() || ((DownloadLink) stopMark).getLinkStatus().hasStatus(LinkStatus.FINISHED))) return true;
                return false;
            }
            if (stopMark instanceof FilePackage) {
                for (DownloadLink dl : ((FilePackage) stopMark).getDownloadLinkList()) {
                    if (dl.getLinkStatus().hasStatus(LinkStatus.FINISHED)) continue;
                    if (dl.isEnabled() && !dl.getLinkStatus().isPluginActive()) return false;
                }
                return true;
            }
            return false;
        }
    }

    private void startDownloadThread(DownloadLink dlink) {
        Interaction.handleInteraction(Interaction.INTERACTION_BEFORE_DOWNLOAD, dlink);
        SingleDownloadController download = new SingleDownloadController(dlink);
        logger.info("Start new Download: " + dlink.getHost());
        dlink.getLinkStatus().setActive(true);
        this.activateDownload(dlink, download);
        download.start();

    }

    public void onDownloadControllerEvent(DownloadControllerEvent event) {
        switch (event.getID()) {
        case DownloadControllerEvent.REMOVE_FILPACKAGE:
        case DownloadControllerEvent.REMOVE_DOWNLOADLINK:
            synchronized (stopMark) {
                if (this.stopMark != null && this.stopMark == event.getParameter()) setStopMark(hiddenstopMark);
            }
            break;
        }

    }
}
