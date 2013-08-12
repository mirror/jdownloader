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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.controlling.TaskQueue;
import jd.controlling.downloadcontroller.DownloadSession.STOPMARK;
import jd.controlling.downloadcontroller.event.DownloadWatchdogEvent;
import jd.controlling.downloadcontroller.event.DownloadWatchdogEventSender;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.ProxyInfo;
import jd.controlling.reconnect.Reconnecter;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.plugins.DeleteTo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

import org.appwork.controlling.State;
import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.controlling.StateMachine;
import org.appwork.controlling.StateMachineInterface;
import org.appwork.exceptions.WTFException;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.controlling.DownloadLinkWalker;
import org.jdownloader.controlling.FileCreationListener;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.IfFileExistsAction;
import org.jdownloader.settings.SilentModeSettings.CaptchaDuringSilentModeAction;
import org.jdownloader.settings.staticreferences.CFG_RECONNECT;
import org.jdownloader.settings.staticreferences.CFG_SILENTMODE;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.UpdateController;
import org.jdownloader.utils.JDFileUtils;

public class DownloadWatchDog implements DownloadControllerListener, StateMachineInterface, ShutdownVetoListener, FileCreationListener {

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

    public static enum DISKSPACECHECK {
        UNKNOWN,
        OK,
        INVALIDFOLDER,
        FAILED
    }

    private final NullsafeAtomicReference<Thread>          currentWatchDogThread = new NullsafeAtomicReference<Thread>(null);
    private final LinkedBlockingDeque<DownloadWatchDogJob> watchDogJobs          = new LinkedBlockingDeque<DownloadWatchDogJob>();

    private final StateMachine                             stateMachine;
    private final DownloadSpeedManager                     dsm;

    private final GeneralSettings                          config;

    private final static DownloadWatchDog                  INSTANCE              = new DownloadWatchDog();

    private final LogSource                                logger;
    private final AtomicLong                               WATCHDOGWAITLOOP      = new AtomicLong(0);
    private final DownloadWatchdogEventSender              eventSender;

    public DownloadWatchdogEventSender getEventSender() {
        return eventSender;
    }

    public static DownloadWatchDog getInstance() {
        return INSTANCE;
    }

    public void wakeUpWatchDog(boolean notify) {
        synchronized (WATCHDOGWAITLOOP) {
            WATCHDOGWAITLOOP.incrementAndGet();
            if (notify) WATCHDOGWAITLOOP.notifyAll();
        }
    }

    private DownloadWatchDog() {
        logger = LogController.CL();
        config = JsonConfig.create(GeneralSettings.class);
        eventSender = new DownloadWatchdogEventSender();
        session.set(new DownloadSession());
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
        startDownloadJobExecuter();
        stateMachine.addListener(new StateEventListener() {

            @Override
            public void onStateUpdate(StateEvent event) {
            }

            @Override
            public void onStateChange(StateEvent event) {
                if (event.getNewState() == RUNNING_STATE) {
                    eventSender.fireEvent(new DownloadWatchdogEvent(this, DownloadWatchdogEvent.Type.STATE_RUNNING));
                } else if (event.getNewState() == STOPPED_STATE) {
                    eventSender.fireEvent(new DownloadWatchdogEvent(this, DownloadWatchdogEvent.Type.STATE_STOPPED));
                } else if (event.getNewState() == IDLE_STATE) {
                    eventSender.fireEvent(new DownloadWatchdogEvent(this, DownloadWatchdogEvent.Type.STATE_IDLE));
                } else if (event.getNewState() == PAUSE_STATE) {
                    eventSender.fireEvent(new DownloadWatchdogEvent(this, DownloadWatchdogEvent.Type.STATE_PAUSE));
                } else if (event.getNewState() == STOPPING_STATE) {
                    eventSender.fireEvent(new DownloadWatchdogEvent(this, DownloadWatchdogEvent.Type.STATE_STOPPING));
                }
            };
        });
        DownloadController.getInstance().addListener(this);
        ShutdownController.getInstance().addShutdownVetoListener(this);
        FileCreationManager.getInstance().getEventSender().addListener(this);
    }

    public DISKSPACECHECK checkFreeDiskSpace(final File file2Root, final SingleDownloadController controller, final long diskspace) throws Exception {
        if (Thread.currentThread() == currentWatchDogThread.get()) {
            return diskSpaceCheck(file2Root, controller, diskspace);
        } else {
            final NullsafeAtomicReference<Object> asyncResult = new NullsafeAtomicReference<Object>(null);
            enqueueJob(new DownloadWatchDogJob() {

                @Override
                public void execute(List<DownloadLink> downloadWatchDogLoopLinks) {
                    try {
                        DISKSPACECHECK result = diskSpaceCheck(file2Root, controller, diskspace);
                        synchronized (asyncResult) {
                            asyncResult.set(result);
                            asyncResult.notifyAll();
                        }
                    } catch (final Exception e) {
                        logger.log(e);
                        synchronized (asyncResult) {
                            asyncResult.set(e);
                            asyncResult.notifyAll();
                        }
                    }
                }
            });
            Object ret = null;
            while (true) {
                synchronized (asyncResult) {
                    ret = asyncResult.get();
                    if (ret != null) break;
                    asyncResult.wait();
                }
            }
            if (ret instanceof DISKSPACECHECK) return (DISKSPACECHECK) ret;
            if (ret instanceof Exception) throw (Exception) ret;
            throw new WTFException("WTF? Result: " + ret);
        }
    }

    /**
     * checks if there is enough diskspace left to use given amount of diskspace, only works with java >=1.6
     * 
     * @param dlLink
     * @return
     */
    private DISKSPACECHECK diskSpaceCheck(File file2Root, SingleDownloadController controller, long diskspace) {
        if (!config.isFreeSpaceCheckEnabled()) return DISKSPACECHECK.UNKNOWN;
        if (Application.getJavaVersion() < Application.JAVA16) {
            /*
             * File.getUsableSpace is 1.6 only
             */
            return DISKSPACECHECK.UNKNOWN;
        }
        diskspace = Math.max(0, diskspace);
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
        for (final SingleDownloadController con : getSession().getControllers()) {
            if (con == controller) continue;
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
        for (final DownloadLink link : DownloadController.getInstance().getAllChildren()) {
            /*
             * do not reset if link is offline, finished , already exist or pluginerror (because only plugin updates can fix this)
             */
            link.getLinkStatus().resetStatus(LinkStatus.ERROR_FATAL | LinkStatus.ERROR_PLUGIN_DEFECT | LinkStatus.ERROR_ALREADYEXISTS, LinkStatus.ERROR_FILE_NOT_FOUND, LinkStatus.FINISHED);
        }
    }

    /**
     * try to force a downloadstart, will ignore maxperhost and maxdownloads limits
     */
    public void forceDownload(final List<DownloadLink> linksForce) {
        if (linksForce == null || linksForce.size() == 0) return;
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                if (DownloadWatchDog.this.stateMachine.isState(STOPPING_STATE)) {
                    /*
                     * controller will shutdown soon or is paused, so no sense in forcing downloads now
                     */
                    return null;
                }
                for (DownloadLink l : linksForce) {
                    l.setSkipReason(null);
                }

                if (DownloadWatchDog.this.stateMachine.isStartState() || DownloadWatchDog.this.stateMachine.isFinal()) {
                    /*
                     * no downloads are running, so we will force only the selected links to get started by setting stopmark to first forced link
                     */

                    // DownloadWatchDog.this.setStopMark(linksForce.get(0));
                    DownloadWatchDog.this.startDownloads(new Runnable() {

                        @Override
                        public void run() {
                            getSession().getStopAfterForcedLinks().set(true);
                            getSession().getForcedLinks().addAllAbsent(linksForce);
                        }
                    });
                } else {
                    getSession().getForcedLinks().addAllAbsent(linksForce);
                }
                wakeUpWatchDog(true);
                return null;
            }
        });
    }

    /**
     * returns how many downloads are currently watched by this DownloadWatchDog
     * 
     * @return
     */
    public int getActiveDownloads() {
        return getSession().getControllers().size();
    }

    /**
     * returns the ThrottledConnectionManager of this DownloadWatchDog
     * 
     * @return
     */
    public DownloadSpeedManager getDownloadSpeedManager() {
        return dsm;
    }

    public File validateDestination(File file) {
        if (!file.exists()) {
            File checking = null;
            try {
                String[] folders;
                switch (CrossSystem.getOSFamily()) {
                case LINUX:
                    folders = CrossSystem.getPathComponents(file);
                    if (folders.length >= 3) {
                        if ("media".equals(folders[1])) {
                            /* 0:/ | 1:media | 2:mounted volume */
                            checking = new File("/media/" + folders[1]);
                        }
                    }
                    break;
                case MAC:
                    folders = CrossSystem.getPathComponents(file);
                    if (folders.length >= 3) {
                        if ("media".equals(folders[1])) {
                            /* 0:/ | 1:media | 2:mounted volume */
                            checking = new File("/media/" + folders[1]);
                        } else if ("Volumes".equals(folders[1])) {
                            /* 0:/ | 1:Volumes | 2:mounted volume */
                            checking = new File("/Volumes/" + folders[1]);
                        }
                    }
                    break;
                case WINDOWS:
                    folders = CrossSystem.getPathComponents(file);
                    if (folders.length > 0) {
                        checking = new File(folders[0]);
                    }
                }
                if (checking != null && checking.exists() && checking.isDirectory()) checking = null;
            } catch (IOException e) {
                logger.log(e);
            }
            if (checking != null) {
                logger.info("DownloadFolderRoot: " + checking + " for " + file + " is invalid! Missing or not a directory!");
                return checking;
            }
        }
        return null;
    }

    /**
     * returns DownloadControlInfo for next possible Download
     * 
     * @return
     */
    public SingleDownloadControllerActivator nextActivator() {
        ArrayList<DownloadLink> removeList = new ArrayList<DownloadLink>();
        try {
            List<List<DownloadLink>> activationRequests = new ArrayList<List<DownloadLink>>();
            if (getSession().forcedLinksWaiting()) {
                activationRequests.add(getSession().getForcedLinks());
                if (getSession().getActivateForcedOnly().get() == false) {
                    activationRequests.add(getSession().getActivationRequests());
                }
            } else {
                activationRequests.add(getSession().getActivationRequests());
            }
            boolean isForced = false;
            for (List<DownloadLink> activationRequest : activationRequests) {
                activationRequest.removeAll(removeList);
                isForced = activationRequest == getSession().getForcedLinks();
                try {
                    Iterator<DownloadLink> it = activationRequest.iterator();
                    while (it.hasNext()) {
                        DownloadLink next = it.next();
                        if (next.getDefaultPlugin() == null) {
                            /* download has no plugin, we can remove it from downloadLinks because we will never be able to handle it */
                            removeList.add(next);
                            continue;
                        }
                        if (FilePackage.isDefaultFilePackage(next.getFilePackage())) {
                            /* download has no longer a valid FilePackage because it got removed, we can remove it from downloadLinks as well */
                            removeList.add(next);
                            continue;
                        }
                        if (next.isSkipped()) {
                            /* download is skipped, remove it from downloadLinks until it is no longer skipped */
                            removeList.add(next);
                            continue;
                        }
                        if (next.getDownloadLinkController() != null) {
                            /* download is in progress */
                            continue;
                        }
                        if (!isForced && !next.isEnabled()) {
                            /* ONLY when not forced */
                            /* link is disabled, lets skip it */
                            removeList.add(next);
                            continue;
                        }
                        if (!isForced && getSession().getActiveDownloadsFromHost(next.getHost()) >= this.getSimultanDownloadNumPerHost()) {
                            /* ONLY when not forced! */
                            /* max downloads per host reached */
                            continue;
                        }
                        if (next.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE)) {
                            /*
                             * no plugin available, download not enabled,link temp unavailable
                             */
                            continue;
                        }
                        if (true) { return new SingleDownloadControllerActivator(next, null, null, false); }
                        // if ((!nextDownloadLink.getLinkStatus().isStatus(LinkStatus.TODO) &&
                        // !nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED))) {
                        // /* download is already in progress or not todo */
                        // continue linkLoop;
                        // }
                        //
                        // java.util.List<Account> usableAccounts = accountCache.get(next.getHost());
                        // if (usableAccounts == null) {
                        // usableAccounts = new ArrayList<Account>();
                        // ArrayList<Account> originalAccountswithoutCaptcha = new ArrayList<Account>();
                        // ArrayList<Account> originalAccountswithCaptcha = new ArrayList<Account>();
                        // ArrayList<Account> multiHostAccountwithoutCaptcha = new ArrayList<Account>();
                        // ArrayList<Account> multiHostAccountwithCaptcha = new ArrayList<Account>();
                        // if (org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.isEnabled()) {
                        // /* Account Handling */
                        // /* check for accounts from original plugin */
                        // LinkedList<Account> accs = AccountController.getInstance().getValidAccounts(next.getHost());
                        // if (accs != null) {
                        // PluginForHost originalPlugin = next.getDefaultPlugin();
                        // for (Account acc : accs) {
                        // if (originalPlugin.hasCaptcha(next, acc) == false) {
                        // originalAccountswithoutCaptcha.add(acc);
                        // } else {
                        // originalAccountswithCaptcha.add(acc);
                        // }
                        // }
                        // }
                        // /* check for accounts from multihost plugins */
                        // accs = AccountController.getInstance().getMultiHostAccounts(nextDownloadLink.getHost());
                        // if (accs != null) {
                        // for (Account acc : accs) {
                        // PluginForHost multiHostPlugin = getSession().getPlugin(acc.getHoster());
                        // if (multiHostPlugin.hasCaptcha(nextDownloadLink, acc) == false) {
                        // multiHostAccountwithoutCaptcha.add(acc);
                        // } else {
                        // multiHostAccountwithCaptcha.add(acc);
                        // }
                        // }
                        // }
                        // usableAccounts = originalAccountswithoutCaptcha;
                        // usableAccounts.addAll(multiHostAccountwithoutCaptcha);
                        // usableAccounts.addAll(originalAccountswithCaptcha);
                        // usableAccounts.addAll(multiHostAccountwithCaptcha);
                        // }
                        // usableAccounts.add(null);/* no account */
                        // accountCache.put(nextDownloadLink.getHost(), usableAccounts);
                        // }
                        // ProxyInfo proxy = null;
                        // boolean byPassMaxSimultanDownload = false;
                        // PluginForHost plugin = null;
                        // String host = null;
                        // boolean freeDownloadAllowed = true;
                        // accLoop: for (Account acc : usableAccounts) {
                        // if (acc != null && (!acc.isEnabled() || acc.isTempDisabled() || !acc.isValid())) {
                        // continue accLoop;
                        // }
                        // if (acc != null) {
                        // /*
                        // * we have to check if we can use account in parallel with others
                        // */
                        //
                        // conLoop: for (SingleDownloadController con : getSession().getControllers()) {
                        // Account conAcc = con.getAccount();
                        // if (conAcc == null) continue conLoop;
                        // freeDownloadAllowed = false;
                        // if (conAcc.getHoster().equalsIgnoreCase(host) && conAcc.isConcurrentUsePossible() == false && acc.isConcurrentUsePossible() == false)
                        // {
                        // /*
                        // * there is already another account handling this host and our acc does not allow concurrent use
                        // */
                        // continue accLoop;
                        // }
                        // }
                        // } else if (freeDownloadAllowed == false) {
                        // /* freeDownload not allowed, because a download with running premium is already running */
                        // continue accLoop;
                        // }
                        // byPassMaxSimultanDownload = false;
                        // proxy = null;
                        // plugin = null;
                        // if (acc == null) {
                        // /*
                        // * no account in use, so host is taken from downloadlink
                        // */
                        // host = next.getHost();
                        // } else {
                        // /* account in use, so host is taken from account */
                        // host = acc.getHoster();
                        // }
                        // /*
                        // * possible account found, lets check if we still can use it
                        // */
                        // plugin = getSession().getPlugin(host);
                        // if (!plugin.canHandle(next, acc)) {
                        // /* plugin can't download given link with acc */
                        // if (acc == null) {
                        // /*
                        // * we tried last account and noone could handle this link, so temp ignore it this session
                        // */
                        // next.setSkipReason(SkipReason.NO_ACCOUNT);
                        // removeList.add(next);
                        // }
                        // continue accLoop;
                        // }
                        // if (!plugin.enoughTrafficFor(next, acc)) {
                        // /* not enough traffic to download link with acc */
                        // if (acc == null) {
                        // /*
                        // * we tried last account and noone could handle this link, so temp ignore it this session
                        // */
                        // next.setSkipReason(SkipReason.NO_ACCOUNT);
                        // removeList.add(next);
                        // }
                        // continue accLoop;
                        // }

                        // synchronized (downloadControlHistory) {
                        // DownloadControlHistory history = downloadControlHistory.get(nextDownloadLink);
                        // DownloadControlHistoryItem accountHistory = null;
                        // if (history != null && (accountHistory = history.accountUsageHistory.get(acc)) != null) {
                        // /* account has already been used before */
                        // if (accountHistory.round < plugin.getMaxRetries(nextDownloadLink, acc)) {
                        // /* we still can retry */
                        // } else {
                        // /*
                        // * max retries reached, we do not use this account
                        // */
                        // Account lastAcc = usableAccounts.get(usableAccounts.size() - 1);
                        // if (freeDownloadAllowed == false) {
                        // lastAcc = usableAccounts.get(usableAccounts.size() - 2);
                        // }
                        // if (acc == lastAcc) {
                        // /*
                        // * we tried every possible account and none is left
                        // */
                        // /*
                        // * we remove downloadControlHistory now and retry again
                        // */
                        // downloadControlHistory.remove(nextDownloadLink);
                        // continue retryLoop;
                        // }
                        // continue accLoop;
                        // }
                        // } else {
                        // /*
                        // * account never got used before, so lets use it now
                        // */
                        // }
                        // }
                        /*
                         * can we bypass maxDownloads for this link and account
                         */
                        // byPassMaxSimultanDownload = plugin.bypassMaxSimultanDownloadNum(next, acc);
                        // /* can we use this account to download the link */
                        // proxy = ProxyController.getInstance().getProxyForDownload(plugin, next, acc, byPassMaxSimultanDownload);
                        // if (proxy != null) {
                        // /*
                        // * we can use the account and proxy to download this link
                        // */
                        // return new SingleDownloadControllerActivator(next, acc, getReplacement(proxy), byPassMaxSimultanDownload);
                        // }
                        // }
                    }
                } finally {
                    activationRequest.removeAll(removeList);
                }
            }
        } catch (final Throwable e) {
            logger.log(e);
        }
        return null;
    }

    private ProxyInfo getReplacement(ProxyInfo proxy) {
        // dirty hack to grab auth info from the updater Proxyselector
        try {
            HTTPProxy p = UpdateController.getInstance().getUpdatedProxy(proxy);
            if (p != null) {

            return new ProxyInfo(p); }
        } catch (Exception e) {
            logger.log(e);
        }
        return proxy;
    }

    /**
     * returns how many downloads are running that may not get interrupted by a reconnect
     * 
     * @return
     */
    public int getForbiddenReconnectDownloadNum() {
        final boolean allowinterrupt = CFG_RECONNECT.CFG.isReconnectAllowedToInterruptResumableDownloads();
        int ret = 0;
        for (final SingleDownloadController con : getSession().getControllers()) {
            DownloadLink link = con.getDownloadLink();
            if (link.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                DownloadInterface dl = link.getDownloadInstance();
                if (!(dl != null && dl.isResumable() && allowinterrupt)) ret++;
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
     * may the DownloadWatchDog start new Downloads?
     * 
     * @return
     */
    private boolean newDLStartAllowed() {
        boolean forcedLinksWaiting = getSession().forcedLinksWaiting();
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
        if (forcedLinksWaiting == false && CFG_RECONNECT.AUTO_RECONNECT_ENABLED.isEnabled() && CFG_RECONNECT.CFG.isDownloadControllerPrefersReconnectEnabled() && IPController.getInstance().isInvalidated()) {
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
        DownloadSession session = getSession();
        if (session.isStopMarkSet() == false) return;
        if (session.getStopMark() == STOPMARK.HIDDEN) return;
        switch (event.getType()) {
        case REMOVE_CONTENT:
            if (session.getStopMark() == event.getParameter()) {
                /* now the stopmark is hidden */
                session.setStopMark(STOPMARK.HIDDEN);
                return;
            } else if (event.getParameter() != null && event.getParameter() instanceof List) {
                List<?> list = (List<?>) event.getParameter();
                for (Object l : list) {
                    if (session.getStopMark() == l) {
                        session.setStopMark(STOPMARK.HIDDEN);
                        return;
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

    private Object                                     shutdownLock = new Object();
    protected NullsafeAtomicReference<DownloadSession> session      = new NullsafeAtomicReference<DownloadSession>(null);

    public void pauseDownloadWatchDog(final boolean value) {
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                if (value && !stateMachine.isState(DownloadWatchDog.RUNNING_STATE)) {
                    /* we can only pause downloads when downloads are running */
                    return null;
                }
                try {
                    DownloadSession session = getSession();
                    if (value) {
                        /* set pause settings */
                        session.getSpeedLimitBeforePause().set(config.getDownloadSpeedLimit());
                        session.getSpeedLimitedBeforePause().set(config.isDownloadSpeedLimitEnabled());
                        config.setDownloadSpeedLimit(config.getPauseSpeed());
                        config.setDownloadSpeedLimitEnabled(true);
                        logger.info("Pause enabled: Reducing downloadspeed to " + config.getPauseSpeed() + " KiB/s");
                        /* pause downloads */
                        DownloadWatchDog.this.getStateMachine().setStatus(PAUSE_STATE);
                    } else {
                        /* revert pause settings if available */
                        if (session.getSpeedLimitBeforePause().get() != null) {
                            logger.info("Pause disabled: Switch back to old downloadspeed");
                            config.setDownloadSpeedLimit(session.getSpeedLimitBeforePause().get());
                        }
                        if (session.getSpeedLimitedBeforePause().get() != null) config.setDownloadSpeedLimitEnabled(session.getSpeedLimitedBeforePause().get());
                        if (DownloadWatchDog.this.getStateMachine().isState(DownloadWatchDog.PAUSE_STATE)) {
                            /* we revert pause to running state */
                            DownloadWatchDog.this.getStateMachine().setStatus(RUNNING_STATE);
                        }
                    }
                } finally {
                    wakeUpWatchDog(true);
                }
                return null;
            }
        });
    }

    /**
     * resets IPBlockWaittime for the given Host
     * 
     * @param host
     */
    private void removeIPBlockTimeout(DownloadLink link) {
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
    private void removeTempUnavailTimeout(DownloadLink link) {
        ProxyController.getInstance().removeHostBlockedTimeout(link.getHost(), false);
        LinkStatus ls = link.getLinkStatus();
        if (ls.hasStatus(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE)) {
            ls.reset();
        }
    }

    public void notifyCurrentState(final DownloadWatchdogListener listener) {
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                if (DownloadWatchDog.this.stateMachine.isState(RUNNING_STATE)) {
                    listener.onDownloadWatchdogStateIsRunning();
                } else if (DownloadWatchDog.this.stateMachine.isState(STOPPED_STATE)) {
                    listener.onDownloadWatchdogStateIsStopped();
                } else if (DownloadWatchDog.this.stateMachine.isState(IDLE_STATE)) {
                    listener.onDownloadWatchdogStateIsIdle();
                } else if (DownloadWatchDog.this.stateMachine.isState(PAUSE_STATE)) {
                    listener.onDownloadWatchdogStateIsPause();
                } else if (DownloadWatchDog.this.stateMachine.isState(STOPPING_STATE)) {
                    listener.onDownloadWatchdogStateIsStopping();
                }
                return null;
            }
        });
    }

    public long getDownloadSpeedbyFilePackage(FilePackage pkg) {
        long speed = -1;
        for (SingleDownloadController con : getSession().getControllers()) {
            if (con.getDownloadLink().getFilePackage() != pkg) continue;
            speed += con.getDownloadLink().getDownloadSpeed();
        }
        return speed;
    }

    public int getDownloadsbyFilePackage(FilePackage pkg) {
        int ret = 0;
        for (SingleDownloadController con : getSession().getControllers()) {
            if (con.getDownloadLink().getFilePackage() != pkg) continue;
            ret++;
        }
        return ret;
    }

    public List<SingleDownloadController> getRunningDownloadLinks() {
        return getSession().getControllers();
    }

    public List<FilePackage> getRunningFilePackages() {
        HashSet<FilePackage> ret = new HashSet<FilePackage>();
        for (SingleDownloadController con : getSession().getControllers()) {
            ret.add(con.getDownloadLink().getParentNode());
        }
        return new ArrayList<FilePackage>(ret);
    }

    public boolean hasRunningDownloads(FilePackage pkg) {
        for (SingleDownloadController con : getSession().getControllers()) {
            if (con.getDownloadLink().getFilePackage() == pkg) return true;
        }
        return false;
    }

    /**
     * aborts all running SingleDownloadControllers, NOTE: DownloadWatchDog is still running, new Downloads will can started after this call
     */
    public void abortAllSingleDownloadControllers() {
        while (true) {
            for (SingleDownloadController con : getSession().getControllers()) {
                if (con.isAlive() && !con.isAborting()) {
                    con.abort();
                }
            }
            synchronized (WATCHDOGWAITLOOP) {
                if (getSession().getControllers().size() == 0) return;
                try {
                    WATCHDOGWAITLOOP.wait(1000);
                } catch (InterruptedException e) {
                    logger.log(e);
                    return;
                }
            }
        }
    }

    public void enqueueJob(final DownloadWatchDogJob job) {
        watchDogJobs.offer(job);
        if (Thread.currentThread() != currentWatchDogThread.get()) {
            wakeUpWatchDog(true);
        } else {
            wakeUpWatchDog(false);
        }
    }

    public void resume(final List<DownloadLink> resetLinks) {
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(List<DownloadLink> downloadWatchDogLoopLinks) {
                for (DownloadLink link : resetLinks) {
                    if (link.getDownloadLinkController() == null && (link.getLinkStatus().isFailed() || link.isSkipped())) {
                        resumeLink(link);
                    }
                }
                return;
            }
        });
    }

    public void reset(final List<DownloadLink> resetLinks) {
        if (resetLinks == null || resetLinks.size() == 0) return;
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(List<DownloadLink> downloadWatchDogLoopLinks) {
                if (downloadWatchDogLoopLinks != null) downloadWatchDogLoopLinks.removeAll(resetLinks);
                for (final DownloadLink link : resetLinks) {
                    SingleDownloadController con = link.getDownloadLinkController();
                    if (con == null || con.isAlive() == false || con.isActive() == false) {
                        /* link has no/no alive singleDownloadController, so reset it now */
                        resetLink(link);
                    } else {
                        /* link has a running singleDownloadController, abort it and reset it after */
                        con.abort();
                        enqueueJob(new DownloadWatchDogJob() {
                            @Override
                            public void execute(List<DownloadLink> downloadWatchDogLoopLinks) {
                                if (downloadWatchDogLoopLinks != null) downloadWatchDogLoopLinks.remove(link);
                                SingleDownloadController con = link.getDownloadLinkController();
                                if (con != null && con.isAlive()) {
                                    /* link still has a running singleDownloadController, abort it and delete it after */
                                    con.abort();
                                    /* enqueue again */
                                    enqueueJob(this);
                                } else {
                                    /* now we can reset the link */
                                    resetLink(link);
                                }
                                return;
                            }
                        });
                    }
                }
                return;
            }
        });
    }

    private void resumeLink(DownloadLink link) {
        if (link.getDownloadLinkController() != null) throw new IllegalStateException("Link is in progress! cannot resume!");
        if (link.isSkipped()) link.setSkipReason(SkipReason.NONE);
        if (!link.isEnabled()) link.setEnabled(true);
        link.getLinkStatus().reset(true);
        removeIPBlockTimeout(link);
        removeTempUnavailTimeout(link);
    }

    private void resetLink(DownloadLink link) {
        if (link.getDownloadLinkController() != null) throw new IllegalStateException("Link is in progress! cannot reset!");
        deleteFile(link, DeleteTo.NULL);
        removeIPBlockTimeout(link);
        removeTempUnavailTimeout(link);
        link.reset();
    }

    public void delete(final List<DownloadLink> deleteFiles, final DeleteTo deleteTo) {
        if (deleteFiles == null || deleteFiles.size() == 0) return;
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(List<DownloadLink> downloadWatchDogLoopLinks) {
                if (downloadWatchDogLoopLinks != null) downloadWatchDogLoopLinks.removeAll(deleteFiles);
                for (final DownloadLink link : deleteFiles) {
                    SingleDownloadController con = link.getDownloadLinkController();
                    if (con == null || con.isAlive() == false || con.isActive() == false) {
                        /* link has no/no alive singleDownloadController, so delete it now */
                        deleteFile(link, deleteTo);
                    } else {
                        /* link has a running singleDownloadController, abort it and delete it after */
                        con.abort();
                        enqueueJob(new DownloadWatchDogJob() {
                            @Override
                            public void execute(List<DownloadLink> downloadWatchDogLoopLinks) {
                                if (downloadWatchDogLoopLinks != null) downloadWatchDogLoopLinks.remove(link);
                                SingleDownloadController con = link.getDownloadLinkController();
                                if (con != null && con.isAlive()) {
                                    /* link still has a running singleDownloadController, abort it and delete it after */
                                    con.abort();
                                    /* enqueue again */
                                    enqueueJob(this);
                                    return;
                                } else {
                                    /* now we can delete the link */
                                    deleteFile(link, deleteTo);
                                }
                                return;
                            }
                        });
                    }
                }
                return;
            }
        });
    }

    private void deleteFile(DownloadLink link, DeleteTo deleteTo) {
        if (deleteTo == null) deleteTo = DeleteTo.NULL;
        ArrayList<File> deleteFiles = new ArrayList<File>();
        try {
            File deletePartFile = new File(link.getFileOutput() + ".part");
            if (deletePartFile.exists() && deletePartFile.isFile()) {
                try {
                    getSession().getFileAccessManager().lock(deletePartFile, this);
                    deleteFiles.add(deletePartFile);
                } catch (FileIsLockedException e) {
                    logger.log(e);
                }
            }
            String finalFilePath = link.getFinalFileOutput();
            if (StringUtils.isNotEmpty(finalFilePath)) {
                /* only delete finalFile if it got loaded by this link */
                File deleteFinalFile = new File(finalFilePath);
                if (deleteFinalFile.exists() && deleteFinalFile.isFile()) {
                    try {
                        getSession().getFileAccessManager().lock(deleteFinalFile, this);
                        deleteFiles.add(deleteFinalFile);
                    } catch (FileIsLockedException e) {
                        logger.log(e);
                    }
                }
                link.setFinalFileOutput(null);
            }
            for (File deleteFile : deleteFiles) {
                switch (deleteTo) {
                case NULL:
                    if (deleteFile.delete() == false) {
                        logger.info("Could not delete file: " + deleteFile + " for " + link);
                    } else {
                        logger.info("Deleted file: " + deleteFile + " for " + link);
                    }
                    break;
                case RECYCLE:
                    try {
                        JDFileUtils.moveToTrash(deleteFile);
                        logger.info("Recycled file: " + deleteFile + " for " + link);
                    } catch (IOException e) {
                        logger.info("Could not recycle file: " + deleteFile + " for " + link);
                    }
                    break;
                }
            }
            /* try to delete folder (if its empty and NOT the default downloadfolder */
            File dlFolder = deletePartFile.getParentFile();
            if (dlFolder != null && dlFolder.exists() && dlFolder.isDirectory() && dlFolder.listFiles() != null && dlFolder.listFiles().length == 0) {
                if (!new File(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder()).equals(dlFolder)) {
                    if (dlFolder.delete()) {
                        logger.info("Could not delete folder: " + dlFolder + " for " + link);
                    } else {
                        logger.info("Deleted folder: " + dlFolder + " for " + link);
                    }
                }
            }
        } finally {
            for (File deleteFile : deleteFiles) {
                getSession().getFileAccessManager().unlock(deleteFile, this);
            }
        }
    }

    /**
     * activates new Downloads as long as possible and returns how many got activated
     * 
     * @return
     **/
    private List<SingleDownloadController> activateDownloads() throws Exception {
        ArrayList<SingleDownloadController> ret = new ArrayList<SingleDownloadController>();
        DownloadSession session = getSession();
        int maxConcurrentNormal = config.getMaxSimultaneDownloads();
        int maxConcurrentForced = maxConcurrentNormal + Math.max(0, config.getMaxForcedDownloads());
        int maxLoopsNormal = session.getActivationRequests().size();
        int maxLoopsForced = session.getForcedLinks().size();

        startLoop: while (session.forcedLinksWaiting() || ((getActiveDownloads() < maxConcurrentNormal) && maxLoopsNormal > 0)) {
            try {
                if (!this.newDLStartAllowed() || session.isStopMarkReached()) {
                    break;
                }
                // if (session.forcedLinksWaiting() || session.getStopAfterForcedLinks().get()) {
                // /* we try to force the link in forcedLink array */
                // ArrayList<DownloadLink> cleanup = new ArrayList<DownloadLink>();
                // synchronized (forcedLinks) {
                // // slow...maybe we should integrate this in getNextDownloadLink?
                // for (DownloadLink dl : forcedLinks) {
                // if (!dl.getLinkStatus().isStatus(LinkStatus.TODO) || dl.isSkipped() || FilePackage.isDefaultFilePackage(dl.getFilePackage())) {
                // cleanup.add(dl);
                // }
                // }
                // forcedLinks.removeAll(cleanup);
                // dci = this.getNextDownloadLink(forcedLinks, accountCache, pluginCache, true);
                // }
                // if (dci == null) {
                // break;
                // }
                // } else {
                /* we try to find next possible normal download */
                SingleDownloadControllerActivator activator = this.nextActivator();
                if (activator == null) {
                    /* no next possible download found */
                    break;
                }
                // }
                DownloadLink dlLink = activator.getLink();
                if (CaptchaDuringSilentModeAction.SKIP_LINK == CFG_SILENTMODE.CFG.getOnCaptchaDuringSilentModeAction()) {
                    try {
                        String hoster = dlLink.getHost();
                        if (activator.getAccount() != null) hoster = activator.getAccount().getHoster();
                        PluginForHost plugin = getSession().getPlugin(hoster);
                        if (plugin != null && plugin.hasCaptcha(dlLink, activator.getAccount())) {
                            dlLink.setSkipReason(SkipReason.CAPTCHA);
                            continue;
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
                String dlFolder = dlLink.getFilePackage().getDownloadDirectory();
                DISKSPACECHECK check = checkFreeDiskSpace(new File(dlFolder), null, (dlLink.getDownloadSize() - dlLink.getDownloadCurrent()));
                synchronized (shutdownLock) {
                    if (!ShutdownController.getInstance().isShutDownRequested()) {
                        switch (check) {
                        case OK:
                        case UNKNOWN:
                            logger.info("Start " + activator);
                            ret.add(this.activate(activator));
                            break;
                        case FAILED:
                            logger.info("Could not start " + dlLink + ": not enough diskspace free in " + dlFolder);
                            dlLink.setSkipReason(SkipReason.DISK_FULL);
                            break;
                        case INVALIDFOLDER:
                            logger.info("Could not start " + dlLink + ": invalid downloadfolder->" + dlFolder);
                            dlLink.setSkipReason(SkipReason.INVALID_DESTINATION);
                            break;
                        }
                    }
                }
            } finally {
                maxLoopsNormal--;
            }
        }
        return ret;
    }

    public void startDownloads() {
        startDownloads(null);
    }

    /**
     * start the DownloadWatchDog
     */
    public void startDownloads(final Runnable runBeforeStartingDownloadWatchDog) {
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                if (DownloadWatchDog.this.stateMachine.isFinal()) {
                    /* downloadwatchdog was in stopped state, so reset it */
                    DownloadWatchDog.this.stateMachine.reset(false);
                }
                if (!DownloadWatchDog.this.stateMachine.isStartState()) {
                    /* only allow to start when in FinalState(NOT_RUNNING) */
                    return null;
                }
                /*
                 * setNewSession
                 */
                DownloadSession oldSession = getSession();
                DownloadSession newSession = new DownloadSession();
                if (oldSession.isStopMarkSet() && oldSession.isStopMarkReached() == false) {
                    newSession.setStopMark(oldSession.getStopMark());
                }
                session.set(newSession);
                /* set state to running */
                stateMachine.setStatus(RUNNING_STATE);
                /* throw start event */
                logger.info("DownloadWatchDog: start");
                if (runBeforeStartingDownloadWatchDog != null) {
                    try {
                        runBeforeStartingDownloadWatchDog.run();
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
                /* start watchdogthread */
                startDownloadWatchDog();
                return null;
            }
        });
    }

    public DownloadSession getSession() {
        return session.get();
    }

    /**
     * activates a new SingleDownloadController for the given SingleDownloadControllerActivator
     * 
     * @param activator
     */
    private SingleDownloadController activate(final SingleDownloadControllerActivator activator) {
        logger.info("Start new Download: Host:" + activator);
        Runnable run = new Runnable() {
            @Override
            public void run() {
                enqueueJob(new DownloadWatchDogJob() {

                    @Override
                    public void execute(List<DownloadLink> downloadWatchDogLoopLinks) {
                        DownloadLink link = activator.getLink();
                        if (activator.isByPassSimultanDownloadNum() == false && activator.getProxy() != null) {
                            activator.getProxy().decreaseActiveDownloads(link.getHost());
                        }
                        try {
                            SingleDownloadController con = link.getDownloadLinkController();
                            getSession().getControllers().remove(con);
                        } finally {
                            wakeUpWatchDog(true);
                        }
                        return;
                    }
                });
            }
        };
        final SingleDownloadController con = new SingleDownloadController(activator, this.dsm, run);
        if (activator.isByPassSimultanDownloadNum() == false && activator.getProxy() != null) {
            activator.getProxy().increaseActiveDownloads(activator.getLink().getHost());
        }
        activator.getLink().setEnabled(true);
        getSession().getControllers().add(con);
        con.start();
        return con;
    }

    private synchronized void startDownloadJobExecuter() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                this.setName("WatchDog: jobExecuter");
                try {
                    while (true) {
                        synchronized (WATCHDOGWAITLOOP) {
                            if (Thread.currentThread() != currentWatchDogThread.get()) return;
                            if (watchDogJobs.size() == 0) WATCHDOGWAITLOOP.wait();
                            if (Thread.currentThread() != currentWatchDogThread.get()) return;
                        }
                        DownloadWatchDogJob job = watchDogJobs.poll();
                        if (job != null) job.execute(null);

                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
            }
        };
        thread.setDaemon(true);
        currentWatchDogThread.set(thread);
        thread.start();
    }

    private synchronized void startDownloadWatchDog() {
        Thread thread = new Thread() {

            protected void processJobs(List<DownloadLink> links) {
                ArrayList<DownloadWatchDogJob> jobs = new ArrayList<DownloadWatchDogJob>();
                watchDogJobs.drainTo(jobs);
                for (DownloadWatchDogJob job : jobs) {
                    try {
                        job.execute(links);
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
            }

            @Override
            public void run() {
                this.setName("WatchDog: downloadWatchDog");
                DownloadControllerListener listener = null;
                AccountControllerListener accListener = null;
                final LinkedBlockingDeque<DownloadLink> stopDownloads = new LinkedBlockingDeque<DownloadLink>();
                long currentWatchDogLoop = 0;
                try {
                    /* reset skipReasons */
                    DownloadController.getInstance().set(new DownloadLinkWalker() {

                        @Override
                        public void handle(DownloadLink link) {
                            if (link.getSkipReason() != SkipReason.PLUGIN_DEFECT) link.setSkipReason(null);
                        }

                        @Override
                        public boolean accept(FilePackage fp) {
                            return true;
                        }

                        @Override
                        public boolean accept(DownloadLink link) {
                            return true;
                        }
                    });

                    AccountController.getInstance().getBroadcaster().addListener(accListener = new AccountControllerListener() {

                        @Override
                        public void onAccountControllerEvent(AccountControllerEvent event) {
                        }
                    }, true);

                    DownloadController.getInstance().addListener(listener = new DownloadControllerListener() {

                        @Override
                        public void onDownloadControllerEvent(DownloadControllerEvent event) {
                            boolean notify = false;
                            switch (event.getType()) {
                            case REMOVE_CONTENT:
                                if (event.getParameter() != null && event.getParameter() instanceof List) {
                                    List<?> list = (List<?>) event.getParameter();
                                    for (Object l : list) {
                                        if (l instanceof DownloadLink) {
                                            notify = true;
                                            stopDownloads.add((DownloadLink) l);
                                        }
                                    }
                                }
                                break;
                            case REFRESH_CONTENT:
                                if (event.getParameter() instanceof DownloadLink) {
                                    DownloadLink dl = (DownloadLink) event.getParameter();
                                    if (dl != null) {
                                        Object property = event.getParameter(1);
                                        if (property instanceof DownloadLinkProperty) {
                                            DownloadLinkProperty dlProperty = (DownloadLinkProperty) property;
                                            switch (dlProperty.getProperty()) {
                                            case ENABLED:
                                                if (Boolean.FALSE.equals(dlProperty.getValue())) {
                                                    synchronized (stopDownloads) {
                                                        notify = true;
                                                        stopDownloads.add(dlProperty.getDownloadLink());
                                                    }
                                                }
                                                break;
                                            case SKIPPED:
                                                if (!SkipReason.NONE.equals(dlProperty.getValue())) {
                                                    notify = true;
                                                    stopDownloads.add(dlProperty.getDownloadLink());
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                            if (notify) {
                                wakeUpWatchDog(true);
                            }
                        }
                    }, true);

                    LinkStatus linkStatus;
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
                        hasTempDisabledLinks = false;
                        waitingNewIP = false;
                        resetWaitingNewIP = false;
                        if (lastReconnectCounter < Reconnecter.getReconnectCounter()) {
                            /* an IP-change happend, reset waittimes */
                            lastReconnectCounter = Reconnecter.getReconnectCounter();
                            ProxyController.getInstance().removeIPBlockTimeout(null, true);
                            resetWaitingNewIP = true;
                        }

                        long currentStructure = DownloadController.getInstance().getPackageControllerChanges();
                        long currentContent = DownloadController.getInstance().getContentChanges();
                        if (currentStructure != lastStructureChange || currentContent != lastContentChange) {
                            {
                                List<DownloadLink> links = new ArrayList<DownloadLink>();
                                /*
                                 * create a map holding all possible links sorted by their position in list and their priority
                                 * 
                                 * by doing this we don't have to walk through possible links multiple times to find next download link, as the list itself will
                                 * already be correct sorted
                                 */
                                HashMap<Long, java.util.List<DownloadLink>> optimizedList = new HashMap<Long, java.util.List<DownloadLink>>();
                                /*
                                 * changes in DownloadController available, refresh DownloadList
                                 */
                                int skippedLinksCounterTmp = 0;
                                for (FilePackage fp : DownloadController.getInstance().getPackagesCopy()) {
                                    boolean readL = fp.getModifyLock().readLock();
                                    try {
                                        for (DownloadLink fpLink : fp.getChildren()) {
                                            if (fpLink.getDefaultPlugin() == null) continue;
                                            if (fpLink.isSkipped()) {
                                                skippedLinksCounterTmp++;
                                                continue;
                                            }
                                            if (!fpLink.isEnabled()) continue;
                                            if (fpLink.getAvailableStatus() == AvailableStatus.FALSE) continue;
                                            if (fpLink.getDownloadLinkController() == null) {
                                                if (fpLink.getLinkStatus().isFinished()) continue;
                                            }
                                            long prio = fpLink.getPriority();
                                            java.util.List<DownloadLink> list = optimizedList.get(prio);
                                            if (list == null) {
                                                list = new ArrayList<DownloadLink>();
                                                optimizedList.put(prio, list);
                                            }
                                            list.add(fpLink);
                                        }
                                    } finally {
                                        fp.getModifyLock().readUnlock(readL);
                                    }
                                }
                                getSession().getSkipCounter().set(skippedLinksCounterTmp);
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
                                getSession().setActivationRequests(new CopyOnWriteArrayList<DownloadLink>(links));
                                eventSender.fireEvent(new DownloadWatchdogEvent(this, DownloadWatchdogEvent.Type.DATA_UPDATE));
                            }
                        }
                        try {
                            ArrayList<DownloadLink> stopDownloadsCopy = new ArrayList<DownloadLink>();
                            stopDownloads.drainTo(stopDownloadsCopy);
                            if (stopDownloadsCopy.size() > 0) {
                                getSession().getActivationRequests().removeAll(stopDownloadsCopy);
                                getSession().getForcedLinks().removeAll(stopDownloadsCopy);
                                for (DownloadLink stopDownloadCopy : stopDownloadsCopy) {
                                    SingleDownloadController con = stopDownloadCopy.getDownloadLinkController();
                                    if (con != null && con.isAlive()) {
                                        con.abort();
                                    }
                                }
                            }
                            processJobs(getSession().getActivationRequests());
                            for (DownloadLink link : getSession().getActivationRequests()) {
                                linkStatus = link.getLinkStatus();
                                if (link.isEnabled()) {
                                    if (link.getDownloadLinkController() == null) {
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
                                                if (getSession().getActiveDownloadsFromHost(link.getHost()) == 0) {
                                                    /*
                                                     * do not reconnect if the request comes from host with active downloads, this will prevent reconnect loops
                                                     * for plugins that allow resume and parallel downloads
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
                                            if (getSession().getActiveDownloadsFromHost(link.getHost()) == 0) {
                                                /*
                                                 * do not reconnect if the request comes from host with active downloads, this will prevent reconnect loops for
                                                 * plugins that allow resume and parallel downloads
                                                 */
                                                waitingNewIP = true;
                                                IPController.getInstance().invalidate();
                                            }
                                        }
                                    }
                                }
                            }
                            /* request a reconnect if allowed and needed */
                            Reconnecter.getInstance().run();
                            List<SingleDownloadController> ret = DownloadWatchDog.this.activateDownloads();
                            if (ret.size() == 0) {
                                /*
                                 * no new download got started, check what happened and what to do next
                                 */
                                if (!hasTempDisabledLinks && getSession().getControllers().size() == 0 && !waitingNewIP) {
                                    /*
                                     * no tempdisabled, no in progress, no reconnect and no next download waiting and no active downloads
                                     */
                                    if (DownloadWatchDog.this.newDLStartAllowed()) {
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
                                continue;
                            }
                        } catch (final Exception e) {
                            logger.log(e);
                        }
                        try {
                            int round = 0;
                            synchronized (WATCHDOGWAITLOOP) {
                                try {
                                    while (DownloadWatchDog.this.stateMachine.isState(DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.PAUSE_STATE)) {
                                        if (++round == 4 || (getSession().getActivationRequests().size() == 0 && DownloadWatchDog.this.getActiveDownloads() == 0) || currentWatchDogLoop != WATCHDOGWAITLOOP.get()) break;
                                        WATCHDOGWAITLOOP.wait(1250);
                                    }
                                } finally {
                                    currentWatchDogLoop = WATCHDOGWAITLOOP.get();
                                }
                            }
                        } catch (final InterruptedException e) {
                        }
                    }
                    TaskQueue.getQueue().addWait(new QueueAction<Void, RuntimeException>() {

                        @Override
                        protected Void run() throws RuntimeException {
                            stateMachine.setStatus(STOPPING_STATE);
                            return null;
                        }
                    });
                    DownloadController.getInstance().removeListener(listener);
                    AccountController.getInstance().getBroadcaster().removeListener(accListener);
                    logger.info("DownloadWatchDog: stopping");
                    synchronized (DownloadWatchDog.this) {
                        startDownloadJobExecuter();
                    }
                    /* stop all remaining downloads */
                    abortAllSingleDownloadControllers();
                    /* clear Status */
                    clearDownloadListStatus();
                    /* clear blocked Accounts */
                    AccountController.getInstance().removeAccountBlocked(null);
                    /* unpause downloads */
                    pauseDownloadWatchDog(false);
                    if (getSession().isStopMarkReached()) {
                        /* remove stopsign if it has been reached */
                        getSession().setStopMark(STOPMARK.NONE);
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                    TaskQueue.getQueue().addWait(new QueueAction<Void, RuntimeException>() {

                        @Override
                        protected Void run() throws RuntimeException {
                            stateMachine.setStatus(STOPPING_STATE);
                            return null;
                        }
                    });
                } finally {
                    /* full stop reached */
                    logger.info("DownloadWatchDog: stopped");
                    /* clear activationPluginCache */
                    getSession().getActivationPluginCache().clear();
                    getSession().getActivationRequests().clear();
                    /* clear forcedLinks list */
                    synchronized (getSession().getForcedLinks()) {
                        getSession().getForcedLinks().clear();
                    }
                    TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                        @Override
                        protected Void run() throws RuntimeException {
                            stateMachine.setStatus(STOPPED_STATE);
                            return null;
                        }
                    });
                }
            }
        };
        thread.setDaemon(true);
        currentWatchDogThread.set(thread);
        thread.start();
    }

    public int getSkippedLinksCounter() {
        return getSession().getSkipCounter().get();
    }

    /**
     * tell the DownloadWatchDog to stop all running Downloads
     */
    public void stopDownloads() {
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                if (DownloadWatchDog.this.stateMachine.isFinal() || DownloadWatchDog.this.stateMachine.isStartState()) {
                    /* not downloading */
                    return null;
                }
                if (DownloadWatchDog.this.stateMachine.isState(STOPPING_STATE)) {
                    /* download is already in stopping, stopped state */
                    return null;
                }
                /* we now want to stop all downloads */
                stateMachine.setStatus(STOPPING_STATE);
                return null;
            }
        });
    }

    /**
     * toggles between start/stop states
     */
    public void toggleStartStop() {
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                if (stateMachine.isStartState() || stateMachine.isFinal()) {
                    /* download is in idle or stopped state */
                    DownloadWatchDog.this.startDownloads();
                } else {
                    /* download can be stopped */
                    DownloadWatchDog.this.stopDownloads();
                }
                return null;
            }

        });
    }

    @Deprecated
    /**
     * @Deprecated: User getEventSender Instead
     */
    public StateMachine getStateMachine() {
        return this.stateMachine;
    }

    public void onShutdown(boolean silent) {
        TaskQueue.getQueue().addWait(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                if (stateMachine.isState(RUNNING_STATE, PAUSE_STATE)) {
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

    public void localFileCheck(final SingleDownloadController controller, final ExceptionRunnable runOkay, final ExceptionRunnable runFailed) throws Exception {
        final NullsafeAtomicReference<Object> asyncResult = new NullsafeAtomicReference<Object>(null);
        enqueueJob(new DownloadWatchDogJob() {

            private void check(SingleDownloadController controller) throws Exception {
                DownloadLink downloadLink = controller.getDownloadLink();
                if (!downloadLink.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                    if (!downloadLink.isAvailabilityStatusChecked() && downloadLink.getForcedFileName() == null) {
                        /*
                         * dont proceed if no linkcheck has done yet, maybe we dont know filename yet
                         */
                        return;
                    }
                }
                String localCheck = downloadLink.getFileOutput();
                File fileOutput = new File(localCheck);
                if (validateDestination(fileOutput) != null) throw new PluginException(SkipReason.INVALID_DESTINATION);
                boolean fileExists = fileOutput.exists();
                if (!fileExists) {
                    for (SingleDownloadController downloadController : getSession().getControllers()) {
                        DownloadLink block = downloadController.getDownloadLink();
                        if (block == downloadLink) continue;
                        if (downloadController.getPlugin() != null && localCheck.equalsIgnoreCase(block.getFileOutput())) { throw new PluginException(SkipReason.FILE_IN_PROCESS); }
                    }
                }
                if (fileOutput.getParentFile() == null) { throw new PluginException(SkipReason.INVALID_DESTINATION); }
                if (fileOutput.isDirectory()) { throw new PluginException(SkipReason.INVALID_DESTINATION); }
                if (!fileOutput.getParentFile().exists()) {
                    if (!fileOutput.getParentFile().mkdirs()) { throw new PluginException(SkipReason.INVALID_DESTINATION); }
                }
                if (fileExists) {
                    if (JsonConfig.create(GeneralSettings.class).getIfFileExistsAction() == IfFileExistsAction.OVERWRITE_FILE) {
                        if (!fileOutput.delete()) { throw new PluginException(LinkStatus.ERROR_FATAL, _JDT._.system_download_errors_couldnotoverwrite()); }
                    } else {
                        throw new PluginException(SkipReason.FILE_EXISTS);
                    }
                }
                DISKSPACECHECK check = checkFreeDiskSpace(fileOutput.getParentFile(), controller, (downloadLink.getDownloadSize() - downloadLink.getDownloadCurrent()));
                switch (check) {
                case FAILED:
                    throw new PluginException(SkipReason.DISK_FULL);
                case INVALIDFOLDER:
                    throw new PluginException(SkipReason.INVALID_DESTINATION);
                }
                return;
            }

            @Override
            public void execute(List<DownloadLink> downloadWatchDogLoopLinks) {
                try {
                    check(controller);
                    if (runOkay != null) runOkay.run();
                    synchronized (asyncResult) {
                        asyncResult.set(asyncResult);
                        asyncResult.notifyAll();
                    }
                } catch (final Exception e) {
                    logger.log(e);
                    try {
                        if (runFailed != null) runFailed.run();
                    } catch (final Throwable e2) {
                        logger.log(e2);
                    } finally {
                        synchronized (asyncResult) {
                            asyncResult.set(e);
                            asyncResult.notifyAll();
                        }
                    }
                }
            }
        });
        Object ret = null;
        while (true) {
            synchronized (asyncResult) {
                ret = asyncResult.get();
                if (ret != null) break;
                asyncResult.wait();
            }
        }
        if (asyncResult == ret) return;
        if (ret instanceof PluginException) throw (PluginException) ret;
        if (ret instanceof Exception) throw (Exception) ret;
        throw new WTFException("WTF? Result: " + ret);
    }

    @Override
    public void onShutdownVetoRequest(ShutdownRequest request) throws ShutdownVetoException {
        if (request.getVetos().size() > 0) {
            /* we already abort shutdown, no need to ask again */
            /*
             */
            return;
        }

        String dialogTitle = null;
        synchronized (this.shutdownLock) {
            /*
             * we sync to make sure that no new downloads get started meanwhile
             */
            if (this.stateMachine.isState(RUNNING_STATE, PAUSE_STATE)) {
                for (final SingleDownloadController con : getSession().getControllers()) {
                    if (con.isAlive() == false || con.isActive() == false) continue;
                    dialogTitle = _JDT._.DownloadWatchDog_onShutdownRequest_();
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
            if (dialogTitle != null) {
                if (request.isSilent() == false) {
                    if (UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, dialogTitle, _JDT._.DownloadWatchDog_onShutdownRequest_msg(), NewTheme.I().getIcon("download", 32), _JDT._.literally_yes(), null)) { return; }
                    throw new ShutdownVetoException(dialogTitle, this);
                } else {
                    throw new ShutdownVetoException(dialogTitle, this);
                }
            }
        }
    }

    @Override
    public long getShutdownVetoPriority() {
        return 0;
    }

    public int getNonResumableRunningCount() {
        int i = 0;
        if (this.stateMachine.isState(RUNNING_STATE, PAUSE_STATE, STOPPING_STATE)) {
            for (final SingleDownloadController con : getSession().getControllers()) {
                DownloadLink link = con.getDownloadLink();
                if (link.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                    DownloadInterface dl = link.getDownloadInstance();
                    if (dl != null && !dl.isResumable()) {
                        i++;
                    }
                }
            }
        }
        return i;
    }

    public long getNonResumableBytes() {
        long i = 0;
        if (this.stateMachine.isState(RUNNING_STATE, PAUSE_STATE, STOPPING_STATE)) {
            for (final SingleDownloadController con : getSession().getControllers()) {
                DownloadLink link = con.getDownloadLink();
                if (link.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                    DownloadInterface dl = link.getDownloadInstance();
                    if (dl != null && !dl.isResumable()) {
                        i += link.getDownloadCurrent();
                    }
                }
            }
        }
        return i;
    }

    public boolean isLinkForced(DownloadLink dlLink) {
        if (getSession().getForcedLinks().size() == 0) return false;
        return getSession().getForcedLinks().contains(dlLink);
    }

    public boolean isRunning() {
        return stateMachine.isState(RUNNING_STATE, PAUSE_STATE);
    }

    public boolean isIdle() {
        return stateMachine.isState(IDLE_STATE, STOPPED_STATE);
    }

    @Override
    public void onShutdown(ShutdownRequest request) {
    }

    @Override
    public void onShutdownVeto(ShutdownRequest request) {
    }

}