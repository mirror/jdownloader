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

package jd.controlling.downloadcontroller;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.AccountController;
import jd.controlling.IOEQ;
import jd.controlling.IOPermission;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.ProxyInfo;
import jd.controlling.reconnect.Reconnecter;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.utils.JDUtilities;

import org.appwork.controlling.State;
import org.appwork.controlling.StateMachine;
import org.appwork.controlling.StateMachineInterface;
import org.appwork.exceptions.WTFException;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.Application;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.FileCreationListener;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.gui.userio.NewUIO;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.IfFileExistsAction;
import org.jdownloader.translate._JDT;

public class DownloadWatchDog implements DownloadControllerListener, StateMachineInterface, ShutdownVetoListener, IOPermission, FileCreationListener {

    /*
     * inner class to provide everything thats needed in order to start a download
     */
    public static class DownloadControlInfo {
        public boolean      byPassSimultanDownloadNum = false;
        public DownloadLink link;
        public ProxyInfo    proxy;
        public Account      account;

        public String toString() {
            return "Proxy: " + proxy + ", Account " + account + " link: " + link + " bypasssim: " + byPassSimultanDownloadNum;
        }
    }

    public static class DownloadControlHistoryItem {
        public int  round     = 0;
        public long timeStamp = -1;
    }

    public static class DownloadControlHistory {
        public HashMap<Account, DownloadControlHistoryItem> accountUsageHistory = new HashMap<Account, DownloadControlHistoryItem>();
    }

    public static final State IDLE_STATE     = new State("IDLE");
    public static final State RUNNING_STATE  = new State("RUNNING");
    public static final State PAUSE_STATE    = new State("PAUSE");
    public static final State STOPPING_STATE = new State("STOPPING");
    public static final State STOPPED_STATE  = new State("STOPPED_STATE");
    static {
        IDLE_STATE.addChildren(RUNNING_STATE);
        RUNNING_STATE.addChildren(STOPPING_STATE, PAUSE_STATE);
        PAUSE_STATE.addChildren(RUNNING_STATE, STOPPING_STATE);
        STOPPING_STATE.addChildren(STOPPED_STATE);
    }

    public static enum STOPMARK {
        /* no stopmark is set */
        NONE,
        /*
         * stopmark is set but no longer visible, eg link/package removed from list
         */
        HIDDEN,
        /* to set a random stopmark */
        RANDOM;
    }

    public static enum DISKSPACECHECK {
        UNKNOWN,
        OK,
        INVALIDFOLDER,
        FAILED
    }

    private final LinkedList<SingleDownloadController>                      downloadControllers    = new LinkedList<SingleDownloadController>();
    private final LinkedList<DownloadLink>                                  forcedLinks            = new LinkedList<DownloadLink>();

    private final HashMap<String, java.util.List<SingleDownloadController>> activeDownloadsbyHost  = new HashMap<String, java.util.List<SingleDownloadController>>();
    private final HashMap<DownloadLink, DownloadControlHistory>             downloadControlHistory = new HashMap<DownloadLink, DownloadControlHistory>();
    private final AtomicInteger                                             sessionDownloads       = new AtomicInteger(0);

    private Object                                                          currentstopMark        = STOPMARK.NONE;

    private Thread                                                          watchDogThread         = null;

    private StateMachine                                                    stateMachine           = null;
    private DownloadSpeedManager                                            dsm                    = null;

    private GeneralSettings                                                 config;

    private HashSet<String>                                                 captchaBlockedHoster   = new HashSet<String>();

    private final static DownloadWatchDog                                   INSTANCE               = new DownloadWatchDog();

    private final LogSource                                                 logger;
    private final AtomicLong                                                WATCHDOGWAITLOOP       = new AtomicLong(0);

    public static DownloadWatchDog getInstance() {
        return INSTANCE;
    }

    private DownloadWatchDog() {
        logger = LogController.CL();
        config = JsonConfig.create(GeneralSettings.class);

        this.dsm = new DownloadSpeedManager();
        this.dsm.setLimit(config.isDownloadSpeedLimitEnabled() ? config.getDownloadSpeedLimit() : 0);
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT.getEventSender().addListener(new GenericConfigEventListener<Integer>() {

            public void onConfigValueModified(KeyHandler<Integer> keyHandler, Integer newValue) {
                dsm.setLimit(config.isDownloadSpeedLimitEnabled() ? config.getDownloadSpeedLimit() : 0);
            }

            public void onConfigValidatorError(KeyHandler<Integer> keyHandler, Integer invalidValue, ValidationException validateException) {
            }
        }, false);
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }

            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                dsm.setLimit(config.isDownloadSpeedLimitEnabled() ? config.getDownloadSpeedLimit() : 0);
            }
        }, false);

        stateMachine = new StateMachine(this, IDLE_STATE, STOPPED_STATE);
        DownloadController.getInstance().addListener(this);
        ShutdownController.getInstance().addShutdownVetoListener(this);
        FileCreationManager.getInstance().getEventSender().addListener(this);
    }

    /**
     * registers the given SingleDownloadController in this DownloadWatchDog
     * 
     * 
     * @param con
     */
    protected void registerSingleDownloadController(final SingleDownloadController con) {
        synchronized (this.downloadControllers) {
            if (this.downloadControllers.contains(con)) { throw new WTFException("SingleDownloadController already registered"); }
            this.downloadControllers.add(con);
        }
        sessionDownloads.incrementAndGet();
        synchronized (activeDownloadsbyHost) {
            String host = con.getDownloadLink().getHost();
            /* increase active counter for this hoster */
            java.util.List<SingleDownloadController> active = this.activeDownloadsbyHost.get(host);
            if (active == null) {
                active = new ArrayList<SingleDownloadController>();
                this.activeDownloadsbyHost.put(host, active);
            }
            active.add(con);
        }

        synchronized (downloadControlHistory) {
            /* update downloadControlHistory */
            DownloadControlHistory history = downloadControlHistory.get(con.getDownloadLink());
            if (history == null) {
                history = new DownloadControlHistory();
                downloadControlHistory.put(con.getDownloadLink(), history);
            }
            DownloadControlHistoryItem info = history.accountUsageHistory.get(con.getAccount());
            if (info == null) {
                info = new DownloadControlHistoryItem();
                history.accountUsageHistory.put(con.getAccount(), info);
            }
            info.timeStamp = System.currentTimeMillis();
            con.getDownloadLink().getLinkStatus().setRetryCount(info.round);
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
            final java.util.List<SingleDownloadController> ret = activeDownloadsbyHost.get(host);
            if (ret != null) return ret.size();
        }
        return 0;
    }

    /**
     * checks if there is enough diskspace left to use given amount of diskspace, only works with java >=1.6
     * 
     * @param dlLink
     * @return
     */
    public synchronized DISKSPACECHECK checkFreeDiskSpace(File file2Root, long diskspace) {
        if (Application.getJavaVersion() < Application.JAVA16) {
            /*
             * File.getUsableSpace is 1.6 only
             */
            return DISKSPACECHECK.UNKNOWN;
        }
        if (!config.isFreeSpaceCheckEnabled()) return DISKSPACECHECK.UNKNOWN;
        /* Set 500MB(default) extra Buffer */
        long spaceneeded = 1024l * 1024 * Math.max(0, config.getForcedFreeSpaceOnDisk());
        /* this HashSet contains all Path-parts of the File we want to download */
        File freeSpace = null;
        java.util.List<String> pathes = new ArrayList<String>();
        if (file2Root != null && file2Root.isFile()) {
            file2Root = file2Root.getParentFile();
        }
        if (file2Root != null) pathes.add(file2Root.getAbsolutePath().toLowerCase(Locale.ENGLISH));
        while (file2Root != null) {
            if (file2Root.exists() && freeSpace == null) {
                freeSpace = file2Root;
                if (freeSpace.getUsableSpace() < (spaceneeded + diskspace)) { return DISKSPACECHECK.FAILED; }
            }
            file2Root = file2Root.getParentFile();
            if (file2Root != null) pathes.add(file2Root.getAbsolutePath().toLowerCase(Locale.ENGLISH));
        }
        if (freeSpace == null) { return DISKSPACECHECK.INVALIDFOLDER; }
        /* calc the needed space for the current running downloads */
        synchronized (this.downloadControllers) {
            for (final SingleDownloadController con : this.downloadControllers) {
                DownloadLink dlink = con.getDownloadLink();
                String folder = dlink.getFilePackage().getDownloadDirectory();
                if (folder == null) continue;
                folder = folder.toLowerCase(Locale.ENGLISH);
                for (String checkPath : pathes) {
                    /*
                     * now we check if the dlink is download to same folder/partition/drive we want to check available space for
                     */
                    if (folder.startsWith(checkPath)) {
                        /* yes, same folder/partition/drive */
                        spaceneeded += Math.max(0, dlink.getDownloadSize() - dlink.getDownloadCurrent());
                        break;
                    }
                }
            }
        }
        /* enough space for needed diskspace */
        if (freeSpace.getUsableSpace() < (spaceneeded + diskspace)) { return DISKSPACECHECK.FAILED; }
        return DISKSPACECHECK.OK;
    }

    /**
     * reset linkstatus for files where it is usefull and needed
     */
    private void clearDownloadListStatus() {
        /* reset ip waittimes only for local ips */
        ProxyController.getInstance().removeIPBlockTimeout(null, true);
        /* reset temp unavailble times for all ips */
        ProxyController.getInstance().removeHostBlockedTimeout(null, false);
        synchronized (DownloadController.getInstance()) {
            for (final FilePackage filePackage : DownloadController.getInstance().getPackages()) {
                synchronized (filePackage) {
                    for (final DownloadLink link : filePackage.getChildren()) {
                        /*
                         * do not reset if link is offline, finished , already exist or pluginerror (because only plugin updates can fix this)
                         */
                        link.getLinkStatus().resetStatus(LinkStatus.ERROR_FATAL | LinkStatus.ERROR_PLUGIN_DEFECT | LinkStatus.ERROR_ALREADYEXISTS, LinkStatus.ERROR_FILE_NOT_FOUND, LinkStatus.FINISHED);
                    }
                }
            }
        }
    }

    /**
     * unregister the given SingleDownloadController from this DownloadWatchDog
     * 
     * @param con
     */
    protected void unregisterSingleDownloadController(final SingleDownloadController con) {
        try {
            DownloadLink link = con.getDownloadLink();
            synchronized (this.downloadControllers) {
                if (this.downloadControllers.remove(con) == false) { throw new WTFException("SingleDownloadController not registed!"); }
            }
            synchronized (activeDownloadsbyHost) {
                String host = link.getHost();
                /* decrease active counter for this hoster */
                java.util.List<SingleDownloadController> active = this.activeDownloadsbyHost.get(host);
                if (active == null) { throw new WTFException("no SingleDownloadController available for this host"); }
                active.remove(con);
                if (active.size() == 0) {
                    this.activeDownloadsbyHost.remove(host);
                }
            }
            synchronized (downloadControlHistory) {
                /* update downloadControlHistory */
                if (FilePackage.isDefaultFilePackage(link.getFilePackage()) || DownloadController.getInstance() != link.getFilePackage().getControlledBy()) {
                    /*
                     * link is no longer controlled by DownloadController, so we can remove the history
                     */
                    downloadControlHistory.remove(link);
                    return;
                }
                DownloadControlHistory history = downloadControlHistory.get(link);
                if (history == null) { throw new WTFException("no history"); }
                DownloadControlHistoryItem info = history.accountUsageHistory.get(con.getAccount());
                if (info == null) { throw new WTFException("no historyitem"); }
                info.timeStamp = System.currentTimeMillis();
                info.round = link.getLinkStatus().getRetryCount();
            }
        } finally {
            synchronized (WATCHDOGWAITLOOP) {
                WATCHDOGWAITLOOP.incrementAndGet();
                WATCHDOGWAITLOOP.notifyAll();
            }
        }
    }

    public boolean forcedLinksWaiting() {
        return forcedLinks.size() > 0;
    }

    /**
     * try to force a downloadstart, will ignore maxperhost and maxdownloads limits
     */
    public void forceDownload(final List<DownloadLink> linksForce) {
        if (linksForce == null || linksForce.size() == 0) return;
        IOEQ.add(new Runnable() {
            public void run() {
                if (DownloadWatchDog.this.stateMachine.isState(STOPPING_STATE)) {
                    /*
                     * controller will shutdown soon or is paused, so no sense in forcing downloads now
                     */
                    return;
                }
                if (DownloadWatchDog.this.stateMachine.isStartState() || DownloadWatchDog.this.stateMachine.isFinal()) {
                    /*
                     * no downloads are running, so we will force only the selected links to get started by setting stopmark to first forced link
                     */
                    DownloadWatchDog.this.setStopMark(linksForce.get(0));
                    DownloadWatchDog.this.startDownloads();
                }
                /* add links to forcedLinks list */
                synchronized (forcedLinks) {
                    forcedLinks.addAll(linksForce);
                }
                synchronized (WATCHDOGWAITLOOP) {
                    WATCHDOGWAITLOOP.incrementAndGet();
                    WATCHDOGWAITLOOP.notifyAll();
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
        return downloadControllers.size();
    }

    /**
     * returns the ThrottledConnectionManager of this DownloadWatchDog
     * 
     * @return
     */
    public DownloadSpeedManager getDownloadSpeedManager() {
        return dsm;
    }

    /**
     * returns how many downloads were started since in this session
     * 
     * @return
     */
    public int getDownloadssincelastStart() {
        return sessionDownloads.get();
    }

    /**
     * returns DownloadControlInfo for next possible Download
     * 
     * @return
     */
    public DownloadControlInfo getNextDownloadLink(List<DownloadLink> possibleLinks, HashMap<String, java.util.List<Account>> accountCache, HashMap<String, PluginForHost> pluginCache, boolean forceDownload) {
        if (accountCache == null) accountCache = new HashMap<String, java.util.List<Account>>();
        if (pluginCache == null) pluginCache = new HashMap<String, PluginForHost>();
        try {
            retryLoop: while (true) {
                linkLoop: for (DownloadLink nextDownloadLink : possibleLinks) {
                    if (nextDownloadLink.getDefaultPlugin() == null) {
                        /* no plugin available, lets skip the link */
                        continue linkLoop;
                    }
                    if (!forceDownload && !nextDownloadLink.isEnabled()) {
                        /* ONLY when not forced */
                        /* link is disabled, lets skip it */
                        continue linkLoop;
                    }
                    if (nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) || nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.TEMP_IGNORE)) {
                        /*
                         * no plugin available, download not enabled,link temp unavailable
                         */
                        continue linkLoop;
                    }
                    if (nextDownloadLink.getLinkStatus().isPluginActive() || (!nextDownloadLink.getLinkStatus().isStatus(LinkStatus.TODO) && !nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED))) {
                        /* download is already in progress or not todo */
                        continue linkLoop;
                    }
                    if (!forceDownload && activeDownloadsbyHosts(nextDownloadLink.getHost()) >= this.getSimultanDownloadNumPerHost()) {
                        /* ONLY when not forced! */
                        /* max downloads per host reached */
                        continue linkLoop;
                    }
                    java.util.List<Account> usableAccounts = accountCache.get(nextDownloadLink.getHost());
                    if (usableAccounts == null) {
                        usableAccounts = new ArrayList<Account>();
                        if (org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.isEnabled()) {
                            /* Account Handling */
                            /* we first add all possible multihoster */
                            LinkedList<Account> accs = AccountController.getInstance().getMultiHostAccounts(nextDownloadLink.getHost());
                            if (accs != null) usableAccounts.addAll(accs);
                            /* then we add all possible local accounts */
                            accs = AccountController.getInstance().getValidAccounts(nextDownloadLink.getHost());
                            if (accs != null) usableAccounts.addAll(accs);
                        }
                        usableAccounts.add(null);/* no account */
                        accountCache.put(nextDownloadLink.getHost(), usableAccounts);
                    }
                    ProxyInfo proxy = null;
                    boolean byPassMaxSimultanDownload = false;
                    PluginForHost plugin = null;
                    String host = null;
                    boolean freeDownloadAllowed = true;
                    accLoop: for (Account acc : usableAccounts) {
                        if (acc != null && (!acc.isEnabled() || acc.isTempDisabled() || !acc.isValid())) {
                            continue accLoop;
                        }
                        if (acc != null) {
                            /*
                             * we have to check if we can use account in parallel with others
                             */
                            synchronized (downloadControllers) {
                                conLoop: for (SingleDownloadController con : downloadControllers) {
                                    Account conAcc = con.getAccount();
                                    if (conAcc == null) continue conLoop;
                                    freeDownloadAllowed = false;
                                    if (conAcc.getHoster().equalsIgnoreCase(host) && conAcc.isConcurrentUsePossible() == false && acc.isConcurrentUsePossible() == false) {
                                        /*
                                         * there is already another account handling this host and our acc does not allow concurrent use
                                         */
                                        continue accLoop;
                                    }
                                }
                            }
                        } else if (freeDownloadAllowed == false) {
                            /* freeDownload not allowed, because a download with running premium is already running */
                            continue accLoop;
                        }
                        byPassMaxSimultanDownload = false;
                        proxy = null;
                        plugin = null;
                        if (acc == null) {
                            /*
                             * no account in use, so host is taken from downloadlink
                             */
                            host = nextDownloadLink.getHost();
                        } else {
                            /* account in use, so host is taken from account */
                            host = acc.getHoster();
                        }
                        /*
                         * possible account found, lets check if we still can use it
                         */
                        plugin = pluginCache.get(host);
                        if (plugin == null) {
                            plugin = JDUtilities.getPluginForHost(host);
                            pluginCache.put(host, plugin);
                        }
                        if (!plugin.canHandle(nextDownloadLink, acc)) {
                            /* plugin can't download given link with acc */
                            if (acc == null) {
                                /*
                                 * we tried last account and noone could handle this link, so temp ignore it this session
                                 */
                                nextDownloadLink.getLinkStatus().addStatus(LinkStatus.TEMP_IGNORE);
                                nextDownloadLink.getLinkStatus().setValue(LinkStatus.TEMP_IGNORE_REASON_NO_SUITABLE_ACCOUNT_FOUND);
                            }
                            continue accLoop;
                        }
                        if (!plugin.enoughTrafficFor(nextDownloadLink, acc)) {
                            /* not enough traffic to download link with acc */
                            if (acc == null) {
                                /*
                                 * we tried last account and noone could handle this link, so temp ignore it this session
                                 */
                                nextDownloadLink.getLinkStatus().addStatus(LinkStatus.TEMP_IGNORE);
                                nextDownloadLink.getLinkStatus().setValue(LinkStatus.TEMP_IGNORE_REASON_NO_SUITABLE_ACCOUNT_FOUND);
                            }
                            continue accLoop;
                        }
                        synchronized (downloadControlHistory) {
                            DownloadControlHistory history = downloadControlHistory.get(nextDownloadLink);
                            DownloadControlHistoryItem accountHistory = null;
                            if (history != null && (accountHistory = history.accountUsageHistory.get(acc)) != null) {
                                /* account has already been used before */
                                if (accountHistory.round < plugin.getMaxRetries(nextDownloadLink, acc)) {
                                    /* we still can retry */
                                } else {
                                    /*
                                     * max retries reached, we do not use this account
                                     */
                                    Account lastAcc = usableAccounts.get(usableAccounts.size() - 1);
                                    if (freeDownloadAllowed == false) {
                                        lastAcc = usableAccounts.get(usableAccounts.size() - 2);
                                    }
                                    if (acc == lastAcc) {

                                        /*
                                         * we tried every possible account and none is left
                                         */
                                        /*
                                         * we remove downloadControlHistory now and retry again
                                         */
                                        downloadControlHistory.remove(nextDownloadLink);
                                        continue retryLoop;
                                    }
                                    continue accLoop;
                                }
                            } else {
                                /*
                                 * account never got used before, so lets use it now
                                 */
                            }
                        }
                        /*
                         * can we bypass maxDownloads for this link and account
                         */
                        byPassMaxSimultanDownload = plugin.bypassMaxSimultanDownloadNum(nextDownloadLink, acc);
                        /* can we use this account to download the link */
                        proxy = ProxyController.getInstance().getProxyForDownload(plugin, nextDownloadLink, acc, byPassMaxSimultanDownload);
                        if (proxy != null) {
                            /*
                             * we can use the account and proxy to download this link
                             */
                            DownloadControlInfo ret = new DownloadControlInfo();
                            ret.byPassSimultanDownloadNum = byPassMaxSimultanDownload;
                            ret.proxy = proxy;
                            ret.link = nextDownloadLink;
                            ret.account = acc;
                            return ret;
                        }
                    }
                }
                /* we tried every possible link without any success */
                break;
            }
        } catch (final Throwable e) {
            logger.log(e);
        }
        return null;
    }

    /**
     * returns how many downloads are running that may not get interrupted by a reconnect
     * 
     * @return
     */
    public int getForbiddenReconnectDownloadNum() {
        final boolean allowinterrupt = config.isInterruptResumeableDownloadsEnable();

        int ret = 0;
        synchronized (this.downloadControllers) {
            for (final SingleDownloadController con : downloadControllers) {
                DownloadLink link = con.getDownloadLink();
                if (link.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                    DownloadInterface dl = link.getDownloadInstance();
                    if (!(dl != null && dl.isResumable() && allowinterrupt)) ret++;
                }
            }
        }
        return ret;
    }

    /**
     * returns how many concurrent downloads from the same host may run
     * 
     * @return
     */
    public int getSimultanDownloadNumPerHost() {
        int ret = 0;
        if (!org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_DOWNLOADS_PER_HOST_ENABLED.isEnabled() || (ret = config.getMaxSimultaneDownloadsPerHost()) <= 0) { return Integer.MAX_VALUE; }
        return ret;
    }

    /**
     * returns current pause state
     * 
     * @return
     */
    public boolean isPaused() {
        return DownloadWatchDog.this.stateMachine.isState(PAUSE_STATE);
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
    private boolean newDLStartAllowed(boolean forcedLinksWaiting) {
        if (forcedLinksWaiting == false && !DownloadWatchDog.this.stateMachine.isState(DownloadWatchDog.RUNNING_STATE)) {
            /*
             * only allow new downloads in running state
             */
            return false;
        }
        if (ShutdownController.getInstance().isShutDownRequested()) {
            /* shutdown is requested, we do not start new downloads */
            return false;
        }
        if (Reconnecter.getInstance().isReconnectInProgress()) {
            /* reconnect in progress */
            return false;
        }
        if (forcedLinksWaiting == false && org.jdownloader.settings.staticreferences.CFG_GENERAL.AUTO_RECONNECT_ENABLED.isEnabled() && config.isDownloadControllerPrefersReconnectEnabled() && IPController.getInstance().isInvalidated()) {
            /*
             * auto reconnect is enabled and downloads are waiting for reconnect and user set to wait for reconnect
             */
            return false;
        }
        return true;
    }

    /**
     * this keeps track of stopmark in case the link/package got removed from downloadlist
     */
    public void onDownloadControllerEvent(final DownloadControllerEvent event) {
        switch (event.getType()) {
        case REMOVE_CONTENT:
            if (this.currentstopMark == event.getParameter()) {
                /* now the stopmark is hidden */
                this.setStopMark(STOPMARK.HIDDEN);
            } else if (event.getParameter() != null && event.getParameter() instanceof List) {
                List<?> list = (List<?>) event.getParameter();
                for (Object l : list) {
                    if (l == this.currentstopMark) {
                        this.setStopMark(STOPMARK.HIDDEN);
                    }
                }
            }

        }
    }

    /**
     * pauses the DownloadWatchDog
     * 
     * @param value
     */
    private Integer speedLimitBeforePause   = null;
    private Boolean speedLimitedBeforePause = null;
    private Object  shutdownLock            = new Object();

    public void pauseDownloadWatchDog(final boolean value) {
        IOEQ.add(new Runnable() {

            public void run() {
                if (value && !DownloadWatchDog.this.getStateMachine().isState(DownloadWatchDog.RUNNING_STATE)) {
                    /* we can only pause downloads when downloads are running */
                    return;
                }
                try {
                    if (value) {
                        /* set pause settings */
                        speedLimitBeforePause = config.getDownloadSpeedLimit();
                        speedLimitedBeforePause = config.isDownloadSpeedLimitEnabled();
                        config.setDownloadSpeedLimit(config.getPauseSpeed());
                        config.setDownloadSpeedLimitEnabled(true);
                        logger.info("Pause enabled: Reducing downloadspeed to " + config.getPauseSpeed() + " KiB/s");
                        /* pause downloads */
                        DownloadWatchDog.this.getStateMachine().setStatus(PAUSE_STATE);
                    } else {
                        /* revert pause settings if available */
                        if (speedLimitBeforePause != null) {
                            logger.info("Pause disabled: Switch back to old downloadspeed");
                            config.setDownloadSpeedLimit(speedLimitBeforePause);
                        }
                        if (speedLimitedBeforePause != null) config.setDownloadSpeedLimitEnabled(speedLimitedBeforePause);
                        speedLimitBeforePause = null;
                        speedLimitedBeforePause = null;
                        if (DownloadWatchDog.this.getStateMachine().isState(DownloadWatchDog.PAUSE_STATE)) {
                            /* we revert pause to running state */
                            DownloadWatchDog.this.getStateMachine().setStatus(RUNNING_STATE);
                        }
                    }
                } finally {
                    synchronized (WATCHDOGWAITLOOP) {
                        WATCHDOGWAITLOOP.incrementAndGet();
                        WATCHDOGWAITLOOP.notifyAll();
                    }
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
            synchronized (downloadControlHistory) {
                if (downloadControlHistory.get(stop) != null) {
                    /*
                     * we already started this download in current session, so stopmark reached
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
                for (final DownloadLink dl : ((FilePackage) stop).getChildren()) {
                    synchronized (downloadControlHistory) {
                        if (downloadControlHistory.get(dl) != null) {
                            /*
                             * we already started this download in current session, so stopmark reached
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
    public void removeIPBlockTimeout(DownloadLink link) {
        /* reset ip waittimes only for local ip */
        ProxyController.getInstance().removeIPBlockTimeout(link.getHost(), true);
        LinkStatus ls = link.getLinkStatus();
        if (ls.hasStatus(LinkStatus.ERROR_IP_BLOCKED)) {
            ls.reset();
        }
    }

    /**
     * resets TempUnavailWaittime for the given Host
     * 
     * @param host
     */
    public void removeTempUnavailTimeout(DownloadLink link) {
        ProxyController.getInstance().removeHostBlockedTimeout(link.getHost(), false);
        LinkStatus ls = link.getLinkStatus();
        if (ls.hasStatus(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE)) {
            ls.reset();
        }
    }

    public long getDownloadSpeedbyFilePackage(FilePackage pkg) {
        long speed = -1;
        synchronized (downloadControllers) {
            for (SingleDownloadController con : downloadControllers) {
                if (con.getDownloadLink().getFilePackage() != pkg) continue;
                speed += con.getDownloadLink().getDownloadSpeed();
            }
        }
        return speed;
    }

    public int getDownloadsbyFilePackage(FilePackage pkg) {
        int ret = 0;
        synchronized (downloadControllers) {
            for (SingleDownloadController con : downloadControllers) {
                if (con.getDownloadLink().getFilePackage() != pkg) continue;
                ret++;
            }
        }
        return ret;
    }

    public List<DownloadLink> getRunningDownloadLinks() {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        synchronized (downloadControllers) {
            for (SingleDownloadController con : downloadControllers) {
                ret.add(con.getDownloadLink());
            }
        }
        return ret;
    }

    public List<FilePackage> getRunningFilePackages() {
        HashSet<FilePackage> ret = new HashSet<FilePackage>();
        synchronized (downloadControllers) {
            for (SingleDownloadController con : downloadControllers) {
                ret.add(con.getDownloadLink().getParentNode());
            }
        }
        return new ArrayList<FilePackage>(ret);
    }

    public boolean hasRunningDownloads(FilePackage pkg) {
        synchronized (downloadControllers) {
            for (SingleDownloadController con : downloadControllers) {
                if (con.getDownloadLink().getFilePackage() == pkg) return true;
            }
        }
        return false;
    }

    /**
     * aborts all running SingleDownloadControllers, NOTE: DownloadWatchDog is still running, new Downloads will can started after this call
     */
    public void abortAllSingleDownloadControllers() {

        while (true) {
            java.util.List<SingleDownloadController> list = new ArrayList<SingleDownloadController>();
            synchronized (downloadControllers) {
                list.addAll(downloadControllers);
            }
            for (SingleDownloadController con : list) {
                if (con.isAlive() && !con.isAborted()) {
                    con.abortDownload();
                }
            }
            boolean alive = false;
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
                logger.log(e);
                return;
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
                con.getStateMachine().executeOnceOnState(new Runnable() {

                    public void run() {
                        /* reset waittimes when controller reached final state */
                        removeIPBlockTimeout(link);
                        removeTempUnavailTimeout(link);
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
     * activates new Downloads as long as possible and returns how many got activated
     * 
     * @return
     **/
    private int setDownloadActive(List<DownloadLink> possibleLinks) {
        DownloadControlInfo dci = null;
        int ret = 0;
        int maxDownloads = config.getMaxSimultaneDownloads();
        int maxLoops = possibleLinks.size();
        HashMap<String, java.util.List<Account>> accountCache = new HashMap<String, java.util.List<Account>>();
        HashMap<String, PluginForHost> pluginCache = new HashMap<String, PluginForHost>();
        java.util.List<DownloadLink> forcedLink = new ArrayList<DownloadLink>(1);
        startLoop: while (this.forcedLinksWaiting() || ((getActiveDownloads() < maxDownloads) && maxLoops >= 0)) {
            if (!this.newDLStartAllowed(forcedLinksWaiting()) || this.isStopMarkReached()) {
                break;
            }
            forcedLink.clear();
            synchronized (forcedLinks) {
                if (forcedLinks.size() > 0) {
                    /*
                     * we remove the first one of forcedLinks list into local array which holds only 1 downloadLink
                     */
                    forcedLink.add(forcedLinks.removeFirst());
                }
            }
            if (forcedLink.size() > 0) {
                /* we try to force the link in forcedLink array */
                dci = this.getNextDownloadLink(forcedLink, accountCache, pluginCache, true);
                if (dci == null) {
                    /* we could not start the forced link */
                    continue startLoop;
                }
            } else {
                /* we try to find next possible normal download */
                dci = this.getNextDownloadLink(possibleLinks, accountCache, pluginCache, false);
                if (dci == null) {
                    /* no next possible download found */
                    break;
                }
            }
            DownloadLink dlLink = dci.link;
            String dlFolder = dlLink.getFilePackage().getDownloadDirectory();
            DISKSPACECHECK check = this.checkFreeDiskSpace(new File(dlFolder), (dlLink.getDownloadSize() - dlLink.getDownloadCurrent()));
            synchronized (shutdownLock) {
                if (!ShutdownController.getInstance().isShutDownRequested()) {
                    switch (check) {
                    case OK:
                    case UNKNOWN:
                        logger.info("Start " + dci);
                        this.activateSingleDownloadController(dci);
                        ret++;
                        break;
                    case FAILED:
                        logger.info("Could not start " + dci + ": not enough diskspace free in " + dlFolder);
                        dci.link.getLinkStatus().setStatus(LinkStatus.TEMP_IGNORE);
                        dci.link.getLinkStatus().setValue(LinkStatus.TEMP_IGNORE_REASON_NOT_ENOUGH_HARDDISK_SPACE);
                        break;
                    case INVALIDFOLDER:
                        logger.info("Could not start " + dci + ": invalid downloadfolder->" + dlFolder);
                        dci.link.getLinkStatus().setStatus(LinkStatus.ERROR_FATAL);
                        dci.link.getLinkStatus().setErrorMessage(_JDT._.downloadlink_status_error_invalid_dest());

                        // dci.link.getLinkStatus().setValue(LinkStatus.TEMP_IGNORE_REASON_INVALID_DOWNLOAD_DESTINATION);
                        break;
                    }
                }
            }
            maxLoops--;
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
                    synchronized (DownloadWatchDog.this.downloadControllers) {
                        if (DownloadWatchDog.this.downloadControllers.size() > 0) {
                            /* use first running download */
                            entry = DownloadWatchDog.this.downloadControllers.getFirst().getDownloadLink();
                        } else {
                            /*
                             * no running download available, set stopmark to none
                             */
                            entry = STOPMARK.NONE;
                        }
                    }
                }
                DownloadWatchDog.this.currentstopMark = entry;
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
                    DownloadWatchDog.this.stateMachine.reset(false);
                }
                if (!DownloadWatchDog.this.stateMachine.isStartState()) {
                    /* only allow to start when in FinalState(NOT_RUNNING) */
                    return;
                }
                /* new download session, we allow all captchas */
                setCaptchaAllowed(null, CAPTCHA.OK);
                /* set state to running */
                stateMachine.setStatus(RUNNING_STATE);
                /* remove stopsign if it is reached */
                if (isStopMarkReached()) {
                    setStopMark(STOPMARK.NONE);
                }
                /* reset downloadControlHistory */
                synchronized (downloadControlHistory) {
                    downloadControlHistory.clear();
                }
                sessionDownloads.set(0);
                /* throw start event */
                logger.info("DownloadWatchDog: start");
                /* start watchdogthread */
                startWatchDogThread();
            }
        }, true);
    }

    /**
     * activates a new SingleDownloadController for the given DownloadControlInfo
     * 
     * @param dci
     */
    private void activateSingleDownloadController(final DownloadControlInfo dci) {
        logger.info("Start new Download: Host:" + dci.link.getHost() + "|:Name" + dci.link.getName() + "|Proxy:" + dci.proxy);
        /* we enable link in case it's a forced disabled link */
        dci.link.setEnabled(true);
        final SingleDownloadController download = new SingleDownloadController(dci.link, dci.account, dci.proxy, this.dsm);
        if (dci.byPassSimultanDownloadNum == false && dci.proxy != null) {
            /*
             * increase Counter for this Host only in case it is handled by normal MaxSimultanDownload handling
             */
            dci.proxy.increaseActiveDownloads(dci.link.getHost());
        }
        download.setIOPermission(this);
        download.getStateMachine().executeOnceOnState(new Runnable() {

            public void run() {
                if (dci.byPassSimultanDownloadNum == false && dci.proxy != null) {
                    dci.proxy.decreaseActiveDownloads(dci.link.getHost());
                }
                unregisterSingleDownloadController(download);
            }

        }, SingleDownloadController.FINAL_STATE);
        registerSingleDownloadController(download);
        download.start();
    }

    private void startWatchDogThread() {
        synchronized (this) {
            if (this.watchDogThread == null || !this.watchDogThread.isAlive()) {
                /**
                 * Workaround, due to activeDownloads bug.
                 */
                this.watchDogThread = new Thread() {
                    @Override
                    public void run() {
                        this.setName("DownloadWatchDog");
                        try {
                            List<DownloadLink> links = new ArrayList<DownloadLink>();
                            LinkStatus linkStatus;
                            boolean hasInProgressLinks;
                            boolean hasTempDisabledLinks;
                            boolean waitingNewIP;
                            boolean resetWaitingNewIP;
                            int stopCounter = 2;
                            long lastStructureChange = -1;
                            long lastContentChange = -1;
                            long lastReconnectCounter = -1;
                            while (DownloadWatchDog.this.stateMachine.isState(DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.PAUSE_STATE)) {
                                /*
                                 * start new download while we are in running state
                                 */
                                hasInProgressLinks = false;
                                hasTempDisabledLinks = false;
                                waitingNewIP = false;
                                resetWaitingNewIP = false;
                                if (lastReconnectCounter < Reconnecter.getReconnectCounter()) {
                                    /* an IP-change happend, reset waittimes */
                                    lastReconnectCounter = Reconnecter.getReconnectCounter();
                                    ProxyController.getInstance().removeIPBlockTimeout(null, true);
                                    resetWaitingNewIP = true;
                                }
                                final boolean readL = DownloadController.getInstance().readLock();
                                try {
                                    long currentStructure = DownloadController.getInstance().getPackageControllerChanges();
                                    long currentContent = DownloadController.getInstance().getContentChanges();
                                    if (currentStructure != lastStructureChange || currentContent != lastContentChange) {
                                        /*
                                         * create a map holding all possible links sorted by their position in list and their priority
                                         * 
                                         * by doing this we don't have to walk through possible links multiple times to find next download link, as the list
                                         * itself will already be correct sorted
                                         */
                                        HashMap<Long, java.util.List<DownloadLink>> optimizedList = new HashMap<Long, java.util.List<DownloadLink>>();
                                        /*
                                         * changes in DownloadController available, refresh DownloadList
                                         */
                                        for (FilePackage fp : DownloadController.getInstance().getPackages()) {
                                            synchronized (fp) {
                                                for (DownloadLink fpLink : fp.getChildren()) {
                                                    if (fpLink.getDefaultPlugin() == null || !fpLink.isEnabled() || (fpLink.getAvailableStatus() == AvailableStatus.FALSE) || fpLink.getLinkStatus().isFinished() || fpLink.getLinkStatus().hasStatus(LinkStatus.TEMP_IGNORE)) continue;
                                                    long prio = fpLink.getPriority();
                                                    java.util.List<DownloadLink> list = optimizedList.get(prio);
                                                    if (list == null) {
                                                        list = new ArrayList<DownloadLink>();
                                                        optimizedList.put(prio, list);
                                                    }
                                                    list.add(fpLink);
                                                }
                                            }
                                        }
                                        links.clear();
                                        /*
                                         * move optimizedList to list in a sorted way
                                         */
                                        while (!optimizedList.isEmpty()) {
                                            /*
                                             * find next highest priority and add the links
                                             */
                                            Long highest = Collections.max(optimizedList.keySet());
                                            java.util.List<DownloadLink> ret = optimizedList.remove(highest);
                                            if (ret != null) links.addAll(ret);
                                        }
                                        lastStructureChange = currentStructure;
                                        lastContentChange = currentContent;
                                    }
                                } finally {
                                    DownloadController.getInstance().readUnlock(readL);
                                }
                                try {
                                    for (DownloadLink link : links) {
                                        linkStatus = link.getLinkStatus();
                                        if (link.isEnabled()) {
                                            if (!linkStatus.isPluginActive()) {
                                                /* enabled and not in progress */
                                                if (linkStatus.hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) || linkStatus.hasStatus(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE)) {
                                                    /*
                                                     * download or hoster temp. unavail
                                                     */
                                                    if (linkStatus.getRemainingWaittime() == 0) {
                                                        /*
                                                         * reset if waittime is over
                                                         */
                                                        linkStatus.reset(false);
                                                    } else {
                                                        /*
                                                         * we have temp. unavail links in list
                                                         */
                                                        hasTempDisabledLinks = true;
                                                    }
                                                } else if (linkStatus.hasStatus(LinkStatus.ERROR_IP_BLOCKED)) {
                                                    /* ip blocked link */
                                                    if (linkStatus.getRemainingWaittime() == 0 || resetWaitingNewIP) {
                                                        /*
                                                         * reset if waittime is over
                                                         */
                                                        linkStatus.reset();
                                                        /*
                                                         * clear blocked accounts for this host
                                                         */
                                                    } else if (!resetWaitingNewIP) {
                                                        /*
                                                         * we request a reconnect if possible
                                                         */
                                                        if (DownloadWatchDog.this.activeDownloadsbyHosts(link.getHost()) == 0) {
                                                            /*
                                                             * do not reconnect if the request comes from host with active downloads, this will prevent
                                                             * reconnect loops for plugins that allow resume and parallel downloads
                                                             */
                                                            waitingNewIP = true;
                                                            IPController.getInstance().invalidate();
                                                        }
                                                    }
                                                } else if (ProxyController.getInstance().hasHostBlocked(link.getHost()) && !link.getLinkStatus().isFinished()) {
                                                    /*
                                                     * we have links that are temp. unavail in list
                                                     */
                                                    hasTempDisabledLinks = true;
                                                } else if (ProxyController.getInstance().hasIPBlock(link.getHost()) && !link.getLinkStatus().isFinished()) {
                                                    /*
                                                     * we have links that are ipblocked in list
                                                     */
                                                    if (DownloadWatchDog.this.activeDownloadsbyHosts(link.getHost()) == 0) {
                                                        /*
                                                         * do not reconnect if the request comes from host with active downloads, this will prevent reconnect
                                                         * loops for plugins that allow resume and parallel downloads
                                                         */
                                                        waitingNewIP = true;
                                                        IPController.getInstance().invalidate();
                                                    }
                                                }
                                            } else {
                                                /* we have active links in list */
                                                hasInProgressLinks = true;
                                            }
                                        }
                                    }
                                    /* request a reconnect if allowed and needed */
                                    Reconnecter.getInstance().run();
                                    int ret = DownloadWatchDog.this.setDownloadActive(links);
                                    if (ret == 0) {
                                        /*
                                         * no new download got started, check what happened and what to do next
                                         */
                                        if (!hasTempDisabledLinks && !hasInProgressLinks && !waitingNewIP && DownloadWatchDog.this.getActiveDownloads() == 0) {
                                            /*
                                             * no tempdisabled, no in progress, no reconnect and no next download waiting and no active downloads
                                             */
                                            if (DownloadWatchDog.this.newDLStartAllowed(forcedLinksWaiting())) {
                                                /*
                                                 * only start countdown to stop downloads if we were allowed to start new ones
                                                 */
                                                stopCounter--;
                                                logger.info(stopCounter + "rounds left to start new downloads");
                                            }
                                            if (stopCounter == 0) {
                                                /*
                                                 * countdown reached, prepare to stop downloadwatchdog
                                                 */
                                                break;
                                            }
                                        }
                                    } else {
                                        /*
                                         * reset countdown, because we new downloads got started
                                         */
                                        stopCounter = 2;
                                    }
                                } catch (final Exception e) {
                                    logger.log(e);
                                }
                                try {
                                    int round = 0;
                                    long currentWatchDogLoop = WATCHDOGWAITLOOP.get();
                                    while (DownloadWatchDog.this.stateMachine.isState(DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.PAUSE_STATE)) {
                                        if (++round == 5 || links.size() == 0 || currentWatchDogLoop != WATCHDOGWAITLOOP.get()) break;
                                        synchronized (WATCHDOGWAITLOOP) {
                                            WATCHDOGWAITLOOP.wait(1000);
                                        }
                                    }
                                } catch (final InterruptedException e) {
                                }

                            }
                            stateMachine.setStatus(STOPPING_STATE);
                            logger.info("DownloadWatchDog: stopping");
                            /* stop all remaining downloads */
                            abortAllSingleDownloadControllers();
                            /* clear Status */
                            clearDownloadListStatus();
                            /* clear blocked Accounts */
                            AccountController.getInstance().removeAccountBlocked(null);
                            /* unpause downloads */
                            pauseDownloadWatchDog(false);
                            if (isStopMarkReached()) {
                                /* remove stopsign if it has been reached */
                                setStopMark(STOPMARK.NONE);
                            }
                        } catch (final Throwable e) {
                            logger.log(e);
                            stateMachine.setStatus(STOPPING_STATE);
                        } finally {
                            /* full stop reached */
                            logger.info("DownloadWatchDog: stopped");
                            synchronized (DownloadWatchDog.this) {
                                watchDogThread = null;
                            }
                            /* clear downloadControlHistory */
                            synchronized (downloadControlHistory) {
                                downloadControlHistory.clear();
                            }
                            /* clear forcedLinks list */
                            synchronized (forcedLinks) {
                                forcedLinks.clear();
                            }
                            stateMachine.setStatus(STOPPED_STATE);
                        }
                    }
                };
                this.watchDogThread.start();
            }
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
                    DownloadsTableModel.getInstance().setStopSignColumnVisible(true);
                }
            }
        }, true);
    }

    public StateMachine getStateMachine() {
        return this.stateMachine;
    }

    public void onShutdown(boolean silent) {
        IOEQ.getQueue().addWait(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                if (stateMachine.isState(RUNNING_STATE, PAUSE_STATE, STOPPING_STATE)) {
                    config.setClosedWithRunningDownloads(true);
                } else {
                    config.setClosedWithRunningDownloads(false);
                }
                stopDownloads();
                return null;
            }

            @Override
            protected boolean allowAsync() {
                return false;
            }
        });
    }

    public synchronized boolean isCaptchaAllowed(String hoster) {
        if (captchaBlockedHoster.contains(null)) return false;
        return !captchaBlockedHoster.contains(hoster);

    }

    public synchronized void setCaptchaAllowed(String hoster, CAPTCHA mode) {
        switch (mode) {
        case OK:
            if (hoster != null && hoster.length() > 0) {
                captchaBlockedHoster.remove(hoster);
            } else {
                captchaBlockedHoster.clear();
            }
            break;
        case BLOCKALL:
            captchaBlockedHoster.add(null);
            break;
        case BLOCKHOSTER:
            captchaBlockedHoster.add(hoster);
            break;
        }
    }

    public void onNewFile(Object obj, final File[] list) {
        if (JsonConfig.create(GeneralSettings.class).isAutoOpenContainerAfterDownload()) {
            /* check if extracted files are container files */

            new Thread() {

                @Override
                public void run() {
                    StringBuilder sb = new StringBuilder();
                    for (final File file : list) {
                        if (sb.length() > 0) {
                            sb.append("\r\n");
                        }
                        sb.append("file://");
                        sb.append(file.getPath());
                    }
                    LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(sb.toString()));
                }

            }.start();
        }
    }

    public static boolean preDownloadCheckFailed(DownloadLink link) {
        if (!link.isAvailabilityStatusChecked() && link.getForcedFileName() == null) {
            /*
             * dont proceed if no linkcheck has done yet, maybe we dont know filename yet
             */
            return false;
        }
        DownloadLink downloadLink = link;
        DownloadLink block = DownloadController.getInstance().getFirstLinkThatBlocks(downloadLink);
        LinkStatus linkstatus = link.getLinkStatus();
        if (block != null) {
            linkstatus.addStatus(LinkStatus.ERROR_ALREADYEXISTS);
            if (block.getDefaultPlugin() != null) linkstatus.setStatusText(_JDT._.system_download_errors_linkisBlocked(block.getHost()));
            return true;
        }
        File fileOutput = new File(downloadLink.getFileOutput());
        if (fileOutput.getParentFile() == null) {
            linkstatus.addStatus(LinkStatus.ERROR_FATAL);
            linkstatus.setErrorMessage(_JDT._.system_download_errors_invalidoutputfile());
            return true;
        }
        if (fileOutput.isDirectory()) return false;
        if (!fileOutput.getParentFile().exists()) {
            if (!fileOutput.getParentFile().mkdirs()) {
                linkstatus.addStatus(LinkStatus.ERROR_FATAL);
                linkstatus.setErrorMessage(_JDT._.system_download_errors_invalidoutputfile());
                return true;
            }
        }
        if (fileOutput.exists()) {
            // TODO: handle all options!?
            if (JsonConfig.create(GeneralSettings.class).getIfFileExistsAction() == IfFileExistsAction.OVERWRITE_FILE) {
                if (!new File(downloadLink.getFileOutput()).delete()) {
                    linkstatus.addStatus(LinkStatus.ERROR_FATAL);
                    linkstatus.setErrorMessage(_JDT._.system_download_errors_couldnotoverwrite());
                    return true;
                }
            } else {
                linkstatus.addStatus(LinkStatus.ERROR_ALREADYEXISTS);
                linkstatus.setErrorMessage(_JDT._.downloadlink_status_error_file_exists());
                return true;
            }
        }
        return false;
    }

    @Override
    public void onSilentShutdownVetoRequest(ShutdownVetoException[] vetos) throws ShutdownVetoException {
        if (vetos.length > 0) {
            /* we already abort shutdown, no need to ask again */
            /* no need to increment the shutdownRequests because it wont happen */
            return;
        }
        synchronized (this.shutdownLock) {
            /*
             * we sync to make sure that no new downloads get started meanwhile
             */
            if (this.stateMachine.isState(RUNNING_STATE, PAUSE_STATE, STOPPING_STATE)) {
                synchronized (this.downloadControllers) {
                    for (final SingleDownloadController con : downloadControllers) {
                        DownloadLink link = con.getDownloadLink();
                        if (con.getAccount() == null) { throw new ShutdownVetoException("DownloadWatchDog is still running: no account", this); }
                        if (link.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                            DownloadInterface dl = link.getDownloadInstance();
                            if (dl != null && !dl.isResumable()) { throw new ShutdownVetoException("DownloadWatchDog is still running: non resumeable", this); }
                        }
                    }
                }

            }

        }
    }

    @Override
    public void onShutdownVetoRequest(ShutdownVetoException[] shutdownVetoExceptions) throws ShutdownVetoException {
        if (shutdownVetoExceptions.length > 0) {
            /* we already abort shutdown, no need to ask again */
            /* no need to increment the shutdownRequests because it wont happen */
            /*
             * we need this ShutdownVetoException here to avoid count issues with shutdownRequests
             */
            throw new ShutdownVetoException("Shutdown already cancelled!", this);
        }
        synchronized (this.shutdownLock) {
            /*
             * we sync on shutdownRequests to make sure that no new downloads get started meanwhile
             */
            if (this.stateMachine.isState(RUNNING_STATE, PAUSE_STATE, STOPPING_STATE)) {
                String dialogTitle = _JDT._.DownloadWatchDog_onShutdownRequest_();
                synchronized (this.downloadControllers) {
                    for (final SingleDownloadController con : downloadControllers) {
                        DownloadLink link = con.getDownloadLink();
                        if (link.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                            DownloadInterface dl = link.getDownloadInstance();
                            if (dl != null && !dl.isResumable()) {
                                dialogTitle = _JDT._.DownloadWatchDog_onShutdownRequest_nonresumable();
                                break;
                            }
                        }
                    }
                }

                try {
                    NewUIO.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | Dialog.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, dialogTitle, _JDT._.DownloadWatchDog_onShutdownRequest_msg(), NewTheme.I().getIcon("download", 32), _JDT._.literally_yes(), null);
                    /* downloadWatchDog was running */

                    return;
                } catch (DialogNoAnswerException e) {
                }
                throw new ShutdownVetoException("DownloadWatchDog is still running", this);
            } else {
                /* downloadWatchDog was not running */

            }
        }
    }

    @Override
    public void onShutdownVeto(ShutdownVetoException[] shutdownVetoExceptions) {

    }

    @Override
    public void onRemoveFile(Object caller, File[] fileList) {
    }

    @Override
    public long getShutdownVetoPriority() {
        return 0;
    }

    public int getNonResumableRunningCount() {
        int i = 0;

        if (this.stateMachine.isState(RUNNING_STATE, PAUSE_STATE, STOPPING_STATE)) {

            synchronized (this.downloadControllers) {
                for (final SingleDownloadController con : downloadControllers) {
                    DownloadLink link = con.getDownloadLink();
                    if (link.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                        DownloadInterface dl = link.getDownloadInstance();
                        if (dl != null && !dl.isResumable()) {
                            i++;
                        }
                    }
                }
            }
        }
        return i;

    }

    public long getNonResumableBytes() {
        long i = 0;

        if (this.stateMachine.isState(RUNNING_STATE, PAUSE_STATE, STOPPING_STATE)) {
            synchronized (this.downloadControllers) {
                for (final SingleDownloadController con : downloadControllers) {
                    DownloadLink link = con.getDownloadLink();
                    if (link.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                        DownloadInterface dl = link.getDownloadInstance();
                        if (dl != null && !dl.isResumable()) {
                            i += link.getDownloadCurrent();
                        }
                    }
                }
            }
        }
        return i;
    }

}