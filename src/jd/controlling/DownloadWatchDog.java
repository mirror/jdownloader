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
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.ProxyInfo;
import jd.controlling.reconnect.Reconnecter;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.event.ControlEvent;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDUtilities;

import org.appwork.controlling.State;
import org.appwork.controlling.StateMachine;
import org.appwork.controlling.StateMachineInterface;
import org.appwork.controlling.StateMonitor;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.net.throttledconnection.ThrottledConnectionManager;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;

public class DownloadWatchDog implements DownloadControllerListener, StateMachineInterface, ShutdownVetoListener {

    /*
     * inner class to provide everything thats needed in order to start a
     * download
     */
    public class DownloadControlInfo {
        public DownloadLink link;
        public ProxyInfo    proxy;
        public Account      account;
    }

    public static final State IDLE_STATE     = new State("IDLE");
    public static final State RUNNING_STATE  = new State("RUNNING");
    public static final State STOPPING_STATE = new State("STOPPING");
    public static final State STOPPED_STATE  = new State("STOPPED_STATE");
    static {
        IDLE_STATE.addChildren(RUNNING_STATE);
        RUNNING_STATE.addChildren(STOPPING_STATE);
        STOPPING_STATE.addChildren(STOPPED_STATE);
    }

    public static enum STOPMARK {
        /* no stopmark is set */
        NONE,
        /*
         * stopmark is set but no longer visible, eg link/package removed from
         * list
         */
        HIDDEN,
        /* to set a random stopmark */
        RANDOM
    }

    private final LinkedList<SingleDownloadController>                 DownloadControllers   = new LinkedList<SingleDownloadController>();
    private final LinkedList<DownloadLink>                             forcedLinks           = new LinkedList<DownloadLink>();
    private final LinkedList<DownloadLink>                             sessionHistory        = new LinkedList<DownloadLink>();
    private final HashMap<String, ArrayList<SingleDownloadController>> activeDownloadsbyHost = new HashMap<String, ArrayList<SingleDownloadController>>();

    private Object                                                     currentstopMark       = STOPMARK.NONE;

    private static final Logger                                        LOG                   = JDLogger.getLogger();

    private boolean                                                    paused                = false;

    private Thread                                                     watchDogThread        = null;

    private DownloadController                                         dlc                   = null;
    private AtomicInteger                                              activeDownloads       = new AtomicInteger(0);

    private StateMachine                                               stateMachine          = null;
    private final ThrottledConnectionManager                           connectionManager;
    private StateMonitor                                               stateMonitor          = null;
    private int                                                        lastReconnectCounter  = 0;
    private GeneralSettings                                            config;

    /**
     * Hier kann de Status des Downloads gespeichert werden.
     */

    private final static DownloadWatchDog                              INSTANCE              = new DownloadWatchDog();

    public static DownloadWatchDog getInstance() {
        return INSTANCE;
    }

    private DownloadWatchDog() {
        config = JsonConfig.create(GeneralSettings.class);

        this.connectionManager = new ThrottledConnectionManager();
        this.connectionManager.setIncommingBandwidthLimit(config.getDownloadSpeedLimit() * 1024);
        stateMachine = new StateMachine(this, IDLE_STATE, STOPPED_STATE);
        stateMonitor = new StateMonitor(stateMachine);
        this.dlc = DownloadController.getInstance();
        this.dlc.addListener(this);
        ShutdownController.getInstance().addShutdownVetoListener(this);
    }

    /**
     * registers the given SingleDownloadController in this DownloadWatchDog
     * 
     * 
     * @param con
     */
    protected void registerSingleDownloadController(final SingleDownloadController con) {
        DownloadLink link = con.getDownloadLink();
        synchronized (this.DownloadControllers) {
            if (this.DownloadControllers.contains(link)) {
                throw new IllegalStateException("SingleDownloadController already registered");
            } else {
                this.DownloadControllers.add(con);
                /* increase running downloads and downloadssincelastStart */
                this.activeDownloads.incrementAndGet();
            }
            /* increase active counter for this hoster */
            String host = link.getHost();
            ArrayList<SingleDownloadController> active = this.activeDownloadsbyHost.get(host);
            if (active == null) {
                active = new ArrayList<SingleDownloadController>();
                this.activeDownloadsbyHost.put(host, active);
            }
            active.add(con);
            /* add download to sessionHistory */
            synchronized (sessionHistory) {
                if (!sessionHistory.contains(con.getDownloadLink())) {
                    sessionHistory.add(con.getDownloadLink());
                }
            }
        }
    }

    /**
     * returns how many downloads for a given host are currently running
     * 
     * @param host
     * @return
     */
    private int activeDownloadsbyHosts(final String host) {
        synchronized (activeDownloadsbyHost) {
            final ArrayList<SingleDownloadController> ret = activeDownloadsbyHost.get(host);
            if (ret != null) return ret.size();
        }
        return 0;
    }

    /**
     * checks if there is enough diskspace left to use given amount of
     * diskspace, only works with java >=1.6
     * 
     * @param dlLink
     * @return
     */
    public boolean checkFreeDiskSpace(final File file2Root, long diskspace) {
        if (Application.getJavaVersion() < 16000000) {
            /*
             * File.getUsableSpace is 1.6 only
             */
            return true;
        }
        File f = file2Root.getParentFile();
        if (f == null) {
            /* no parent folder, seems user set an invalid folder?! */
            return false;
        }
        while (!f.exists()) {
            f = f.getParentFile();
            if (f == null) { return false; }
        }
        /* Set 500MB extra Buffer */

        long spaceneeded = 1024l * 1024 * config.getForcedFreeSpaceOnDisk();
        /* calc the needed space for the current running downloads */
        synchronized (this.DownloadControllers) {
            for (final SingleDownloadController con : this.DownloadControllers) {
                DownloadLink dlink = con.getDownloadLink();
                spaceneeded += dlink.getDownloadSize() - dlink.getDownloadCurrent();
            }
        }
        /* enough space for needed diskspace */
        if (f.getUsableSpace() < (spaceneeded + diskspace)) { return false; }
        return true;
    }

    /**
     * reset linkstatus for files where it is usefull and needed
     */
    private void clearDownloadListStatus() {
        /* reset ip waittimes only for local ips */
        ProxyController.getInstance().resetIPBlockWaittime(null, true);
        /* reset temp unavailble times for all ips */
        ProxyController.getInstance().resetTempUnavailWaittime(null, false);
        synchronized (DownloadController.ACCESSLOCK) {
            for (final FilePackage filePackage : DownloadController.getInstance().getPackages()) {
                synchronized (filePackage) {
                    for (final DownloadLink link : filePackage.getControlledDownloadLinks()) {
                        /*
                         * do not reset if link is offline, finished , already
                         * exist or pluginerror (because only plugin updates can
                         * fix this)
                         */
                        link.getLinkStatus().resetStatus(LinkStatus.ERROR_FATAL | LinkStatus.ERROR_PLUGIN_DEFECT | LinkStatus.ERROR_ALREADYEXISTS, LinkStatus.ERROR_FILE_NOT_FOUND, LinkStatus.FINISHED);
                    }
                }
            }
        }
        DownloadController.getInstance().fireGlobalUpdate();
    }

    /**
     * unregister the given SingleDownloadController from this DownloadWatchDog
     * 
     * @param con
     */
    protected void unregisterSingleDownloadController(final SingleDownloadController con) {
        DownloadLink link = con.getDownloadLink();
        synchronized (this.DownloadControllers) {
            if (this.DownloadControllers.remove(con) == false) {
                throw new IllegalStateException("SingleDownloadController not registed!");
            } else {
                /* remove download from active download counter */
                this.activeDownloads.decrementAndGet();
                String host = link.getHost();
                ArrayList<SingleDownloadController> active = this.activeDownloadsbyHost.get(host);
                if (active == null) {
                    throw new IllegalStateException("SingleDownloadController not registed!");
                } else {
                    active.remove(con);
                    if (active.size() == 0) {
                        this.activeDownloadsbyHost.remove(host);
                    }
                }
            }
        }
    }

    public boolean forcedLinksWaiting() {
        synchronized (forcedLinks) {
            return forcedLinks.size() > 0;
        }
    }

    /**
     * try to force a downloadstart, will ignore maxperhost and maxdownloads
     * limits
     */
    public void forceDownload(final ArrayList<DownloadLink> linksForce) {
        if (linksForce == null || linksForce.size() == 0) return;
        IOEQ.add(new Runnable() {
            public void run() {
                if (DownloadWatchDog.this.stateMachine.isState(STOPPING_STATE)) {
                    /*
                     * controller will shutdown soon, so no sense in forcing
                     * downloads now
                     */
                    return;
                }
                if (DownloadWatchDog.this.stateMachine.isStartState() || DownloadWatchDog.this.stateMachine.isFinal()) {
                    /*
                     * no downloads are running, so we will force only the
                     * selected links to get started by setting stopmark to
                     * first forced link
                     */
                    DownloadWatchDog.this.setStopMark(linksForce.get(0));
                    DownloadWatchDog.this.startDownloads();
                }
                /* add links to forcedLinks list */
                synchronized (forcedLinks) {
                    forcedLinks.addAll(linksForce);
                }
            }
        }, true);
    }

    /**
     * returns how many downloads are currently watched by this DownloadWatchDog
     * 
     * @return
     */
    public int getActiveDownloads() {
        return this.activeDownloads.get();
    }

    /**
     * returns the ThrottledConnectionManager of this DownloadWatchDog
     * 
     * @return
     */
    public ThrottledConnectionManager getConnectionManager() {
        return this.connectionManager;
    }

    /**
     * returns how many downloads were started since in this session
     * 
     * @return
     */
    public int getDownloadssincelastStart() {
        synchronized (sessionHistory) {
            return sessionHistory.size();
        }
    }

    /**
     * returns DownloadControlInfo for next forced Download
     * 
     * @return
     */
    private DownloadControlInfo getNextForcedDownloadLink() {
        DownloadLink link = null;
        synchronized (forcedLinks) {
            if (forcedLinks.size() > 0) link = forcedLinks.removeFirst();
        }
        if (link == null) return null;
        if (link.getDefaultPlugin() == null || link.getLinkStatus().isPluginActive()) {
            /* no plugin available or plugin already active */
            return null;
        }
        final boolean tryAcc = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true);
        Account acc = null;
        if (!link.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE)) {
            if (tryAcc) {
                acc = AccountController.getInstance().getValidAccount(link.getDefaultPlugin());
            }
            /* search next possible proxy for this download */
            ProxyInfo proxy = ProxyController.getInstance().getProxyForDownload(link.getDefaultPlugin(), acc);
            if (proxy != null) {
                if (link.getLinkStatus().isStatus(LinkStatus.TODO)) {
                    if (!link.isEnabled()) {
                        link.setEnabled(true);
                    }
                    final DownloadControlInfo ret = new DownloadControlInfo();
                    ret.link = link;
                    ret.account = acc;
                    ret.proxy = proxy;
                    return ret;
                }
            }
        }
        return null;
    }

    /**
     * returns DownloadControlInfo for next possible Download
     * 
     * @return
     */
    public DownloadControlInfo getNextDownloadLink() {
        /* we first check if there is a forced download waiting to get started */
        DownloadControlInfo ret = getNextForcedDownloadLink();
        if (ret != null) {
            return ret;
        } else {
            ret = new DownloadControlInfo();
        }
        final boolean tryAcc = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true);
        DownloadLink nextDownloadLink = null;
        Account acc = null;
        String accHost = null;
        final int maxPerHost = this.getSimultanDownloadNumPerHost();
        try {
            for (final FilePackage filePackage : this.dlc.getPackages()) {
                for (final Iterator<DownloadLink> it2 = filePackage.getControlledDownloadLinks().iterator(); it2.hasNext();) {
                    nextDownloadLink = it2.next();
                    if (nextDownloadLink.getDefaultPlugin() == null || !nextDownloadLink.isEnabled() || nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE)) {
                        /*
                         * no plugin available, download not enabled,link temp
                         * unavailable
                         */
                        continue;
                    }
                    if (nextDownloadLink.getLinkStatus().isPluginActive() || (!nextDownloadLink.getLinkStatus().isStatus(LinkStatus.TODO) && !nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED))) {
                        /* download is already in progress or not todo */
                        continue;
                    }
                    if (activeDownloadsbyHosts(nextDownloadLink.getHost()) >= maxPerHost) {
                        /* max downloads per host reached */
                        continue;
                    }
                    /* Account Handling */
                    if (tryAcc) {
                        if (accHost == null || !accHost.equalsIgnoreCase(nextDownloadLink.getHost())) {
                            /*
                             * in case there is no current account or account
                             * host does not match download host, get new
                             * account
                             */
                            acc = AccountController.getInstance().getValidAccount(nextDownloadLink.getDefaultPlugin());
                            if (acc != null) {
                                accHost = nextDownloadLink.getHost();
                            }
                        }
                    }
                    /* search next possible proxy for this download */
                    ProxyInfo proxy = ProxyController.getInstance().getProxyForDownload(nextDownloadLink.getDefaultPlugin(), acc);
                    if (proxy != null) {
                        /* possible proxy found */
                        if (ret.link == null || nextDownloadLink.getPriority() > ret.link.getPriority()) {
                            /*
                             * next download found or download with higher
                             * priority
                             */
                            ret.proxy = proxy;
                            ret.link = nextDownloadLink;
                            ret.account = acc;
                        }
                    }
                }
            }
        } catch (final Exception e) {
            /*
             * because of speed reasons, nothing is synched here...ugly but okay
             * for the moment, rewrite when there is time
             */
        }
        if (ret.link == null) { return null; }
        return ret;
    }

    /**
     * returns how many downloads are running that may not get interrupted by a
     * reconnect
     * 
     * @return
     */
    public int getForbiddenReconnectDownloadNum() {
        final boolean allowinterrupt = config.isInterruptResumeableDownloadsEnable();

        int ret = 0;
        synchronized (this.DownloadControllers) {
            for (final SingleDownloadController con : DownloadControllers) {
                DownloadLink link = con.getDownloadLink();
                if (link.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                    if (!(link.getTransferStatus().supportsResume() && allowinterrupt)) ret++;
                }
            }
        }
        return ret;
    }

    /**
     * returns how many concurrent downloads may run
     * 
     * @return
     */
    public int getSimultanDownloadNum() {

        return config.getMaxSimultaneDownloads();
    }

    /**
     * returns how many concurrent downloads from the same host may run
     * 
     * @return
     */
    public int getSimultanDownloadNumPerHost() {
        int ret = 0;
        if ((ret = config.getMaxSimultaneDownloadsPerHost()) == 0) { return Integer.MAX_VALUE; }
        return ret;
    }

    /**
     * returns current pause state
     * 
     * @return
     */
    public boolean isPaused() {
        return this.paused;
    }

    /**
     * returns true if the given object is our set stopmark
     * 
     * @param item
     * @return
     */
    public boolean isStopMark(final Object item) {
        return this.currentstopMark == item;
    }

    /**
     * returns if currently there is a stopmark set
     * 
     * @return
     */
    public boolean isStopMarkSet() {
        return this.currentstopMark != STOPMARK.NONE;
    }

    /**
     * may the DownloadWatchDog start new Downloads?
     * 
     * @return
     */
    private boolean newDLStartAllowed() {
        if (!DownloadWatchDog.this.stateMachine.isState(DownloadWatchDog.RUNNING_STATE)) {
            /*
             * only allow new downloads in running state
             */
            return false;
        }
        if (this.paused) {
            /* pause is active */
            return false;
        }
        if (Reconnecter.getInstance().isReconnectInProgress()) {
            /* reconnect in progress */
            return false;
        }
        if (Reconnecter.getInstance().isAutoReconnectEnabled() && config.isDownloadControllerPrefersReconnectEnabled() && IPController.getInstance().isInvalidated()) {
            /*
             * auto reconnect is enabled and downloads are waiting for reconnect
             * and user set to wait for reconnect
             */
            return false;
        }
        return true;
    }

    /**
     * this keeps track of stopmark in case the link/package got removed from
     * downloadlist
     */
    @SuppressWarnings("deprecation")
    public void onDownloadControllerEvent(final DownloadControllerEvent event) {
        switch (event.getEventID()) {
        case DownloadControllerEvent.REMOVE_FILPACKAGE:
        case DownloadControllerEvent.REMOVE_DOWNLOADLINK:
            if (this.currentstopMark == event.getParameter()) {
                /* now the stopmark is hidden */
                this.setStopMark(STOPMARK.HIDDEN);
            }
        }
    }

    /**
     * pauses the DownloadWatchDog
     * 
     * @param value
     */
    private int speedBeforePause = 0;

    public void pauseDownloadWatchDog(final boolean value) {
        IOEQ.add(new Runnable() {

            public void run() {
                if (DownloadWatchDog.this.paused == value) { return; }
                DownloadWatchDog.this.paused = value;
                if (value) {
                    ActionController.getToolBarAction("toolbar.control.pause").setSelected(true);
                    speedBeforePause = config.getDownloadSpeedLimit();

                    config.setDownloadSpeedLimit(config.getPauseSpeed());
                    // JSonWrapper.get("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED,
                    // JSonWrapper.get("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_PAUSE_SPEED,
                    // 10));
                    DownloadWatchDog.LOG.info("Pause enabled: Reducing downloadspeed to " + config.getPauseSpeed() + " KiB/s");
                } else {
                    config.setDownloadSpeedLimit(speedBeforePause);
                    // JSonWrapper.get("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED,
                    // JSonWrapper.get("DOWNLOAD").getIntegerProperty("MAXSPEEDBEFOREPAUSE",
                    // 0));
                    speedBeforePause = 0;
                    DownloadWatchDog.LOG.info("Pause disabled: Switch back to old downloadspeed");
                    ActionController.getToolBarAction("toolbar.control.pause").setSelected(false);
                }

            }
        }, true);
    }

    /**
     * checks if the Stopmark has been reached
     * 
     * @return
     */
    private boolean isStopMarkReached() {
        if (forcedLinksWaiting()) {
            /* we still have forced links waiting for start */
            return false;
        }
        Object stop = this.currentstopMark;
        if (stop == STOPMARK.HIDDEN) { return true; }
        if (stop instanceof DownloadLink) {
            synchronized (sessionHistory) {
                if (sessionHistory.contains(stop)) {
                    /*
                     * we already started this download in current session, so
                     * stopmark reached
                     */
                    return true;
                }
            }
            final DownloadLink dl = (DownloadLink) stop;
            if (!dl.isEnabled()) { return true; }
            if (dl.getLinkStatus().isFinished()) { return true; }
            return false;
        }
        if (stop instanceof FilePackage) {
            synchronized (stop) {
                for (final DownloadLink dl : ((FilePackage) stop).getControlledDownloadLinks()) {
                    synchronized (sessionHistory) {
                        if (sessionHistory.contains(dl)) {
                            /*
                             * we already started this download in current
                             * session, so stopmark reached
                             */
                            continue;
                        }
                    }
                    if (dl.isEnabled() && dl.getLinkStatus().isFinished()) {
                        continue;
                    }
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * resets IPBlockWaittime for the given Host
     * 
     * @param host
     */
    public void resetIPBlockWaittime(final String host) {
        /* reset ip waittimes only for local ip */
        ProxyController.getInstance().resetIPBlockWaittime(host, true);
    }

    /**
     * resets TempUnavailWaittime for the given Host
     * 
     * @param host
     */
    public void resetTempUnavailWaittime(final String host) {
        ProxyController.getInstance().resetTempUnavailWaittime(host, false);
    }

    /**
     * aborts all running SingleDownloadControllers, NOTE: DownloadWatchDog is
     * still running, new Downloads will can started after this call
     */
    public void abortAllSingleDownloadControllers() {
        ArrayList<SingleDownloadController> list = new ArrayList<SingleDownloadController>();
        synchronized (DownloadControllers) {
            list.addAll(DownloadControllers);
            for (SingleDownloadController con : DownloadControllers) {
                con.abortDownload();
            }
        }
        /* wait till all downloads are stopped */
        int waitStop = DownloadWatchDog.this.activeDownloads.get();
        if (waitStop > 0) {
            final ProgressController progress = new ProgressController(_JDT._.jd_controlling_DownloadWatchDog_stopping(waitStop), waitStop, null);
            try {
                while (true) {
                    boolean alive = true;
                    for (SingleDownloadController con : list) {
                        if (con.isAlive()) {
                            alive = true;
                            break;
                        }
                    }
                    if (alive == false) break;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        JDLogger.exception(e);
                    }
                    progress.setStatusText("Stopping all downloads " + waitStop);
                }
            } finally {
                progress.doFinalize();
            }
        }
    }

    /**
     * resets the given SingleDownloadController
     * 
     * @param con
     */
    public void resetSingleDownloadController(final SingleDownloadController con) {
        IOEQ.add(new Runnable() {
            public void run() {
                final DownloadLink link = con.getDownloadLink();
                con.getStateMonitor().executeOnceOnState(new Runnable() {

                    public void run() {
                        /* reset waittimes when controller reached final state */
                        String host = link.getHost();
                        resetIPBlockWaittime(host);
                        resetTempUnavailWaittime(host);
                        link.reset();
                    }

                }, SingleDownloadController.FINAL_STATE);
                if (link.getLinkStatus().isPluginActive()) {
                    /* abort download if still active */
                    con.abortDownload();
                }
            }
        }, true);
    }

    /**
     * activates new Downloads as long as possible and returns how many got
     * activated
     * 
     * @return
     **/
    private int setDownloadActive() {
        DownloadControlInfo dci = null;
        int ret = 0;
        int maxDownloads = getSimultanDownloadNum();
        while ((this.forcedLinksWaiting() || this.activeDownloads.get() < maxDownloads)) {
            if (!this.newDLStartAllowed()) { return ret; }
            if (this.isStopMarkReached()) { return ret; }
            dci = this.getNextDownloadLink();
            if (dci == null) {
                /* no next possible download found */
                return ret;
            }
            DownloadLink dlLink = dci.link;
            if (!this.checkFreeDiskSpace(new File(dlLink.getFileOutput()), (dlLink.getDownloadSize() - dlLink.getDownloadCurrent()))) {
                dci.link.getLinkStatus().setStatus(LinkStatus.NOT_ENOUGH_HARDDISK_SPACE);
                continue;
            }
            this.activateSingleDownloadController(dci);
            ret++;
        }
        return ret;
    }

    /**
     * set a new StopMark, null == nostopMark
     * 
     * @param stopEntry
     */
    public void setStopMark(final Object stopEntry) {
        IOEQ.add(new Runnable() {
            public void run() {
                Object entry = stopEntry;
                if (entry == null || entry == STOPMARK.NONE) {
                    entry = STOPMARK.NONE;
                }
                if (entry == STOPMARK.RANDOM) {
                    /* user wants to set a random stopmark */
                    synchronized (DownloadWatchDog.this.DownloadControllers) {
                        if (DownloadWatchDog.this.DownloadControllers.size() > 0) {
                            /* use first running download */
                            entry = DownloadWatchDog.this.DownloadControllers.getFirst().getDownloadLink();
                        } else {
                            /*
                             * no running download available, set stopmark to
                             * none
                             */
                            entry = STOPMARK.NONE;
                        }
                    }
                }
                DownloadWatchDog.this.currentstopMark = entry;
                final ToolBarAction stopMark = ActionController.getToolBarAction("toolbar.control.stopmark");
                /* set new stopmark */
                if (entry instanceof DownloadLink) {
                    stopMark.setSelected(true);
                    stopMark.setEnabled(true);
                    stopMark.setToolTipText(_JDT._.jd_controlling_DownloadWatchDog_stopmark_downloadlink(((DownloadLink) entry).getName()));
                    DownloadController.getInstance().fireDownloadLinkUpdate(entry);
                } else if (entry instanceof FilePackage) {
                    stopMark.setSelected(true);
                    stopMark.setEnabled(true);
                    stopMark.setToolTipText(_JDT._.jd_controlling_DownloadWatchDog_stopmark_filepackage(((FilePackage) entry).getName()));
                } else if (entry == STOPMARK.HIDDEN) {
                    stopMark.setSelected(true);
                    stopMark.setEnabled(true);
                    stopMark.setToolTipText(_JDT._.jd_controlling_DownloadWatchDog_stopmark_set());
                } else if (entry == STOPMARK.NONE) {
                    stopMark.setSelected(false);
                    stopMark.setToolTipText(_JDT._.jd_gui_swing_jdgui_actions_actioncontroller_toolbar_control_stopmark_tooltip());
                }
            }
        }, true);
    }

    /**
     * start the DownloadWatchDog
     */
    public void startDownloads() {
        IOEQ.add(new Runnable() {
            public void run() {
                if (DownloadWatchDog.this.stateMachine.isFinal()) {
                    /* downloadwatchdog was in stopped state, so reset it */
                    DownloadWatchDog.this.stateMachine.reset();
                }
                if (!DownloadWatchDog.this.stateMachine.isStartState()) {
                    /* only allow to start when in FinalState(NOT_RUNNING) */
                    return;
                }
                /* set state to running */
                stateMachine.setStatus(RUNNING_STATE);
                /* remove stopsign if it is reached */
                if (isStopMarkReached()) {
                    setStopMark(STOPMARK.NONE);
                }
                /* reset sessionHistory */
                synchronized (sessionHistory) {
                    sessionHistory.clear();
                }
                /* throw start event */
                DownloadWatchDog.LOG.info("DownloadWatchDog: start");
                JDController.getInstance().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_START, this));
                /* start watchdogthread */
                startWatchDogThread();
            }
        }, true);
    }

    /**
     * activates a new SingleDownloadController for the given
     * DownloadControlInfo
     * 
     * @param dci
     */
    private void activateSingleDownloadController(final DownloadControlInfo dci) {
        DownloadWatchDog.LOG.info("Start new Download: " + dci.link.getHost() + ":" + dci.link.getName() + ":" + dci.proxy);
        final SingleDownloadController download = new SingleDownloadController(dci.link, dci.account, dci.proxy);
        registerSingleDownloadController(download);
        download.getStateMonitor().executeOnceOnState(new Runnable() {

            public void run() {
                unregisterSingleDownloadController(download);
            }

        }, SingleDownloadController.FINAL_STATE);
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
                    this.setName("DownloadWatchDog");
                    try {
                        LinkedList<DownloadLink> links;
                        final ArrayList<FilePackage> fps = new ArrayList<FilePackage>();
                        DownloadLink link;
                        LinkStatus linkStatus;
                        boolean hasInProgressLinks;
                        boolean hasTempDisabledLinks;
                        boolean waitingNewIP;
                        boolean resetWaitingNewIP;
                        int stopCounter = 2;
                        int inProgress = 0;
                        /*
                         * TODO: optimize for less ressource using, eg only walk
                         * through list when changes are available
                         */
                        while (DownloadWatchDog.this.stateMachine.isState(DownloadWatchDog.RUNNING_STATE)) {
                            /* start new download while we are in running state */
                            hasInProgressLinks = false;
                            hasTempDisabledLinks = false;
                            waitingNewIP = false;
                            resetWaitingNewIP = false;
                            if (DownloadWatchDog.this.lastReconnectCounter < Reconnecter.getReconnectCounter()) {
                                /* an IP-change happend, reset waittimes */
                                lastReconnectCounter = Reconnecter.getReconnectCounter();
                                ProxyController.getInstance().resetIPBlockWaittime(null, true);
                                resetWaitingNewIP = true;
                            }
                            /* so we can work on a list without threading errors */
                            fps.clear();
                            synchronized (DownloadController.ACCESSLOCK) {
                                /*
                                 * TODO: change to a much better way, for
                                 * example keep copy of current structure and
                                 * build temp structure to optimize
                                 * nextDownloadLink
                                 */
                                fps.addAll(DownloadWatchDog.this.dlc.getPackages());
                            }
                            inProgress = 0;
                            try {
                                for (final FilePackage filePackage : fps) {
                                    links = filePackage.getControlledDownloadLinks();
                                    for (int i = 0; i < links.size(); i++) {
                                        link = links.get(i);
                                        if (link.getDefaultPlugin() == null) {
                                            /* download does not have a plugin?! */
                                            continue;
                                        }
                                        linkStatus = link.getLinkStatus();
                                        if (link.isEnabled() && !linkStatus.isPluginActive()) {
                                            /* enabled and not in progress */
                                            if (linkStatus.hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) || linkStatus.hasStatus(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE)) {
                                                /*
                                                 * download or hoster temp.
                                                 * unavail
                                                 */
                                                if (linkStatus.getRemainingWaittime() == 0) {
                                                    /*
                                                     * clear blocked accounts
                                                     * for this host
                                                     */
                                                    if (linkStatus.hasStatus(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE)) {
                                                        AccountController.getInstance().removeAccountBlocked(link.getHost());
                                                    }
                                                    /* reset if waittime is over */
                                                    linkStatus.reset(false);
                                                } else if (linkStatus.getRemainingWaittime() > 0) {
                                                    /*
                                                     * we have temp. unavail
                                                     * links in list
                                                     */
                                                    hasTempDisabledLinks = true;
                                                }
                                            } else if (linkStatus.hasStatus(LinkStatus.ERROR_IP_BLOCKED)) {
                                                /* ip blocked link */
                                                if (linkStatus.getRemainingWaittime() == 0 || resetWaitingNewIP) {
                                                    /* reset if waittime is over */
                                                    linkStatus.reset();
                                                    /*
                                                     * clear blocked accounts
                                                     * for this host
                                                     */
                                                    AccountController.getInstance().removeAccountBlocked(link.getHost());
                                                } else if (linkStatus.getRemainingWaittime() > 0) {
                                                    /*
                                                     * we request a reconnect if
                                                     * possible
                                                     */
                                                    if (DownloadWatchDog.this.activeDownloadsbyHosts(link.getHost()) == 0) {
                                                        /*
                                                         * do not reconnect if
                                                         * the request comes
                                                         * from host with active
                                                         * downloads, this will
                                                         * prevent reconnect
                                                         * loops for plugins
                                                         * that allow resume and
                                                         * parallel downloads
                                                         */
                                                        waitingNewIP = true;
                                                        IPController.getInstance().invalidate();
                                                    }
                                                }
                                            } else if (ProxyController.getInstance().hasTempUnavailWaittime(link.getHost()) && !link.getLinkStatus().isFinished()) {
                                                /*
                                                 * we have links that are temp.
                                                 * unavail in list
                                                 */
                                                hasTempDisabledLinks = true;
                                            } else if (ProxyController.getInstance().hasRemainingIPBlockWaittime(link.getHost()) && !link.getLinkStatus().isFinished()) {
                                                /*
                                                 * we have links that are
                                                 * ipblocked in list
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
                                int ret = DownloadWatchDog.this.setDownloadActive();
                                /* request a reconnect if allowed and needed */
                                Reconnecter.getInstance().run();
                                if (ret == 0) {
                                    /*
                                     * no new download got started, check what
                                     * happened and what to do next
                                     */
                                    if (!hasTempDisabledLinks && !hasInProgressLinks && !waitingNewIP && DownloadWatchDog.this.activeDownloads.get() == 0) {
                                        /*
                                         * no tempdisabled, no in progress, no
                                         * reconnect and no next download
                                         * waiting and no active downloads
                                         */
                                        if (DownloadWatchDog.this.newDLStartAllowed()) {
                                            /*
                                             * only start countdown to stop
                                             * downloads if we were allowed to
                                             * start new ones
                                             */
                                            stopCounter--;
                                            DownloadWatchDog.LOG.info(stopCounter + "rounds left to start new downloads");
                                        }
                                        if (stopCounter == 0) {
                                            /*
                                             * countdown reached, prepare to
                                             * stop downloadwatchdog
                                             */
                                            break;
                                        }
                                    }
                                } else {
                                    /*
                                     * reset countdown, because we new downloads
                                     * got started
                                     */
                                    stopCounter = 2;
                                }
                            } catch (final Exception e) {
                                JDLogger.exception(e);
                            }
                            try {
                                Thread.sleep(5000);
                            } catch (final InterruptedException e) {
                            }
                        }
                        stateMachine.setStatus(STOPPING_STATE);
                        /* clear forcedLinks list */
                        synchronized (forcedLinks) {
                            forcedLinks.clear();
                        }
                        DownloadWatchDog.LOG.info("DownloadWatchDog: stopping");
                        /* stop all remaining downloads */
                        synchronized (DownloadControllers) {
                            for (SingleDownloadController con : DownloadControllers) {
                                con.abortDownload();
                            }
                        }
                        /* wait till all downloads are stopped */
                        int waitStop = DownloadWatchDog.this.activeDownloads.get();
                        if (waitStop > 0) {
                            final ProgressController progress = new ProgressController(_JDT._.jd_controlling_DownloadWatchDog_stopping(waitStop), waitStop, null);
                            try {
                                while (true) {
                                    if ((waitStop = DownloadWatchDog.this.activeDownloads.get()) == 0) break;
                                    try {
                                        sleep(1000);
                                    } catch (InterruptedException e) {
                                        JDLogger.exception(e);
                                    }
                                    progress.setStatusText("Stopping all downloads " + waitStop);
                                }
                            } finally {
                                progress.doFinalize();
                            }
                        }
                        /* clear sessionHistory */
                        synchronized (sessionHistory) {
                            sessionHistory.clear();
                        }
                        /* clear Status */
                        clearDownloadListStatus();
                        /* clear blocked Accounts */
                        AccountController.getInstance().removeAccountBlocked((String) null);
                        /* unpause downloads */
                        pauseDownloadWatchDog(false);
                        if (isStopMarkReached()) {
                            /* remove stopsign if it has been reached */
                            setStopMark(STOPMARK.NONE);
                        }
                    } finally {
                        /* full stop reached */
                        DownloadWatchDog.LOG.info("DownloadWatchDog: stopped");
                        stateMachine.setStatus(STOPPED_STATE);
                        JDController.getInstance().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DOWNLOAD_STOP, this));
                    }
                }
            };
            this.watchDogThread.start();
        }
    }

    /**
     * tell the DownloadWatchDog to stop all running Downloads
     */
    public void stopDownloads() {
        IOEQ.add(new Runnable() {
            public void run() {
                if (DownloadWatchDog.this.stateMachine.isFinal() || DownloadWatchDog.this.stateMachine.isStartState()) {
                    /* not downloading */
                    return;
                }
                if (DownloadWatchDog.this.stateMachine.isState(STOPPING_STATE)) {
                    /* download is already in stopping, stopped state */
                    return;
                }
                /* we now want to stop all downloads */
                stateMachine.setStatus(STOPPING_STATE);
            }
        }, true);
    }

    /**
     * toggles between start/stop states
     */
    public void toggleStartStop() {
        IOEQ.add(new Runnable() {
            public void run() {
                if (stateMachine.isStartState() || stateMachine.isFinal()) {
                    /* download is in idle or stopped state */
                    DownloadWatchDog.this.startDownloads();
                } else {
                    /* download can be stopped */
                    DownloadWatchDog.this.stopDownloads();
                }
            }
        }, true);
    }

    /**
     * toggles the stopmark for a given object
     * 
     * @param entry
     */
    public void toggleStopMark(final Object entry) {
        IOEQ.add(new Runnable() {
            public void run() {
                if (entry == null || entry == DownloadWatchDog.this.currentstopMark || entry == STOPMARK.NONE) {
                    /* no stopmark OR toggle current set stopmark */
                    DownloadWatchDog.this.setStopMark(STOPMARK.NONE);
                } else {
                    /* set new stopmark */
                    DownloadWatchDog.this.setStopMark(entry);
                }
            }
        }, true);
    }

    /**
     * needed to keep backwards compatibility to 0.9581 stable
     * 
     * @param host
     * @return
     */
    @Deprecated
    public long getRemainingTempUnavailWaittime(String host) {
        return ProxyController.getInstance().getRemainingTempUnavailWaittime(host);
    }

    /**
     * returns StateMonitor for this DownloadWatchDog
     * 
     * @return
     */
    public StateMonitor getStateMonitor() {
        return stateMonitor;
    }

    /**
     * throws an UnsupportedOperationException, only needed for the internal
     * StateMachine, use getStateMonitor() instead
     */
    @Deprecated
    public StateMachine getStateMachine() {
        throw new UnsupportedOperationException("statemachine not accessible");
    }

    public void onShutdown() {
        stopDownloads();
    }

    public void onShutdownRequest() throws ShutdownVetoException {
        if (this.stateMachine.isState(RUNNING_STATE, STOPPING_STATE)) { throw new ShutdownVetoException("DownloadWatchDog is still running"); }
    }

    public void onShutdownVeto(ArrayList<ShutdownVetoException> vetos) {
    }
}