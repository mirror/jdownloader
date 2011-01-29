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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.controlling.reconnect.Reconnecter;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.views.settings.panels.JSonWrapper;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.Application;
import org.appwork.utils.net.throttledconnection.ThrottledConnectionManager;

/**
 * Dieser Controller verwaltet die downloads. Während StartDownloads.java für
 * die Steuerung eines einzelnen Downloads zuständig ist, ist DownloadWatchdog
 * für die Verwaltung und steuerung der ganzen Download Liste zuständig
 * 
 * @author JD-Team
 * 
 */
public class DownloadWatchDog implements ControlListener, DownloadControllerListener {

    public class DownloadControlInfo {
        public DownloadLink link;
        public Account      account;
    }

    public static enum STATE {
        RUNNING, NOT_RUNNING, STOPPING
    }

    private static final String                                   JDL_PREFIX              = "jd.controlling.DownloadWatchDog.";

    private boolean                                               aborted                 = false;

    private boolean                                               aborting;
    private final HashMap<DownloadLink, SingleDownloadController> DownloadControllers     = new HashMap<DownloadLink, SingleDownloadController>();

    private final ArrayList<DownloadLink>                         stopMarkTracker         = new ArrayList<DownloadLink>();
    private final HashMap<String, Long>                           HOST_IPBLOCK            = new HashMap<String, Long>();

    private final HashMap<String, Long>                           HOST_TEMP_UNAVAIL       = new HashMap<String, Long>();
    private static final Object                                   nostopMark              = new Object();
    private static final Object                                   hiddenstopMark          = new Object();

    private Object                                                stopMark                = DownloadWatchDog.nostopMark;

    private final HashMap<String, Integer>                        activeHosts             = new HashMap<String, Integer>();

    private static final Logger                                   LOG                     = JDLogger.getLogger();

    private boolean                                               paused                  = false;

    private Thread                                                watchDogThread          = null;

    private DownloadController                                    dlc                     = null;
    private int                                                   activeDownloads         = 0;

    private int                                                   downloadssincelastStart = 0;

    private final static Object                                   StartStopSync           = new Object();

    private final static Object                                   CountLOCK               = new Object();

    private final static Object                                   DownloadLOCK            = new Object();

    /**
     * Hier kann de Status des Downloads gespeichert werden.
     */
    private STATE                                                 downloadStatus          = STATE.NOT_RUNNING;

    private static DownloadWatchDog                               INSTANCE;

    public synchronized static DownloadWatchDog getInstance() {
        if (DownloadWatchDog.INSTANCE == null) {
            DownloadWatchDog.INSTANCE = new DownloadWatchDog();
        }
        return DownloadWatchDog.INSTANCE;
    }

    private final ThrottledConnectionManager connectionManager;

    private DownloadWatchDog() {
        this.connectionManager = new ThrottledConnectionManager();
        this.connectionManager.setIncommingBandwidthLimit(JSonWrapper.get("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0) * 1024);
        this.dlc = DownloadController.getInstance();
        this.dlc.addListener(this);
    }

    /**
     * Bricht den Watchdog ab. Alle laufenden downloads werden beendet und die
     * downloadliste zurückgesetzt. Diese Funktion blockiert bis alle Downloads
     * erfolgreich abgeborhcen wurden.
     */
    void abort() {
        this.aborting = true;
        DownloadWatchDog.LOG.finer("Abort all active Downloads");
        final ProgressController progress = new ProgressController(JDL.LF(DownloadWatchDog.JDL_PREFIX + "stopping", "Stopping all downloads %s", this.activeDownloads), this.activeDownloads, null);
        final ArrayList<DownloadLink> al = new ArrayList<DownloadLink>();
        ArrayList<SingleDownloadController> cons = null;
        synchronized (this.DownloadControllers) {
            cons = new ArrayList<SingleDownloadController>(this.DownloadControllers.values());
        }
        for (final SingleDownloadController singleDownloadController : cons) {
            al.add(singleDownloadController.abortDownload().getDownloadLink());
        }
        final DownloadController downloadController = DownloadController.getInstance();
        downloadController.fireDownloadLinkUpdate(al);
        boolean check = true;
        while (true) {
            progress.setStatusText("Stopping all downloads " + this.activeDownloads);
            check = true;
            final ArrayList<DownloadLink> links = this.getRunningDownloads();
            for (final DownloadLink link : links) {
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
            } catch (final InterruptedException e) {
            }
            synchronized (this.DownloadControllers) {
                cons = new ArrayList<SingleDownloadController>(this.DownloadControllers.values());
            }
            for (final SingleDownloadController singleDownloadController : cons) {
                al.add(singleDownloadController.abortDownload().getDownloadLink());
            }
        }
        downloadController.fireDownloadLinkUpdate(al);
        progress.doFinalize();
        this.aborting = false;
    }

    private void activateDownload(final DownloadLink link, final SingleDownloadController con) {
        synchronized (this.DownloadControllers) {
            if (this.DownloadControllers.containsKey(link)) { return; }
            this.DownloadControllers.put(link, con);
            synchronized (DownloadWatchDog.CountLOCK) {
                this.activeDownloads++;
                this.downloadssincelastStart++;
            }
        }
        final String cl = link.getHost();
        synchronized (this.activeHosts) {
            if (this.activeHosts.containsKey(cl)) {
                final int count = this.activeHosts.get(cl);
                this.activeHosts.put(cl, count + 1);
            } else {
                this.activeHosts.put(cl, 1);
            }
        }
    }

    public int activeDownloadControllers() {
        synchronized (this.DownloadControllers) {
            return this.DownloadControllers.keySet().size();
        }
    }

    /**
     * Zählt die Downloads die bereits über das Hostplugin laufen
     * 
     * @param plugin
     * @return Anzahl der downloads über das plugin
     */
    private int activeDownloadsbyHosts(final PluginForHost plugin) {
        return this.activeDownloadsbyHosts(plugin.getHost());
    }

    private int activeDownloadsbyHosts(final String host) {
        synchronized (this.activeHosts) {
            if (this.activeHosts.containsKey(host)) { return this.activeHosts.get(host); }
        }
        return 0;
    }

    /**
     * Checks if the Download has enough space.
     * 
     * @param dlLink
     * @return
     */
    private boolean checkSize(final DownloadLink dlLink) {
        if (Application.getJavaVersion() < 16000000) { return true; }

        File f = new File(dlLink.getFileOutput()).getParentFile();

        while (!f.exists()) {
            f = f.getParentFile();
            if (f == null) { return false; }
        }

        // Set 500MB extra Buffer
        long size = 1024 * 1024 * 1024 * 500;

        for (final DownloadLink dlink : this.getRunningDownloads()) {
            size += dlink.getDownloadSize() - dlink.getDownloadCurrent();
        }

        if (f.getUsableSpace() < size + dlLink.getDownloadSize() - dlLink.getDownloadCurrent()) { return false; }

        return true;
    }

    /**
     * Setzt den Status der Downloadliste zurück. zB. bei einem Abbruch
     */
    private void clearDownloadListStatus() {
        ArrayList<DownloadLink> links = this.getRunningDownloads();
        for (final DownloadLink link : links) {
            this.deactivateDownload(link);
        }
        this.resetIPBlockWaittime(null);
        this.resetTempUnavailWaittime(null);
        final ArrayList<FilePackage> fps = this.dlc.getPackages();
        synchronized (fps) {
            for (final FilePackage filePackage : fps) {
                links = filePackage.getDownloadLinkList();
                final int linksSize = links.size();
                for (int i = 0; i < linksSize; i++) {
                    /*
                     * do not reset if link is offline, finished , already exist
                     * or pluginerror (because only plugin updates can fix this)
                     */
                    links.get(i).getLinkStatus().resetStatus(LinkStatus.ERROR_FATAL | LinkStatus.ERROR_PLUGIN_DEFECT | LinkStatus.ERROR_ALREADYEXISTS, LinkStatus.ERROR_FILE_NOT_FOUND, LinkStatus.FINISHED);
                }
            }
        }
        DownloadController.getInstance().fireGlobalUpdate();
    }

    public void controlEvent(final ControlEvent event) {
        if (event.getEventID() == ControlEvent.CONTROL_PLUGIN_INACTIVE && event.getCaller() instanceof PluginForHost) {
            this.deactivateDownload(((SingleDownloadController) event.getParameter()).getDownloadLink());
        }
    }

    private void deactivateDownload(final DownloadLink link) {
        synchronized (this.DownloadControllers) {
            if (!this.DownloadControllers.containsKey(link)) {
                DownloadWatchDog.LOG.severe("Link not in ControllerList!");
                return;
            }
            this.DownloadControllers.remove(link);
            synchronized (DownloadWatchDog.CountLOCK) {
                this.activeDownloads--;
            }
        }
        final String cl = link.getHost();
        synchronized (this.activeHosts) {
            if (this.activeHosts.containsKey(cl)) {
                final int count = this.activeHosts.get(cl);
                if (count - 1 < 0) {
                    DownloadWatchDog.LOG.severe("WatchDog Counter MissMatch!!");
                    this.activeHosts.remove(cl);
                } else {
                    this.activeHosts.put(cl, count - 1);
                }
            } else {
                DownloadWatchDog.LOG.severe("WatchDog Counter MissMatch!!");
            }
        }
    }

    /**
     * try to force a downloadstart, will ignore maxperhost and maxdownloads
     * limits
     */
    public void forceDownload(final ArrayList<DownloadLink> linksForce) {
        synchronized (DownloadWatchDog.DownloadLOCK) {
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            for (final DownloadLink link : linksForce) {
                /* remove links without a plugin */
                if (link.getDefaultPlugin() != null) {
                    links.add(link);
                }
            }
            for (final DownloadLink link : links) {
                if (!link.getDefaultPlugin().isAGBChecked()) {
                    try {
                        SingleDownloadController.onErrorAGBNotSigned(link, link.getDefaultPlugin());
                    } catch (final InterruptedException e) {
                        return;
                    }

                }
            }
            synchronized (DownloadWatchDog.StartStopSync) {
                if (this.downloadStatus == STATE.NOT_RUNNING) {
                    this.startDownloads();
                } else if (this.downloadStatus == STATE.STOPPING) { return; }
            }
            Account acc = null;
            final boolean tryAcc = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true);
            for (final DownloadLink link : links) {
                if (!link.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE)) {
                    if (!tryAcc) {
                        /* accounts global disabled */
                        acc = null;
                    } else {
                        /* host of current account */
                        final String host = AccountController.getInstance().getHosterName(acc);
                        if (host == null || !host.equalsIgnoreCase(link.getHost())) {
                            /*
                             * in case there is no current account or account
                             * host does not match download host, get new
                             * account
                             */
                            acc = AccountController.getInstance().getValidAccount(link.getDefaultPlugin());
                        }
                    }
                    if (acc != null || this.getRemainingIPBlockWaittime(link.getHost()) <= 0 && this.getRemainingTempUnavailWaittime(link.getHost()) <= 0) {
                        if (!this.isDownloadLinkActive(link)) {
                            if (!link.getLinkStatus().isPluginActive()) {
                                if (link.getLinkStatus().isStatus(LinkStatus.TODO)) {
                                    final int activePerHost = this.activeDownloadsbyHosts(link.getDefaultPlugin());
                                    if (activePerHost < link.getDefaultPlugin().getMaxSimultanDownload(acc) && link.getDefaultPlugin().getWrapper().isEnabled()) {
                                        if (!link.isEnabled()) {
                                            link.setEnabled(true);
                                        }
                                        this.startDownloadThread(link, acc);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public int getActiveDownloads() {
        return this.activeDownloads;
    }

    public ThrottledConnectionManager getConnectionManager() {
        return this.connectionManager;
    }

    public int getDownloadssincelastStart() {
        return this.downloadssincelastStart;
    }

    /**
     * Gibt den Status (ID) der downloads zurück
     * 
     * @return
     */
    public STATE getDownloadStatus() {
        return this.downloadStatus;
    }

    /**
     * Liefert den nächsten DownloadLink
     * 
     * @return Der nächste DownloadLink oder null
     */
    public DownloadControlInfo getNextDownloadLink() {
        synchronized (DownloadWatchDog.DownloadLOCK) {
            if (this.reachedStopMark()) { return null; }
            final boolean tryAcc = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true);
            DownloadLink nextDownloadLink = null;
            final DownloadControlInfo ret = new DownloadControlInfo();
            Account acc = null;
            final int maxPerHost = this.getSimultanDownloadNumPerHost();
            try {
                for (final FilePackage filePackage : this.dlc.getPackages()) {
                    for (final Iterator<DownloadLink> it2 = filePackage.getDownloadLinkList().iterator(); it2.hasNext();) {
                        nextDownloadLink = it2.next();
                        if (nextDownloadLink.getDefaultPlugin() == null) {
                            continue;
                        }
                        if (nextDownloadLink.isEnabled() && !nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE)) {
                            if (!tryAcc) {
                                /* accounts global disabled */
                                acc = null;
                            } else {
                                /* host of current account */
                                final String host = AccountController.getInstance().getHosterName(acc);
                                if (host == null || !host.equalsIgnoreCase(nextDownloadLink.getHost())) {
                                    /*
                                     * in case there is no current account or
                                     * account host does not match download
                                     * host, get new account
                                     */
                                    acc = AccountController.getInstance().getValidAccount(nextDownloadLink.getDefaultPlugin());
                                }
                            }
                            /* check for account or non blocked */
                            if (acc != null || this.getRemainingIPBlockWaittime(nextDownloadLink.getHost()) <= 0 && this.getRemainingTempUnavailWaittime(nextDownloadLink.getHost()) <= 0) {
                                if (!this.isDownloadLinkActive(nextDownloadLink)) {
                                    if (!nextDownloadLink.getLinkStatus().isPluginActive()) {
                                        if (nextDownloadLink.getLinkStatus().isStatus(LinkStatus.TODO)) {
                                            final int active = this.activeDownloadsbyHosts(nextDownloadLink.getDefaultPlugin());
                                            if (active < maxPerHost && active < nextDownloadLink.getDefaultPlugin().getMaxSimultanDownload(acc) && nextDownloadLink.getDefaultPlugin().getWrapper().isEnabled()) {
                                                if (ret.link == null || nextDownloadLink.getPriority() > ret.link.getPriority()) {
                                                    /*
                                                     * next download found or
                                                     * download with higher
                                                     * priority
                                                     */
                                                    ret.link = nextDownloadLink;
                                                    ret.account = acc;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (final Exception e) {
            }
            if (ret.link == null) { return null; }
            return ret;
        }
    }

    public long getRemainingIPBlockWaittime(final String host) {
        synchronized (this.HOST_IPBLOCK) {
            if (!this.HOST_IPBLOCK.containsKey(host)) { return 0; }
            return Math.max(0, this.HOST_IPBLOCK.get(host) - System.currentTimeMillis());
        }
    }

    public long getRemainingTempUnavailWaittime(final String host) {
        synchronized (this.HOST_TEMP_UNAVAIL) {
            if (!this.HOST_TEMP_UNAVAIL.containsKey(host)) { return 0; }
            return Math.max(0, this.HOST_TEMP_UNAVAIL.get(host) - System.currentTimeMillis());
        }
    }

    public ArrayList<DownloadLink> getRunningDownloads() {
        synchronized (this.DownloadControllers) {
            return new ArrayList<DownloadLink>(this.DownloadControllers.keySet());
        }
    }

    /**
     * Gibt die Configeinstellung zurück, wieviele simultane Downloads der user
     * erlaubt hat
     * 
     * @return
     */
    public int getSimultanDownloadNum() {
        return JSonWrapper.get("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2);
    }

    /**
     * Gibt die Configeinstellung zurück, wieviele simultane Downloads der user
     * pro Hoster erlaubt hat
     * 
     * @return
     */
    public int getSimultanDownloadNumPerHost() {
        if (JSonWrapper.get("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN_PER_HOST, 0) == 0) { return Integer.MAX_VALUE; }
        return JSonWrapper.get("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN_PER_HOST, 0);
    }

    public Object getStopMark() {
        return this.stopMark;
    }

    boolean isDownloadLinkActive(final DownloadLink nextDownloadLink) {
        synchronized (this.DownloadControllers) {
            return this.DownloadControllers.containsKey(nextDownloadLink);
        }
    }

    public boolean isPaused() {
        return this.paused;
    }

    public boolean isStopMark(final Object item) {
        return this.stopMark == item;
    }

    public boolean isStopMarkSet() {
        return this.stopMark != DownloadWatchDog.nostopMark;
    }

    public boolean newDLStartAllowed() {
        if (this.paused) { return false; }
        if (Reconnecter.getInstance().isReconnectInProgress()) { return false; }
        if (this.aborting || this.aborted) { return false; }
        if (Reconnecter.getInstance().isAutoReconnectEnabled() && JSonWrapper.get("DOWNLOAD").getBooleanProperty("PARAM_DOWNLOAD_PREFER_RECONNECT", true) && IPController.getInstance().isInvalidated()) { return false; }

        return true;
    }

    public void onDownloadControllerEvent(final DownloadControllerEvent event) {
        switch (event.getEventID()) {
        case DownloadControllerEvent.REMOVE_FILPACKAGE:
        case DownloadControllerEvent.REMOVE_DOWNLOADLINK:
            synchronized (this.stopMark) {
                if (this.stopMark == event.getParameter()) {
                    this.setStopMark(DownloadWatchDog.hiddenstopMark);
                }
            }
            break;
        }
    }

    public void pauseDownloads(final boolean value) {
        if (this.paused == value) { return; }
        this.paused = value;
        if (value) {
            ActionController.getToolBarAction("toolbar.control.pause").setSelected(true);
            JSonWrapper.get("DOWNLOAD").setProperty("MAXSPEEDBEFOREPAUSE", JSonWrapper.get("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0));
            JSonWrapper.get("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, JSonWrapper.get("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_PAUSE_SPEED, 10));
            DownloadWatchDog.LOG.info("Pause enabled: Reducing downloadspeed to " + JSonWrapper.get("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_PAUSE_SPEED, 10) + " KiB/s");
        } else {
            JSonWrapper.get("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, JSonWrapper.get("DOWNLOAD").getIntegerProperty("MAXSPEEDBEFOREPAUSE", 0));
            JSonWrapper.get("DOWNLOAD").setProperty("MAXSPEEDBEFOREPAUSE", null);
            DownloadWatchDog.LOG.info("Pause disabled: Switch back to old downloadspeed");
            ActionController.getToolBarAction("toolbar.control.pause").setSelected(false);
        }
        JSonWrapper.get("DOWNLOAD").save();
    }

    private boolean reachedStopMark() {
        synchronized (this.stopMark) {
            if (this.stopMark == DownloadWatchDog.hiddenstopMark) { return true; }
            if (this.stopMark instanceof DownloadLink) {
                if (this.stopMarkTracker.contains(this.stopMark)) { return true; }
                final DownloadLink dl = (DownloadLink) this.stopMark;
                if (!dl.isEnabled()) { return true; }
                if (dl.getLinkStatus().isFinished()) { return true; }
                return false;
            }
            if (this.stopMark instanceof FilePackage) {
                for (final DownloadLink dl : ((FilePackage) this.stopMark).getDownloadLinkList()) {
                    if (this.stopMarkTracker.contains(dl)) {
                        continue;
                    }
                    if (dl.isEnabled() && dl.getLinkStatus().isFinished()) {
                        continue;
                    }
                    return false;
                }
                return true;
            }
            return false;
        }
    }

    public void resetIPBlockWaittime(final String host) {
        synchronized (this.HOST_IPBLOCK) {
            if (host != null) {
                this.HOST_IPBLOCK.remove(host);
            } else {
                this.HOST_IPBLOCK.clear();
            }
        }
    }

    public void resetTempUnavailWaittime(final String host) {
        synchronized (this.HOST_TEMP_UNAVAIL) {
            if (host != null) {
                this.HOST_TEMP_UNAVAIL.remove(host);
            } else {
                this.HOST_TEMP_UNAVAIL.clear();
            }
        }
    }

    /**
     * Aktiviert solange neue Downloads, bis die Maxmalanzahl erreicht ist oder
     * die Liste zueende ist
     * 
     * @return
     */
    private int setDownloadActive() {
        DownloadControlInfo dci = null;
        int ret = 0;
        if (!this.newDLStartAllowed()) { return ret; }
        synchronized (DownloadWatchDog.DownloadLOCK) {
            while (this.activeDownloads < this.getSimultanDownloadNum()) {
                dci = this.getNextDownloadLink();
                if (dci == null) {
                    break;
                }
                /* for what? */
                // if (dlink != getNextDownloadLink()) {
                // break;
                // }
                if (this.reachedStopMark()) { return ret; }
                if (!this.checkSize(dci.link)) {
                    dci.link.getLinkStatus().setStatus(LinkStatus.NOT_ENOUGH_HARDDISK_SPACE);
                    continue;
                }
                this.startDownloadThread(dci.link, dci.account);
                ret++;
            }
        }
        return ret;
    }

    /**
     * set ipblock waittime for given host. <0 for disable/delete
     * 
     * @param host
     * @param until
     */
    public void setIPBlockWaittime(final String host, final long waittime) {
        synchronized (this.HOST_IPBLOCK) {
            if (waittime <= 0) {
                this.HOST_IPBLOCK.remove(host);
            } else {
                this.HOST_IPBLOCK.put(host, System.currentTimeMillis() + waittime);
            }
        }
    }

    public void setStopMark(Object entry) {
        synchronized (this.stopMark) {
            if (entry == null) {
                entry = DownloadWatchDog.nostopMark;
            }
            if (this.stopMark instanceof DownloadLink) {
                DownloadController.getInstance().fireDownloadLinkUpdate(this.stopMark);
            } else if (this.stopMark instanceof FilePackage) {
                DownloadController.getInstance().fireDownloadLinkUpdate(((FilePackage) this.stopMark).get(0));
            }
            this.stopMark = entry;
            final ToolBarAction stopMark = ActionController.getToolBarAction("toolbar.control.stopmark");
            if (entry instanceof DownloadLink) {
                stopMark.setSelected(true);
                stopMark.setEnabled(true);
                stopMark.setToolTipText(JDL.LF(DownloadWatchDog.JDL_PREFIX + "stopmark.downloadlink", "Stopmark is set on Downloadlink: %s", ((DownloadLink) entry).getName()));
                DownloadController.getInstance().fireDownloadLinkUpdate(entry);
            } else if (entry instanceof FilePackage) {
                stopMark.setSelected(true);
                stopMark.setEnabled(true);
                stopMark.setToolTipText(JDL.LF(DownloadWatchDog.JDL_PREFIX + "stopmark.filepackage", "Stopmark is set on Filepackage: %s", ((FilePackage) entry).getName()));
                DownloadController.getInstance().fireDownloadLinkUpdate(((FilePackage) entry).get(0));
            } else if (entry == DownloadWatchDog.hiddenstopMark) {
                stopMark.setSelected(true);
                stopMark.setEnabled(true);
                stopMark.setToolTipText(JDL.L(DownloadWatchDog.JDL_PREFIX + "stopmark.set", "Stopmark is still set!"));
            } else if (entry == DownloadWatchDog.nostopMark) {
                stopMark.setSelected(false);
                stopMark.setToolTipText(JDL.L("jd.gui.swing.jdgui.actions.actioncontroller.toolbar.control.stopmark.tooltip", "Stop after current Downloads"));
            }
        }
    }

    public void setTempUnavailWaittime(final String host, final long waittime) {
        synchronized (this.HOST_TEMP_UNAVAIL) {
            if (waittime <= 0) {
                this.HOST_TEMP_UNAVAIL.remove(host);
            } else {
                this.HOST_TEMP_UNAVAIL.put(host, System.currentTimeMillis() + waittime);
            }
        }
    }

    /**
     * Startet den Downloadvorgang. Dies eFUnkton sendet das startdownload event
     * und aktiviert die ersten downloads.
     */
    public boolean startDownloads() {
        if (this.downloadStatus != STATE.STOPPING && this.downloadStatus != STATE.RUNNING) {
            synchronized (DownloadWatchDog.StartStopSync) {
                if (this.downloadStatus == STATE.NOT_RUNNING) {
                    /* set state to running */
                    this.downloadStatus = STATE.RUNNING;
                    /* clear stopMarkTracker */
                    this.stopMarkTracker.clear();
                    /* remove stopsign if it is reached */
                    if (this.reachedStopMark()) {
                        this.setStopMark(DownloadWatchDog.nostopMark);
                    }
                    /* restore speed limit */
                    /* full start reached */
                    DownloadWatchDog.LOG.info("DownloadWatchDog: start");
                    JDController.getInstance().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_START, this));
                    /* reset downloadcounter */
                    this.downloadssincelastStart = 0;
                    synchronized (DownloadWatchDog.CountLOCK) {
                        this.activeDownloads = 0;
                    }
                    this.startWatchDogThread();
                    return true;
                }
            }
        }
        return false;
    }

    private void startDownloadThread(final DownloadLink dlink, final Account account) {
        dlink.getLinkStatus().setActive(true);
        final SingleDownloadController download = new SingleDownloadController(dlink, account);
        DownloadWatchDog.LOG.info("Start new Download: " + dlink.getHost() + ":" + dlink.getName());
        this.activateDownload(dlink, download);
        /* add download to stopMarkTracker */
        if (!this.stopMarkTracker.contains(dlink)) {
            this.stopMarkTracker.add(dlink);
        }
        download.start();
    }

    private synchronized void startWatchDogThread() {
        if (this.watchDogThread == null || !this.watchDogThread.isAlive()) {
            /**
             * Workaround, due to activeDownloads bug.
             */
            this.watchDogThread = new Thread() {
                @Override
                public void run() {
                    JDUtilities.getController().addControlListener(DownloadWatchDog.INSTANCE);
                    this.setName("DownloadWatchDog");
                    ArrayList<DownloadLink> links;
                    final ArrayList<DownloadLink> updates = new ArrayList<DownloadLink>();
                    final ArrayList<FilePackage> fps = new ArrayList<FilePackage>();
                    DownloadLink link;
                    LinkStatus linkStatus;
                    boolean hasInProgressLinks;
                    boolean hasTempDisabledLinks;
                    boolean waitingNewIP;
                    DownloadWatchDog.this.aborted = false;
                    DownloadWatchDog.this.aborting = false;
                    int stopCounter = 5;
                    int inProgress = 0;
                    while (DownloadWatchDog.this.aborted != true) {

                        hasInProgressLinks = false;
                        hasTempDisabledLinks = false;
                        waitingNewIP = false;

                        /* so we can work on a list without threading errors */
                        fps.clear();
                        synchronized (DownloadController.ControllerLock) {
                            synchronized (DownloadWatchDog.this.dlc.getPackages()) {
                                fps.addAll(DownloadWatchDog.this.dlc.getPackages());
                            }
                        }
                        inProgress = 0;
                        updates.clear();
                        try {
                            for (final FilePackage filePackage : fps) {
                                links = filePackage.getDownloadLinkList();
                                for (int i = 0; i < links.size(); i++) {
                                    link = links.get(i);
                                    if (link.getDefaultPlugin() == null) {
                                        continue;
                                    }
                                    linkStatus = link.getLinkStatus();
                                    if (!linkStatus.hasStatus(LinkStatus.PLUGIN_IN_PROGRESS) && link.isEnabled()) {
                                        /* enabled and not in progress */
                                        if (linkStatus.hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) || linkStatus.hasStatus(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE)) {
                                            /* download or hoster temp. unavail */
                                            if (linkStatus.getRemainingWaittime() == 0) {
                                                /*
                                                 * clear blocked accounts for
                                                 * this host
                                                 */
                                                if (linkStatus.hasStatus(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE)) {
                                                    AccountController.getInstance().removeAccountBlocked(link.getHost());
                                                }
                                                /* reset if waittime is over */
                                                linkStatus.reset(false);
                                            } else if (linkStatus.getRemainingWaittime() > 0) {
                                                /*
                                                 * we have temp. unavail links
                                                 * in list
                                                 */
                                                hasTempDisabledLinks = true;
                                                updates.add(link);
                                            }
                                        } else if (linkStatus.hasStatus(LinkStatus.ERROR_IP_BLOCKED)) {
                                            /* ip blocked link */
                                            if (linkStatus.getRemainingWaittime() == 0) {
                                                /* reset if waittime is over */
                                                linkStatus.reset();
                                                /*
                                                 * clear blocked accounts for
                                                 * this host
                                                 */
                                                AccountController.getInstance().removeAccountBlocked(link.getHost());
                                            } else if (linkStatus.getRemainingWaittime() > 0) {
                                                /*
                                                 * we request a reconnect if
                                                 * possible
                                                 */
                                                if (DownloadWatchDog.this.activeDownloadsbyHosts(link.getHost()) == 0) {
                                                    /*
                                                     * do not reconnect if the
                                                     * request comes from host
                                                     * with active downloads,
                                                     * this will prevent
                                                     * reconnect loops for
                                                     * plugins that allow resume
                                                     * and parallel downloads
                                                     */
                                                    waitingNewIP = true;
                                                    IPController.getInstance().invalidate();
                                                }
                                                updates.add(link);
                                            }
                                        } else if (DownloadWatchDog.this.getRemainingTempUnavailWaittime(link.getHost()) > 0 && !link.getLinkStatus().isFinished()) {
                                            /*
                                             * we have links that are temp.
                                             * unavail in list
                                             */

                                            hasTempDisabledLinks = true;
                                            updates.add(link);
                                        } else if (DownloadWatchDog.this.getRemainingIPBlockWaittime(link.getHost()) > 0 && !link.getLinkStatus().isFinished()) {
                                            /*
                                             * we have links that are ipblocked
                                             * in list
                                             */
                                            if (DownloadWatchDog.this.activeDownloadsbyHosts(link.getHost()) == 0) {
                                                /*
                                                 * do not reconnect if the
                                                 * request comes from host with
                                                 * active downloads, this will
                                                 * prevent reconnect loops for
                                                 * plugins that allow resume and
                                                 * parallel downloads
                                                 */
                                                waitingNewIP = true;
                                                IPController.getInstance().invalidate();
                                            }
                                            updates.add(link);
                                        }
                                    } else if (link.isEnabled()) {
                                        /* enabled links */
                                        if (linkStatus.isPluginActive()) {
                                            /* we have active links in list */
                                            hasInProgressLinks = true;
                                        }
                                        if (linkStatus.hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                                            /* link is downloading atm */
                                            inProgress++;
                                        }
                                    }
                                }
                            }
                            /* request a reconnect if allowed and needed */
                            Reconnecter.getInstance().run();
                            if (updates.size() > 0) {
                                /* fire gui updates */
                                DownloadController.getInstance().fireDownloadLinkUpdate(updates);
                            }
                            int ret = 0;
                            if (DownloadWatchDog.this.activeDownloads < DownloadWatchDog.this.getSimultanDownloadNum()) {
                                if (!DownloadWatchDog.this.reachedStopMark()) {
                                    ret = DownloadWatchDog.this.setDownloadActive();
                                }
                            }
                            if (ret == 0) {
                                /*
                                 * no new download got started, check what
                                 * happened and what to do next
                                 */
                                if (!hasTempDisabledLinks && !hasInProgressLinks && !waitingNewIP && DownloadWatchDog.this.getNextDownloadLink() == null && DownloadWatchDog.this.activeDownloads == 0) {
                                    /*
                                     * no tempdisabled, no in progress, no
                                     * reconnect and no next download waiting
                                     * and no active downloads
                                     */
                                    if (DownloadWatchDog.this.newDLStartAllowed()) {
                                        /*
                                         * only start countdown to stop
                                         * downloads if we were allowed to start
                                         * new ones
                                         */
                                        stopCounter--;
                                        DownloadWatchDog.LOG.info(stopCounter + "rounds left to start new downloads");
                                    }
                                    if (stopCounter == 0) {
                                        /*
                                         * countdown reached, prepare to stop
                                         * downloadwatchdog
                                         */
                                        break;
                                    }
                                }
                            } else {
                                /*
                                 * reset countdown, because we new downloads got
                                 * started
                                 */
                                stopCounter = 5;
                            }
                        } catch (final Exception e) {
                            JDLogger.exception(e);
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (final InterruptedException e) {
                        }
                    }
                    DownloadWatchDog.this.aborted = true;
                    while (DownloadWatchDog.this.aborting) {
                        try {
                            Thread.sleep(1000);
                        } catch (final InterruptedException e) {
                        }
                    }
                    JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, this));
                    JDUtilities.getController().removeControlListener(DownloadWatchDog.INSTANCE);
                    DownloadWatchDog.getInstance().stopDownloads();
                }
            };
            this.watchDogThread.start();
        }
    }

    /**
     * Bricht den Download ab und blockiert bis er abgebrochen wurde.
     */
    public boolean stopDownloads() {
        if (this.downloadStatus != STATE.STOPPING && this.downloadStatus != STATE.NOT_RUNNING) {
            synchronized (DownloadWatchDog.StartStopSync) {
                if (this.downloadStatus == STATE.RUNNING) {
                    /* set state to stopping */
                    this.downloadStatus = STATE.STOPPING;
                    /*
                     * check if there are still running downloads, if so abort
                     * them
                     */
                    this.aborted = true;
                    if (this.getActiveDownloads() > 0) {
                        this.abort();
                    }
                    /* clear Status */
                    this.clearDownloadListStatus();
                    /* clear blocked Accounts */
                    AccountController.getInstance().removeAccountBlocked((String) null);
                    this.pauseDownloads(false);
                    /* remove stopsign if it is reached */
                    if (this.reachedStopMark()) {
                        this.setStopMark(DownloadWatchDog.nostopMark);
                    }
                    /* full stop reached */
                    DownloadWatchDog.LOG.info("DownloadWatchDog: stop");
                    this.downloadStatus = STATE.NOT_RUNNING;
                    JDController.getInstance().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_STOP, this));
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Startet den download wenn er angehalten ist und hält ihn an wenn er läuft
     */
    public void toggleStartStop() {
        if (!this.startDownloads()) {
            this.stopDownloads();
        }
    }

    public void toggleStopMark(final Object entry) {
        synchronized (this.stopMark) {
            if (entry == null || entry == this.stopMark) {
                this.setStopMark(DownloadWatchDog.nostopMark);
            } else {
                this.setStopMark(entry);
            }
        }
    }
}
