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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.controlling.TaskQueue;
import jd.controlling.downloadcontroller.AccountCache.CachedAccount;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult.RESULT;
import jd.controlling.downloadcontroller.DownloadSession.STOPMARK;
import jd.controlling.downloadcontroller.DownloadSession.SessionState;
import jd.controlling.downloadcontroller.ProxyInfoHistory.WaitingSkipReasonContainer;
import jd.controlling.downloadcontroller.event.DownloadWatchdogEvent;
import jd.controlling.downloadcontroller.event.DownloadWatchdogEventSender;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.ProxyInfo;
import jd.controlling.reconnect.Reconnecter;
import jd.controlling.reconnect.Reconnecter.ReconnectResult;
import jd.controlling.reconnect.ReconnecterEvent;
import jd.controlling.reconnect.ReconnecterListener;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageProperty;
import jd.plugins.LinkStatus;
import jd.plugins.LinkStatusProperty;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.raf.HashResult;

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
import org.appwork.uio.UserIODefinition.CloseReason;
import org.appwork.utils.Application;
import org.appwork.utils.ConcatIterator;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.ProxyAuthException;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.captcha.blacklist.CaptchaBlackList;
import org.jdownloader.controlling.DownloadLinkWalker;
import org.jdownloader.controlling.FileCreationEvent;
import org.jdownloader.controlling.FileCreationListener;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.FileCreationManager.DeleteOption;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.download.DownloadControllerListener;
import org.jdownloader.controlling.hosterrule.AccountUsageRule;
import org.jdownloader.controlling.hosterrule.HosterRuleController;
import org.jdownloader.controlling.hosterrule.HosterRuleControllerListener;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.ConditionalSkipReason;
import org.jdownloader.plugins.ConditionalSkipReasonException;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.plugins.IgnorableConditionalSkipReason;
import org.jdownloader.plugins.MirrorLoading;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.plugins.ValidatableConditionalSkipReason;
import org.jdownloader.plugins.WaitForAccountSkipReason;
import org.jdownloader.plugins.WaitingSkipReason;
import org.jdownloader.plugins.WaitingSkipReason.CAUSE;
import org.jdownloader.settings.CleanAfterDownloadAction;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.IfFileExistsAction;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;
import org.jdownloader.settings.staticreferences.CFG_RECONNECT;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.UpdateController;
import org.jdownloader.utils.JDFileUtils;

public class DownloadWatchDog implements DownloadControllerListener, StateMachineInterface, ShutdownVetoListener, FileCreationListener {

    private static class ReconnectThread extends Thread {

        private AtomicBoolean                        finished = new AtomicBoolean(false);
        private volatile Reconnecter.ReconnectResult result   = null;

        public Reconnecter.ReconnectResult waitForResult() throws InterruptedException {
            return waitForResult(-1);
        }

        public Reconnecter.ReconnectResult waitForResult(long timeout) throws InterruptedException {
            synchronized (finished) {
                if (finished.get() == false) {
                    if (timeout > 0) {
                        finished.wait(timeout);
                    } else {
                        finished.wait();
                    }
                    if (finished.get() == false) { return Reconnecter.ReconnectResult.RUNNING; }
                }
            }
            return result;
        }

        @Override
        public void run() {
            try {
                IPController.getInstance().invalidate();
                result = Reconnecter.getInstance().doReconnect();
            } finally {
                DownloadWatchDog.getInstance().reconnectThread.compareAndSet(Thread.currentThread(), null);
                synchronized (finished) {
                    finished.set(true);
                    finished.notifyAll();
                }
            }
        }
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

    public static enum DISKSPACECHECK {
        UNKNOWN,
        OK,
        INVALIDFOLDER,
        FAILED
    }

    protected final NullsafeAtomicReference<Thread>        currentWatchDogThread = new NullsafeAtomicReference<Thread>(null);
    protected final NullsafeAtomicReference<Thread>        reconnectThread       = new NullsafeAtomicReference<Thread>(null);
    protected final NullsafeAtomicReference<Thread>        tempWatchDogJobThread = new NullsafeAtomicReference<Thread>(null);
    protected NullsafeAtomicReference<DownloadWatchDogJob> currentWatchDogJob    = new NullsafeAtomicReference<DownloadWatchDogJob>(null);
    private final LinkedBlockingDeque<DownloadWatchDogJob> watchDogJobs          = new LinkedBlockingDeque<DownloadWatchDogJob>();

    private final StateMachine                             stateMachine;
    private final DownloadSpeedManager                     dsm;

    private final GeneralSettings                          config;

    private final static DownloadWatchDog                  INSTANCE              = new DownloadWatchDog();

    private final LogSource                                logger;
    private final AtomicLong                               WATCHDOGWAITLOOP      = new AtomicLong(0);
    private final AtomicBoolean                            autoReconnectEnabled  = new AtomicBoolean(false);
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
        DownloadSession initSession = new DownloadSession();
        session.set(initSession);
        this.dsm = new DownloadSpeedManager();
        /* speed limit */
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT.getEventSender().addListener(new GenericConfigEventListener<Integer>() {

            public void onConfigValueModified(KeyHandler<Integer> keyHandler, Integer newValue) {
                dsm.setLimit(config.isDownloadSpeedLimitEnabled() ? config.getDownloadSpeedLimit() : 0);
            }

            public void onConfigValidatorError(KeyHandler<Integer> keyHandler, Integer invalidValue, ValidationException validateException) {
            }
        }, false);
        this.dsm.setLimit(config.isDownloadSpeedLimitEnabled() ? config.getDownloadSpeedLimit() : 0);
        /* speed limiter enabled? */
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }

            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                dsm.setLimit(config.isDownloadSpeedLimitEnabled() ? config.getDownloadSpeedLimit() : 0);
            }
        }, false);
        /* changes in max simultan downloads */
        org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS.getEventSender().addListener(new GenericConfigEventListener<Integer>() {

            @Override
            public void onConfigValueModified(KeyHandler<Integer> keyHandler, Integer newValue) {
                enqueueJob(new DownloadWatchDogJob() {

                    @Override
                    public void execute(DownloadSession currentSession) {
                        currentSession.refreshCandidates();
                    }

                    @Override
                    public void interrupt() {
                    }
                });
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Integer> keyHandler, Integer invalidValue, ValidationException validateException) {
            }
        }, false);
        /* changes in max simultan downloads per host */
        org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS_PER_HOST.getEventSender().addListener(new GenericConfigEventListener<Integer>() {

            @Override
            public void onConfigValueModified(KeyHandler<Integer> keyHandler, final Integer newValue) {
                if (org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_DOWNLOADS_PER_HOST_ENABLED.isEnabled()) {
                    enqueueJob(new DownloadWatchDogJob() {

                        @Override
                        public void execute(DownloadSession currentSession) {
                            if (newValue != null && org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_DOWNLOADS_PER_HOST_ENABLED.isEnabled()) {
                                currentSession.setMaxConcurrentDownloadsPerHost(newValue);
                            } else {
                                currentSession.setMaxConcurrentDownloadsPerHost(-1);
                            }
                            currentSession.refreshCandidates();
                        }

                        @Override
                        public void interrupt() {
                        }
                    });
                }
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Integer> keyHandler, Integer invalidValue, ValidationException validateException) {
            }
        }, false);
        /* max simultan downloads per host enabled? */
        org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_DOWNLOADS_PER_HOST_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, final Boolean newValue) {
                enqueueJob(new DownloadWatchDogJob() {

                    @Override
                    public void execute(DownloadSession currentSession) {
                        if (newValue != null && Boolean.TRUE.equals(newValue)) {
                            currentSession.setMaxConcurrentDownloadsPerHost(org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS_PER_HOST.getValue());
                        } else {
                            currentSession.setMaxConcurrentDownloadsPerHost(-1);
                        }
                        currentSession.refreshCandidates();
                    }

                    @Override
                    public void interrupt() {
                    }
                });
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        }, false);
        if (org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_DOWNLOADS_PER_HOST_ENABLED.isEnabled()) {
            initSession.setMaxConcurrentDownloadsPerHost(org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS_PER_HOST.getValue());
        }
        /* auto reconnect enabled? */
        CFG_RECONNECT.AUTO_RECONNECT_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                autoReconnectEnabled.set(Boolean.TRUE.equals(newValue));
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        }, false);
        autoReconnectEnabled.set(CFG_RECONNECT.AUTO_RECONNECT_ENABLED.isEnabled());
        /* use account enabled? */
        CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, final Boolean newValue) {
                enqueueJob(new DownloadWatchDogJob() {

                    @Override
                    public void execute(DownloadSession currentSession) {
                        currentSession.setUseAccountsEnabled(Boolean.TRUE.equals(newValue));
                    }

                    @Override
                    public void interrupt() {
                    }
                });
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        }, false);
        initSession.setUseAccountsEnabled(CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.isEnabled());
        Reconnecter.getInstance().getEventSender().addListener(new ReconnecterListener() {

            @Override
            public void onBeforeReconnect(ReconnecterEvent event) {
            }

            @Override
            public void onAfterReconnect(final ReconnecterEvent event) {
                enqueueJob(new DownloadWatchDogJob() {

                    @Override
                    public void execute(DownloadSession currentSession) {
                        if (event.getResult() == ReconnectResult.SUCCESSFUL) {
                            ProxyInfoHistory proxyInfoHistory = currentSession.getProxyInfoHistory();
                            proxyInfoHistory.validate();
                            List<WaitingSkipReasonContainer> reconnects = proxyInfoHistory.list(WaitingSkipReason.CAUSE.IP_BLOCKED, null);
                            if (reconnects != null) {
                                for (WaitingSkipReasonContainer reconnect : reconnects) {
                                    if (!reconnect.getProxyInfo().isReconnectSupported()) continue;
                                    reconnect.invalidate();
                                }
                            }
                        }
                        currentSession.compareAndSetSessionState(DownloadSession.SessionState.RECONNECT_RUNNING, DownloadSession.SessionState.NORMAL);
                    }

                    @Override
                    public void interrupt() {
                    }
                });
                if (isAutoReconnectEnabled() && Reconnecter.getFailedCounter() > 5) {
                    switch (event.getResult()) {
                    case FAILED:
                        CFG_RECONNECT.AUTO_RECONNECT_ENABLED.setValue(false);
                        UserIO.getInstance().requestMessageDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, _JDT._.jd_controlling_reconnect_Reconnector_progress_failed2());
                        break;
                    }
                }
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

    private boolean isWatchDogThread() {
        Thread current = Thread.currentThread();
        return current == currentWatchDogThread.get() || current == tempWatchDogJobThread.get();
    }

    private Thread getWatchDogThread() {
        Thread current = tempWatchDogJobThread.get();
        if (current != null) return current;
        return currentWatchDogThread.get();
    }

    public DISKSPACECHECK checkFreeDiskSpace(final File file2Root, final SingleDownloadController controller, final long diskspace) throws Exception {
        if (isWatchDogThread()) {
            return diskSpaceCheck(file2Root, controller, diskspace);
        } else {
            final NullsafeAtomicReference<Object> asyncResult = new NullsafeAtomicReference<Object>(null);
            enqueueJob(new DownloadWatchDogJob() {

                @Override
                public void execute(DownloadSession currentSession) {
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

                @Override
                public void interrupt() {
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
        /* reset skipReasons */
        DownloadController.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                DownloadController.getInstance().set(new DownloadLinkWalker() {

                    @Override
                    public void handle(DownloadLink link) {
                        link.setConditionalSkipReason(null);
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
                return null;
            }
        });
    }

    /**
     * try to force a downloadstart, will ignore maxperhost and maxdownloads limits
     */
    public void forceDownload(final List<DownloadLink> linksForce) {
        if (linksForce == null || linksForce.size() == 0) return;
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(DownloadSession currentSession) {
                if (DownloadWatchDog.this.stateMachine.isState(STOPPING_STATE)) {
                    /*
                     * controller will shutdown soon or is paused, so no sense in forcing downloads now
                     */
                }
                unSkip(linksForce);

                if (DownloadWatchDog.this.stateMachine.isStartState() || DownloadWatchDog.this.stateMachine.isFinal()) {
                    /*
                     * no downloads are running, so we will force only the selected links to get started by setting stopmark to first forced link
                     */

                    // DownloadWatchDog.this.setStopMark(linksForce.get(0));
                    DownloadWatchDog.this.startDownloads(new Runnable() {

                        @Override
                        public void run() {
                            getSession().setForcedOnlyModeEnabled(true);
                            enqueueForcedDownloads(linksForce);
                        }
                    });
                } else {
                    enqueueForcedDownloads(linksForce);
                }
            }

            @Override
            public void interrupt() {
            }
        });
    }

    private void enqueueForcedDownloads(final List<DownloadLink> linksForce) {
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(DownloadSession currentSession) {
                Set<DownloadLink> oldList = new HashSet<DownloadLink>(currentSession.getForcedLinks());
                oldList.addAll(linksForce);
                currentSession.setForcedLinks(new CopyOnWriteArrayList<DownloadLink>(sortActivationRequests(oldList)));
            }

            @Override
            public void interrupt() {
            }
        });
    }

    private List<DownloadLink> sortActivationRequests(Iterable<DownloadLink> links) {
        HashMap<Priority, java.util.List<DownloadLink>> optimizedList = new HashMap<Priority, java.util.List<DownloadLink>>();
        int count = 0;
        for (DownloadLink link : links) {
            count++;
            Priority prio = link.getPriorityEnum();
            java.util.List<DownloadLink> list = optimizedList.get(prio);
            if (list == null) {
                list = new ArrayList<DownloadLink>();
                optimizedList.put(prio, list);
            }
            list.add(link);
        }
        List<DownloadLink> newList = new ArrayList<DownloadLink>(count);
        for (Priority prio : Priority.values()) {
            java.util.List<DownloadLink> ret = optimizedList.remove(prio);
            if (ret != null) newList.addAll(ret);
        }
        return newList;
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
                            checking = new File("/media/" + folders[2]);
                        } else if ("mnt".equals(folders[1])) {
                            /* 0:/ | 1:media | 2:mounted volume */
                            checking = new File("/mnt/" + folders[2]);
                        }
                    }
                    break;
                case MAC:
                    folders = CrossSystem.getPathComponents(file);
                    if (folders.length >= 3) {
                        if ("media".equals(folders[1])) {
                            /* 0:/ | 1:media | 2:mounted volume */
                            checking = new File("/media/" + folders[2]);
                        } else if ("mnt".equals(folders[1])) {
                            /* 0:/ | 1:media | 2:mounted volume */
                            checking = new File("/mnt/" + folders[2]);
                        } else if ("Volumes".equals(folders[1])) {
                            /* 0:/ | 1:Volumes | 2:mounted volume */
                            checking = new File("/Volumes/" + folders[2]);
                        }
                    }
                    break;
                case WINDOWS:
                default:
                    if (file.getAbsolutePath().length() > 259) {
                        // old windows API does not allow pathes longer than that (this api is even used in the windows 7 explorer and other
                        // tools like ffmpeg)
                        checking = file;
                    } else {
                        folders = CrossSystem.getPathComponents(file);
                        if (folders.length > 0) {
                            String root = folders[0];
                            if (root.matches("^[a-zA-Z]{1}:\\\\$") || root.matches("^[a-zA-Z]{1}://$")) {
                                /* X:/ or X:\ */
                                checking = new File(folders[0]);
                            } else if (root.equals("\\\\")) {
                                if (folders.length >= 3) {
                                    /* \\\\computer\\folder\\ in network */
                                    checking = new File(folders[0] + folders[1] + "\\" + folders[2]);
                                }
                            }
                        }

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

    private DownloadLinkCandidate next(DownloadLinkCandidateSelector selector) {
        DownloadSession currentSession = selector.getSession();
        candidateLoop: while (newDLStartAllowed(currentSession)) {
            List<DownloadLinkCandidate> allCandidates = allPossibleDownloadLinkCandidates(selector);
            if (allCandidates == null || allCandidates.size() == 0) return null;
            List<DownloadLinkCandidate> possibleCandidates = new ArrayList<DownloadLinkCandidate>();
            if (validateDestination(new File(allCandidates.get(0).getLink().getFilePackage().getDownloadDirectory())) != null) {
                for (DownloadLinkCandidate candidate : allCandidates) {
                    selector.addExcluded(candidate, new DownloadLinkCandidateResult(SkipReason.INVALID_DESTINATION));
                }
                continue candidateLoop;
            } else {
                DownloadLink dlLink = allCandidates.get(0).getLink();
                String dlFolder = dlLink.getFilePackage().getDownloadDirectory();
                DISKSPACECHECK result = diskSpaceCheck(new File(dlFolder), null, (dlLink.getDownloadSize() - dlLink.getDownloadCurrent()));
                switch (result) {
                case INVALIDFOLDER:
                    for (DownloadLinkCandidate candidate : allCandidates) {
                        selector.addExcluded(candidate, new DownloadLinkCandidateResult(SkipReason.INVALID_DESTINATION));
                    }
                    continue candidateLoop;
                case FAILED:
                    for (DownloadLinkCandidate candidate : allCandidates) {
                        selector.addExcluded(candidate, new DownloadLinkCandidateResult(SkipReason.DISK_FULL));
                    }
                    continue candidateLoop;
                }
            }
            for (DownloadLinkCandidate candidate : allCandidates) {
                if (candidate.getCachedAccount().hasCaptcha(candidate.getLink()) && CaptchaBlackList.getInstance().matches(new PrePluginCheckDummyChallenge(candidate.getLink()))) {
                    selector.addExcluded(candidate, new DownloadLinkCandidateResult(SkipReason.CAPTCHA));
                } else {
                    List<ProxyInfo> proxies = null;
                    if (selector.isDownloadLinkCandidateAllowed(candidate)) {
                        DownloadLink link = candidate.getLink();
                        switch (candidate.getCachedAccount().getType()) {
                        case ORIGINAL:
                        case MULTI:
                            Account acc = candidate.getCachedAccount().getAccount();
                            proxies = ProxyController.getInstance().getPossibleProxies(acc.getHoster(), true, candidate.getCachedAccount().getPlugin().getMaxSimultanDownload(link, acc));
                            break;
                        case NONE:
                            proxies = ProxyController.getInstance().getPossibleProxies(link.getHost(), false, candidate.getCachedAccount().getPlugin().getMaxSimultanDownload(link, null));
                            break;
                        }
                    }
                    if (proxies != null && proxies.size() > 0) {
                        for (ProxyInfo proxy : proxies) {
                            candidate = new DownloadLinkCandidate(candidate, getReplacement(proxy));
                            if (selector.validateDownloadLinkCandidate(candidate)) {
                                possibleCandidates.add(candidate);
                            }
                        }
                    } else {
                        if (selector.validateDownloadLinkCandidate(candidate)) {
                            selector.addExcluded(candidate, new DownloadLinkCandidateResult(RESULT.CONNECTION_UNAVAILABLE));
                        }
                    }
                }
            }
            DownloadLinkCandidate finalCandidate = findFinalCandidate(selector, possibleCandidates);
            if (finalCandidate != null) {
                selector.setExcluded(finalCandidate.getLink());
                MirrorLoading condition = new MirrorLoading(finalCandidate.getLink());
                for (DownloadLink mirror : findDownloadLinkMirrors(finalCandidate.getLink())) {
                    selector.setExcluded(mirror);
                    if (mirror.getFinalLinkState() == null || FinalLinkState.CheckFailed(mirror.getFinalLinkState())) mirror.setConditionalSkipReason(condition);
                }
                return finalCandidate;
            }
        }
        return null;
    }

    private DownloadLinkCandidate findFinalCandidate(DownloadLinkCandidateSelector selector, final List<DownloadLinkCandidate> candidates) {
        if (candidates == null || candidates.size() == 0) return null;
        Iterator<DownloadLinkCandidate> it = getCandidateIterator(selector, candidates);
        DownloadLinkCandidate ret = null;
        while (it.hasNext()) {
            DownloadLinkCandidate next = it.next();
            switch (next.getCachedAccount().getType()) {
            case MULTI:
            case ORIGINAL:
                return next;
            case NONE:
                if (ret == null) {
                    ret = next;
                } else if (ret.getCachedAccount().hasCaptcha(ret.getLink()) && !next.getCachedAccount().hasCaptcha(next.getLink())) {
                    ret = next;
                }
                break;
            }
        }
        return ret;
    }

    private Iterator<DownloadLinkCandidate> getCandidateIterator(DownloadLinkCandidateSelector selector, final List<DownloadLinkCandidate> candidates) {
        LinkedHashMap<String, LinkedHashMap<DownloadLink, List<DownloadLinkCandidate>>> bestCandidatesMap = new LinkedHashMap<String, LinkedHashMap<DownloadLink, List<DownloadLinkCandidate>>>();
        for (DownloadLinkCandidate possibleCandidate : candidates) {
            String host = possibleCandidate.getLink().getHost();
            LinkedHashMap<DownloadLink, List<DownloadLinkCandidate>> map = bestCandidatesMap.get(host);
            if (map == null) {
                map = new LinkedHashMap<DownloadLink, List<DownloadLinkCandidate>>();
                bestCandidatesMap.put(host, map);
            }
            List<DownloadLinkCandidate> list = map.get(possibleCandidate.getLink());
            if (list == null) {
                list = new ArrayList<DownloadLinkCandidate>();
                map.put(possibleCandidate.getLink(), list);
            }
            list.add(possibleCandidate);
        }
        List<DownloadLinkCandidate> ret = new ArrayList<DownloadLinkCandidate>();
        while (!bestCandidatesMap.isEmpty()) {
            Iterator<Entry<String, LinkedHashMap<DownloadLink, List<DownloadLinkCandidate>>>> it = bestCandidatesMap.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, LinkedHashMap<DownloadLink, List<DownloadLinkCandidate>>> next = it.next();
                LinkedHashMap<DownloadLink, List<DownloadLinkCandidate>> value = next.getValue();
                if (value.isEmpty()) {
                    it.remove();
                } else {
                    Iterator<Entry<DownloadLink, List<DownloadLinkCandidate>>> it2 = value.entrySet().iterator();
                    linkLoop: while (it2.hasNext()) {
                        Entry<DownloadLink, List<DownloadLinkCandidate>> next2 = it2.next();
                        List<DownloadLinkCandidate> value2 = next2.getValue();
                        if (value2.isEmpty()) {
                            it2.remove();
                        } else {
                            Iterator<DownloadLinkCandidate> it3 = value2.iterator();
                            while (it3.hasNext()) {
                                DownloadLinkCandidate next3 = it3.next();
                                ret.add(next3);
                                it3.remove();
                                continue linkLoop;
                            }
                        }
                    }
                }
            }
        }
        return ret.iterator();
    }

    private boolean isMirrorCandidate(DownloadLink linkCandidate, String cachedLinkCandidateName, DownloadLink mirrorCandidate) {
        if (cachedLinkCandidateName == null) cachedLinkCandidateName = linkCandidate.getName();
        Boolean sameSizeResult = null;
        final boolean sameName;
        if (CrossSystem.isWindows() || config.isForceMirrorDetectionCaseInsensitive()) {
            sameName = cachedLinkCandidateName.equalsIgnoreCase(mirrorCandidate.getName());
        } else {
            sameName = cachedLinkCandidateName.equals(mirrorCandidate.getName());
        }
        switch (config.getMirrorDetectionDecision()) {
        case AUTO:
            Boolean sameHashResult = hasSameHash(linkCandidate, mirrorCandidate);
            if (sameHashResult != null) return sameHashResult;
            sameSizeResult = hasSameSize(linkCandidate, mirrorCandidate);
            if (sameSizeResult != null && Boolean.FALSE.equals(sameSizeResult)) return false;
            return sameName;
        case FILENAME:
            return sameName;
        case FILENAME_FILESIZE:
            sameSizeResult = hasSameSize(linkCandidate, mirrorCandidate);
            return sameName && sameSizeResult != null && Boolean.TRUE.equals(sameSizeResult);
        }
        return false;
    }

    private Boolean hasSameSize(DownloadLink linkCandidate, DownloadLink mirrorCandidate) {
        int fileSizeEquality = config.getMirrorDetectionFileSizeEquality();
        long sizeA = linkCandidate.getVerifiedFileSize();
        long sizeB = mirrorCandidate.getVerifiedFileSize();
        if (fileSizeEquality == 10000) {
            /* 100 percent sure, only use verifiedFileSizes */
            if (sizeA >= 0 && sizeB >= 0) return sizeA == sizeB;
        } else {
            /* we use knownDownloadSize for check */
            sizeA = linkCandidate.getKnownDownloadSize();
            sizeB = mirrorCandidate.getKnownDownloadSize();
            if (sizeA >= 0 && sizeB >= 0) {
                long diff = Math.abs(sizeA - sizeB);
                int maxDiffPercent = 10000 - fileSizeEquality;
                long maxDiff = (sizeA * maxDiffPercent) / 10000;
                return diff <= maxDiff;
            }
        }
        return null;
    }

    private Boolean hasSameHash(DownloadLink linkCandidate, DownloadLink mirrorCandidate) {
        String hashA = linkCandidate.getMD5Hash();
        String hashB = mirrorCandidate.getMD5Hash();
        if (hashA != null && hashB != null) { return hashA.equalsIgnoreCase(hashB); }
        hashA = linkCandidate.getSha1Hash();
        hashB = mirrorCandidate.getSha1Hash();
        if (hashA != null && hashB != null) { return hashA.equalsIgnoreCase(hashB); }
        return null;
    }

    private List<DownloadLink> findDownloadLinkMirrors(final DownloadLink link) {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final FilePackage fp = link.getFilePackage();
        final String name = link.getName();
        fp.getModifyLock().runReadLock(new Runnable() {

            @Override
            public void run() {
                for (DownloadLink mirror : fp.getChildren()) {
                    if (mirror == link) continue;
                    if (isMirrorCandidate(link, name, mirror)) {
                        ret.add(mirror);
                    }
                }
            }
        });
        return ret;
    }

    private void setFinalLinkStatus(DownloadLinkCandidate candidate, DownloadLinkCandidateResult value, SingleDownloadController singleDownloadController) {
        DownloadSession currentSession = getSession();
        DownloadLink link = candidate.getLink();
        final boolean onDetach = singleDownloadController != null;
        HashResult hashResult = null;
        if (onDetach) hashResult = singleDownloadController.getHashResult();
        switch (value.getResult()) {
        case CONDITIONAL_SKIPPED:
            logger.info(candidate + "->" + value.getResult() + "|" + value.getConditionalSkip());
            break;
        case SKIPPED:
            logger.info(candidate + "->" + value.getResult() + "|" + value.getSkipReason());
            break;
        default:
            logger.info(candidate + "->" + value.getResult());
            break;
        }
        ProxyInfoHistory proxyHistory = currentSession.getProxyInfoHistory();
        switch (value.getResult()) {
        case PROXY_UNAVAILABLE:
            if (!onDetach) {
                ConditionalSkipReason conditionalSkipReason = proxyHistory.getConditionalSkipReason(candidate);
                if (conditionalSkipReason != null) link.setConditionalSkipReason(conditionalSkipReason);
                return;
            }
            break;
        case CONDITIONAL_SKIPPED:
            ConditionalSkipReason conditionalSkipReason = value.getConditionalSkip();
            if (conditionalSkipReason != null) link.setConditionalSkipReason(conditionalSkipReason);
            return;
        case IP_BLOCKED:
            if (onDetach) {
                long remainingTime = value.getRemainingTime();
                if (remainingTime > 0) proxyHistory.putIntoHistory(candidate, new WaitingSkipReason(CAUSE.IP_BLOCKED, remainingTime, value.getMessage()));
                return;
            }
            break;
        case HOSTER_UNAVAILABLE:
            if (onDetach) {
                long remainingTime = value.getRemainingTime();
                if (remainingTime > 0) proxyHistory.putIntoHistory(candidate, new WaitingSkipReason(CAUSE.HOST_TEMP_UNAVAILABLE, remainingTime, value.getMessage()));
                return;
            }
            break;
        case FILE_UNAVAILABLE:
            if (!onDetach) {
                long remaining = value.getRemainingTime();
                if (remaining > 0) link.setConditionalSkipReason(new WaitingSkipReason(CAUSE.FILE_TEMP_UNAVAILABLE, remaining, value.getMessage()));
            }
            return;
        case CONNECTION_ISSUES:
            if (!onDetach) {
                long remaining = value.getRemainingTime();
                if (remaining > 0) link.setConditionalSkipReason(new WaitingSkipReason(CAUSE.CONNECTION_TEMP_UNAVAILABLE, remaining, value.getMessage()));
            }
            return;
        case SKIPPED:
            currentSession.removeHistory(link);
            candidate.getLink().setSkipReason(value.getSkipReason());
            return;
        case PLUGIN_DEFECT:
            if (!onDetach) {
                currentSession.removeHistory(link);
                candidate.getLink().setFinalLinkState(FinalLinkState.PLUGIN_DEFECT);
            }
            return;
        case OFFLINE_UNTRUSTED:
            if (!onDetach) {
                currentSession.removeHistory(link);
                candidate.getLink().setFinalLinkState(FinalLinkState.OFFLINE);
            }
            return;
        case OFFLINE_TRUSTED:
            if (onDetach) {
                currentSession.removeHistory(link);
                candidate.getLink().setFinalLinkState(FinalLinkState.OFFLINE);
                return;
            }
            break;
        case FINISHED_EXISTS:
            if (onDetach) {
                currentSession.removeHistory(link);
                candidate.getLink().setFinishedDate(value.getFinishTime());
                candidate.getLink().setFinalLinkState(FinalLinkState.FINISHED_MIRROR);
                return;
            }
            break;
        case FINISHED:
            if (onDetach) {
                currentSession.removeHistory(link);
                candidate.getLink().setFinishedDate(value.getFinishTime());
                if (hashResult != null) {
                    switch (hashResult.getType()) {
                    case CRC32:
                        candidate.getLink().setFinalLinkState(FinalLinkState.FINISHED_CRC32);
                        break;
                    case MD5:
                        candidate.getLink().setFinalLinkState(FinalLinkState.FINISHED_MD5);
                        break;
                    case SHA1:
                        candidate.getLink().setFinalLinkState(FinalLinkState.FINISHED_SHA1);
                        break;
                    }
                } else {
                    candidate.getLink().setFinalLinkState(FinalLinkState.FINISHED);
                }
                return;
            }
            break;
        case FAILED_EXISTS:
            if (onDetach) {
                currentSession.removeHistory(link);
                candidate.getLink().setFinalLinkState(FinalLinkState.FAILED_EXISTS);
                return;
            }
            break;
        case FAILED:
            if (onDetach) {
                currentSession.removeHistory(link);
                if (hashResult != null) {
                    switch (hashResult.getType()) {
                    case CRC32:
                        candidate.getLink().setFinalLinkState(FinalLinkState.FAILED_CRC32);
                        break;
                    case MD5:
                        candidate.getLink().setFinalLinkState(FinalLinkState.FAILED_MD5);
                        break;
                    case SHA1:
                        candidate.getLink().setFinalLinkState(FinalLinkState.FAILED_SHA1);
                        break;
                    }
                } else {
                    candidate.getLink().setFinalLinkState(FinalLinkState.FAILED);
                }
                return;
            }
            break;
        case STOPPED:
            if (onDetach) {
                currentSession.removeHistory(link);
                return;
            }
            break;
        case ACCOUNT_INVALID:
            if (onDetach) {
                candidate.getCachedAccount().getAccount().setValid(false);
                return;
            }
            break;
        case ACCOUNT_UNAVAILABLE:
            if (onDetach) {
                candidate.getCachedAccount().getAccount().setTempDisabled(true);
                return;
            }
            break;
        case ACCOUNT_REQUIRED:
            if (!onDetach) {
                currentSession.removeHistory(link);
                candidate.getLink().setSkipReason(SkipReason.NO_ACCOUNT);
            }
            return;
        case CAPTCHA:
            if (!onDetach) {
                currentSession.removeHistory(link);
                candidate.getLink().setSkipReason(SkipReason.CAPTCHA);
            }
            return;
        case FATAL_ERROR:
            if (!onDetach) {
                currentSession.removeHistory(link);
                candidate.getLink().setFinalLinkState(FinalLinkState.FAILED_FATAL);
                candidate.getLink().setProperty(DownloadLink.PROPERTY_CUSTOM_MESSAGE, value.getMessage());
            }
            return;
        default:
            if (onDetach) {
                /* todo */
                logger.info("TODO: " + value.getResult());
                return;
            }
            break;
        }
        throw new WTFException("This should never happen!? " + value.getResult());
    }

    private List<DownloadLinkCandidate> allPossibleDownloadLinkCandidates(DownloadLinkCandidateSelector selector) {
        DownloadLinkCandidate candidate = null;
        List<DownloadLinkCandidate> mirrors = new ArrayList<DownloadLinkCandidate>();
        List<DownloadLinkCandidate> possible = new ArrayList<DownloadLinkCandidate>();
        DownloadSession currentSession = selector.getSession();
        while (newDLStartAllowed(currentSession)) {
            candidate = nextDownloadLinkCandidate(selector);
            if (candidate == null) return null;
            possible.clear();
            possible.add(candidate);
            if (!candidate.isForced() && selector.isMirrorManagement()) {
                possible.addAll(findDownloadLinkCandidateMirrors(selector, candidate));
            }
            if (mirrors.size() > 0) mirrors.clear();
            for (DownloadLinkCandidate mirror : possible) {
                selector.addExcluded(mirror.getLink());
                AccountCache accountCache = currentSession.getAccountCache(mirror.getLink());
                Iterator<CachedAccount> it = accountCache.iterator();
                boolean cachedAccountFound = false;
                boolean cachedAccountUsed = false;
                CachedAccount tempDisabledCachedAccount = null;
                while (it.hasNext()) {
                    CachedAccount cachedAccount = it.next();
                    /* this cachedAccount can handle our candidate */
                    switch (selector.getCachedAccountPermission(cachedAccount)) {
                    case TEMP_DISABLED:
                        if (tempDisabledCachedAccount == null) {
                            tempDisabledCachedAccount = cachedAccount;
                        } else if (tempDisabledCachedAccount.getAccount().getTmpDisabledTimeout() > cachedAccount.getAccount().getTmpDisabledTimeout()) {
                            tempDisabledCachedAccount = cachedAccount;
                        }
                        cachedAccountFound = true;
                        break;
                    case FORBIDDEN:
                        break;
                    case DISABLED:
                        break;
                    case OK:
                        cachedAccountFound = true;
                        if (cachedAccount.canHandle(mirror.getLink())) {
                            cachedAccountUsed = true;
                            mirrors.add(new DownloadLinkCandidate(mirror, cachedAccount));
                        }
                        break;
                    }
                }
                if (cachedAccountFound == false) {
                    /* even NONE cannot handle our candidate, so let's skip it */
                    selector.addExcluded(mirror, new DownloadLinkCandidateResult(SkipReason.NO_ACCOUNT));
                } else if (cachedAccountUsed == false) {
                    if (tempDisabledCachedAccount != null) selector.addExcluded(mirror, new DownloadLinkCandidateResult(new WaitForAccountSkipReason(tempDisabledCachedAccount.getAccount())));
                }
            }
            if (mirrors.size() > 0) { return mirrors; }
        }
        return null;
    }

    private List<DownloadLinkCandidate> findDownloadLinkCandidateMirrors(DownloadLinkCandidateSelector selector, DownloadLinkCandidate candidate) {
        List<DownloadLinkCandidate> ret = new ArrayList<DownloadLinkCandidate>();
        if (candidate.isForced()) return ret;
        DownloadLink link = candidate.getLink();
        FilePackage filePackage = link.getFilePackage();
        String name = link.getName();
        ArrayList<DownloadLink> removeList = new ArrayList<DownloadLink>();
        for (DownloadLink next : getSession().getActivationRequests()) {
            if (next == link) continue;
            if (next.getFilePackage() != filePackage) {
                /* AT THE MOMENT the mirror must be in the same package */
                continue;
            }
            if (canIgnore(selector, next, false)) {
                continue;
            }
            if (canRemove(next, false)) {
                next.setConditionalSkipReason(null);
                removeList.add(next);
                continue;
            }
            if (isMirrorCandidate(link, name, next)) {
                ret.add(new DownloadLinkCandidate(next, false));
            }
        }
        getSession().removeActivationRequests(removeList);
        return ret;
    }

    private boolean canRemove(DownloadLink next, boolean isForced) {
        if (next.getDefaultPlugin() == null) {
            /* download has no plugin, we can remove it from downloadLinks because we will never be able to handle it */
            return true;
        }
        if (FilePackage.isDefaultFilePackage(next.getFilePackage())) {
            /* download has no longer a valid FilePackage because it got removed, we can remove it from downloadLinks as well */
            return true;
        }
        if (next.getAvailableStatus() == AvailableStatus.FALSE) return true;
        if (next.getFinalLinkState() != null) return true;
        if (next.isSkipped()) {
            /* download is skipped, remove it from downloadLinks until it is no longer skipped */
            return true;
        }
        if (!isForced && !next.isEnabled()) {
            /* ONLY when not forced */
            /* link is disabled, lets skip it */
            return true;
        }
        return false;
    }

    private boolean canIgnore(DownloadLinkCandidateSelector selector, DownloadLink next, boolean isForced) {
        if (!selector.isDownloadLinkCandidateAllowed(new DownloadLinkCandidate(next, isForced))) { return true; }
        if (next.getDownloadLinkController() != null) {
            /* download is in progress */
            return true;
        }
        ConditionalSkipReason conditionalSkipReason = next.getConditionalSkipReason();
        if (conditionalSkipReason != null) {
            if (conditionalSkipReason instanceof IgnorableConditionalSkipReason && ((IgnorableConditionalSkipReason) conditionalSkipReason).canIgnore()) return true;
            if (!(conditionalSkipReason instanceof ValidatableConditionalSkipReason) || ((ValidatableConditionalSkipReason) conditionalSkipReason).isValid()) return true;
        }
        return false;
    }

    private DownloadLinkCandidate nextDownloadLinkCandidate(DownloadLinkCandidateSelector selector) {
        ArrayList<DownloadLink> removeList = new ArrayList<DownloadLink>();
        try {
            DownloadSession currentSession = selector.getSession();
            HashSet<DownloadLink> avoidReHandle = new HashSet<DownloadLink>();
            List<List<DownloadLink>> activationRequests = new ArrayList<List<DownloadLink>>(2);
            if (currentSession.isForcedLinksWaiting()) {
                activationRequests.add(currentSession.getForcedLinks());
            }
            if (selector.isForcedOnly() == false && currentSession.isForcedOnlyModeEnabled() == false) {
                activationRequests.add(currentSession.getActivationRequests());
            }
            for (List<DownloadLink> activationRequest : activationRequests) {
                activationRequest.removeAll(removeList);
                boolean isForced = activationRequest == currentSession.getForcedLinks();
                try {
                    Iterator<DownloadLink> it = activationRequest.iterator();
                    while (it.hasNext()) {
                        DownloadLink next = it.next();
                        if (selector.isExcluded(next)) continue;
                        if (!avoidReHandle.add(next)) continue;
                        if (canIgnore(selector, next, isForced)) {
                            selector.addExcluded(next);
                            continue;
                        }
                        if (canRemove(next, isForced)) {
                            next.setConditionalSkipReason(null);
                            removeList.add(next);
                            continue;
                        }
                        return new DownloadLinkCandidate(next, isForced);
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
            if (p != null) return new ProxyInfo(p);
        } catch (Exception e) {
            logger.log(e);
        }
        return proxy;
    }

    /**
     * returns current pause state
     * 
     * @return
     */
    public boolean isPaused() {
        return DownloadWatchDog.this.stateMachine.isState(PAUSE_STATE);
    }

    private boolean isAutoReconnectEnabled() {
        return autoReconnectEnabled.get();
    }

    /**
     * may the DownloadWatchDog start new Downloads?
     * 
     * @return
     */
    private boolean newDLStartAllowed(DownloadSession session) {
        if (ShutdownController.getInstance().isShutDownRequested()) {
            /* shutdown is requested, we do not start new downloads */
            return false;
        }
        SessionState sessionState = session.getSessionState();
        if (sessionState == DownloadSession.SessionState.RECONNECT_RUNNING) {
            /* reconnect in progress */
            return false;
        }
        boolean isForcedLinksWaiting = getSession().isForcedLinksWaiting();
        if (isForcedLinksWaiting == false && !DownloadWatchDog.this.stateMachine.isState(DownloadWatchDog.RUNNING_STATE)) {
            /*
             * only allow new downloads in running state
             */
            return false;
        }
        if (isForcedLinksWaiting == false && isAutoReconnectEnabled() && CFG_RECONNECT.CFG.isDownloadControllerPrefersReconnectEnabled() && sessionState == DownloadSession.SessionState.RECONNECT_REQUESTED) {
            /*
             * auto reconnect is enabled and downloads are waiting for reconnect and user set to wait for reconnect
             */
            return false;
        }
        return true;
    }

    protected void setTempWatchDogJobThread(Thread thread) {
        tempWatchDogJobThread.set(thread);
    }

    /**
     * pauses the DownloadWatchDog
     * 
     * @param value
     */

    private Object                                     shutdownLock = new Object();
    protected NullsafeAtomicReference<DownloadSession> session      = new NullsafeAtomicReference<DownloadSession>(null);

    public void pauseDownloadWatchDog(final boolean value) {
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(DownloadSession currentSession) {
                if (value && !stateMachine.isState(DownloadWatchDog.RUNNING_STATE)) {
                    /* we can only pause downloads when downloads are running */
                    return;
                }
                if (value) {
                    /* set pause settings */
                    currentSession.setSpeedLimitBeforePause(config.getDownloadSpeedLimit());
                    currentSession.setSpeedWasLimitedBeforePauseEnabled(config.isDownloadSpeedLimitEnabled());
                    config.setDownloadSpeedLimit(config.getPauseSpeed());
                    config.setDownloadSpeedLimitEnabled(true);
                    logger.info("Pause enabled: Reducing downloadspeed to " + config.getPauseSpeed() + " KiB/s");
                    /* pause downloads */
                    stateMachine.setStatus(PAUSE_STATE);
                } else {
                    /* revert pause settings if available */
                    if (currentSession.getSpeedLimitBeforePause() > 0) {
                        logger.info("Pause disabled: Switch back to old downloadspeed");
                        config.setDownloadSpeedLimit(currentSession.getSpeedLimitBeforePause());
                    }
                    Boolean beforeEnabled = currentSession.isSpeedWasLimitedBeforePauseEnabled();
                    if (beforeEnabled != null) config.setDownloadSpeedLimitEnabled(beforeEnabled);
                    if (stateMachine.isState(DownloadWatchDog.PAUSE_STATE)) {
                        /* we revert pause to running state */
                        stateMachine.setStatus(RUNNING_STATE);
                    }
                }
            }

            @Override
            public void interrupt() {
            }
        });
    }

    public void notifyCurrentState(final DownloadWatchdogListener listener) {
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(DownloadSession currentSession) {
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
            }

            @Override
            public void interrupt() {
            }
        });
    }

    public long getDownloadSpeedbyFilePackage(FilePackage pkg) {
        long speed = -1;
        for (SingleDownloadController con : getSession().getControllers()) {
            if (con.getDownloadLink().getFilePackage() != pkg) continue;
            DownloadInterface dli = con.getDownloadInstance();
            if (dli == null) continue;
            speed += dli.getManagedConnetionHandler().getSpeed();
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
        if (isWatchDogThread()) throw new WTFException("it is not possible to use this method inside WatchDogThread!");
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
        if (!isWatchDogThread()) {
            wakeUpWatchDog(true);
        } else {
            wakeUpWatchDog(false);
        }
    }

    public void resume(final List<DownloadLink> resetLinks) {
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(DownloadSession currentSession) {
                for (DownloadLink link : resetLinks) {
                    if (link.getDownloadLinkController() == null && (FinalLinkState.CheckFailed(link.getFinalLinkState()) || link.isSkipped())) {
                        resumeLink(link, currentSession);
                    }
                }
                return;
            }

            @Override
            public void interrupt() {
            }
        });
    }

    public void unSkip(final List<DownloadLink> resetLinks) {
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(DownloadSession currentSession) {
                for (DownloadLink link : resetLinks) {
                    if (link.getDownloadLinkController() == null && link.isSkipped()) {
                        unSkipLink(link, currentSession);
                    }
                }
                return;
            }

            @Override
            public void interrupt() {
            }
        });
    }

    public void reset(final List<DownloadLink> resetLinks) {
        if (resetLinks == null || resetLinks.size() == 0) return;
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(DownloadSession currentSession) {
                if (currentSession != null) {
                    currentSession.getActivationRequests().removeAll(resetLinks);
                    currentSession.getForcedLinks().removeAll(resetLinks);
                }
                for (final DownloadLink link : resetLinks) {
                    SingleDownloadController con = link.getDownloadLinkController();
                    if (con == null) {
                        /* link has no/no alive singleDownloadController, so reset it now */
                        resetLink(link, currentSession);
                    } else {
                        /* link has a running singleDownloadController, abort it and reset it after */
                        con.getJobsAfterDetach().add(new DownloadWatchDogJob() {

                            @Override
                            public void interrupt() {
                            }

                            @Override
                            public void execute(DownloadSession currentSession) {
                                /* now we can reset the link */
                                resetLink(link, currentSession);
                            }
                        });
                        con.abort();
                    }
                }
                return;
            }

            @Override
            public void interrupt() {
            }
        });
    }

    private void resumeLink(DownloadLink link, DownloadSession session) {
        if (link.getDownloadLinkController() != null) throw new IllegalStateException("Link is in progress! cannot resume!");
        if (!FinalLinkState.CheckFinished(link.getFinalLinkState())) {
            if (FinalLinkState.CheckFailed(link.getFinalLinkState())) link.setFinalLinkState(null);
            unSkipLink(link, session);
            if (!link.isEnabled()) link.setEnabled(true);
        }
        link.resume();
    }

    private void unSkipLink(DownloadLink link, DownloadSession session) {
        if (link.isSkipped()) {
            link.setSkipReason(null);
        }
        CaptchaBlackList.getInstance().addWhitelist(link);
    }

    private void resetLink(DownloadLink link, DownloadSession session) {
        if (link.getDownloadLinkController() != null) throw new IllegalStateException("Link is in progress! cannot reset!");
        session.removeHistory(link);
        List<WaitingSkipReasonContainer> list = session.getProxyInfoHistory().list(link.getHost());
        if (list != null) {
            for (WaitingSkipReasonContainer container : list) {
                container.invalidate();
            }
        }
        deleteFile(link, DeleteOption.NULL);
        unSkipLink(link, session);
        link.reset();
    }

    public void delete(final List<DownloadLink> deleteFiles, final DeleteOption deleteTo) {
        if (deleteFiles == null || deleteFiles.size() == 0) return;
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(DownloadSession currentSession) {
                if (currentSession != null) {
                    currentSession.getActivationRequests().removeAll(deleteFiles);
                    currentSession.getForcedLinks().removeAll(deleteFiles);
                }
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
                            public void execute(DownloadSession currentSession) {
                                if (currentSession != null) {
                                    currentSession.getActivationRequests().remove(link);
                                    currentSession.getForcedLinks().remove(link);
                                }
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

                            @Override
                            public void interrupt() {
                            }
                        });
                    }
                }
                return;
            }

            @Override
            public void interrupt() {
            }
        });
    }

    private void deleteFile(DownloadLink link, DeleteOption deleteTo) {
        if (deleteTo == null) deleteTo = DeleteOption.NULL;
        if (DeleteOption.NO_DELETE == deleteTo) return;
        ArrayList<File> deleteFiles = new ArrayList<File>();
        try {
            for (File deleteFile : link.getDefaultPlugin().deleteDownloadLink(link)) {
                if (deleteFile.exists() && deleteFile.isFile()) {
                    try {
                        getSession().getFileAccessManager().lock(deleteFile, this);
                        deleteFiles.add(deleteFile);
                    } catch (FileIsLockedException e) {
                        logger.log(e);
                    }
                }
            }
            link.setFinalFileOutput(null);
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
            File dlFolder = new File(link.getDownloadDirectory());
            if (dlFolder != null && dlFolder.exists() && dlFolder.isDirectory() && dlFolder.listFiles() != null && dlFolder.listFiles().length == 0) {
                if (!new File(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder()).equals(dlFolder)) {
                    if (!dlFolder.delete()) {
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
        DownloadLinkCandidateSelector selector = new DownloadLinkCandidateSelector(session);
        /* avoid captchas or not ? */
        HashSet<DownloadLink> finalLinkStateLinks = finalizeConditionalSkipReasons(session);
        handleFinalLinkStates(finalLinkStateLinks, session, logger, null);
        int maxConcurrentNormal = Math.max(1, config.getMaxSimultaneDownloads());
        int maxConcurrentForced = maxConcurrentNormal + Math.max(0, config.getMaxForcedDownloads());
        int loopCounter = 0;
        boolean invalidate = session.setCandidatesRefreshRequired(false);
        boolean abort = false;
        try {
            if (invalidate) {
                for (DownloadLink candidate : new ConcatIterator<DownloadLink>(session.getForcedLinks().iterator(), session.getActivationRequests().iterator())) {
                    ConditionalSkipReason con = candidate.getConditionalSkipReason();
                    if (con != null && con instanceof ValidatableConditionalSkipReason) {
                        ((ValidatableConditionalSkipReason) con).invalidate();
                    }
                }
            }
            if (session.isForcedLinksWaiting()) {
                /* first process forcedLinks */
                selector.setForcedOnly(true);
                loopCounter = 0;
                while (abort == false && session.isForcedLinksWaiting() && (getActiveDownloads() < maxConcurrentForced) && loopCounter < session.getForcedLinks().size()) {
                    try {
                        if (abort || (abort = (!this.newDLStartAllowed(session)))) {
                            break;
                        }
                        DownloadLinkCandidate candidate = this.next(selector);
                        if (candidate == null) break;
                        ret.add(attach(candidate));
                    } finally {
                        loopCounter++;
                    }
                }
            }
            loopCounter = 0;
            selector.setForcedOnly(false);
            /* then process normal activationRequests */
            while (abort == false && getActiveDownloads() < maxConcurrentNormal && loopCounter < session.getActivationRequests().size()) {
                try {
                    if (abort || (abort = (!this.newDLStartAllowed(session) || session.isStopMarkReached()))) {
                        break;
                    }
                    DownloadLinkCandidate candidate = this.next(selector);
                    if (candidate == null) break;
                    ret.add(attach(candidate));
                } finally {
                    loopCounter++;
                }
            }
        } finally {
            finalize(selector);
        }
        return ret;
    }

    private void finalize(DownloadLinkCandidateSelector selector) {
        DownloadSession session = selector.getSession();
        for (DownloadLink candidate : new ConcatIterator<DownloadLink>(session.getForcedLinks().iterator(), session.getActivationRequests().iterator())) {
            ConditionalSkipReason con = candidate.getConditionalSkipReason();
            if (con != null && con instanceof ValidatableConditionalSkipReason && !((ValidatableConditionalSkipReason) con).isValid()) {
                candidate.setConditionalSkipReason(null);
            }
        }
        // int maxConditionalSkipReasons = Math.max(0, Math.max(1, config.getMaxSimultaneDownloads()) - getActiveDownloads());
        LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult> results = selector.finalizeDownloadLinkCandidatesResults();
        Iterator<Entry<DownloadLinkCandidate, DownloadLinkCandidateResult>> it = results.entrySet().iterator();
        while (it.hasNext()) {
            Entry<DownloadLinkCandidate, DownloadLinkCandidateResult> next = it.next();
            DownloadLinkCandidate candidate = next.getKey();
            DownloadLinkCandidateResult result = next.getValue();
            setFinalLinkStatus(candidate, result, null);
        }
    }

    public void startDownloads() {
        startDownloads(null);
    }

    /**
     * start the DownloadWatchDog
     */
    public void startDownloads(final Runnable runBeforeStartingDownloadWatchDog) {

        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(DownloadSession currentSession) {
                if (DownloadWatchDog.this.stateMachine.isFinal()) {
                    /* downloadwatchdog was in stopped state, so reset it */
                    DownloadWatchDog.this.stateMachine.reset(false);
                }
                if (!DownloadWatchDog.this.stateMachine.isStartState()) {
                    /* only allow to start when in FinalState(NOT_RUNNING) */
                    return;
                }
                /*
                 * setNewSession
                 */
                session.set(new DownloadSession(currentSession));
                if (runBeforeStartingDownloadWatchDog != null) {
                    try {
                        runBeforeStartingDownloadWatchDog.run();
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
                /* start watchdogthread */
                startDownloadWatchDog();
                /* throw start event */
                logger.info("DownloadWatchDog: start");
            }

            @Override
            public void interrupt() {
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
    private SingleDownloadController attach(final DownloadLinkCandidate candidate) {
        logger.info("Start new Download: Host:" + candidate);
        String downloadTo = candidate.getLink().getFileOutput(true, false);
        String customDownloadTo = candidate.getLink().getFileOutput(true, true);
        logger.info("Download To: " + downloadTo);
        if (!StringUtils.equalsIgnoreCase(downloadTo, customDownloadTo)) {
            logger.info("Download To(custom): " + customDownloadTo);
        }

        DownloadLinkCandidateHistory history = getSession().buildHistory(candidate.getLink());
        if (history == null || !history.attach(candidate)) {
            logger.severe("Could not attach to History: " + candidate);
        }
        final SingleDownloadController con = new SingleDownloadController(candidate, this);
        if (candidate.getProxy() != null) {
            candidate.getProxy().add(con);
        }
        candidate.getLink().setEnabled(true);
        getSession().getControllers().add(con);
        con.start();
        eventSender.fireEvent(new DownloadWatchdogEvent(this, DownloadWatchdogEvent.Type.LINK_STARTED, con));
        return con;
    }

    protected void detach(final SingleDownloadController singleDownloadController, final SingleDownloadReturnState returnState) {
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(DownloadSession currentSession) {
                LogSource logger = null;
                try {
                    logger = new LogSource("Dummy") {

                        @Override
                        public synchronized void clear() {
                        }

                        public synchronized void log(java.util.logging.LogRecord record) {
                            DownloadWatchDog.this.logger.log(record);
                        };
                    };
                    if (singleDownloadController.getLogger() instanceof LogSource) {
                        logger = (LogSource) singleDownloadController.getLogger();
                    }
                    DownloadLinkCandidate candidate = singleDownloadController.getDownloadLinkCandidate();
                    final DownloadLink link = candidate.getLink();
                    if (candidate.getProxy() != null) {
                        candidate.getProxy().remove(singleDownloadController);
                    }
                    try {
                        DownloadLinkCandidateResult result = handleReturnState(logger, singleDownloadController, returnState);
                        result.setStartTime(singleDownloadController.getStartTimestamp());
                        result.setFinishTime(returnState.getTimeStamp());
                        setFinalLinkStatus(candidate, result, singleDownloadController);
                        DownloadLinkCandidateHistory existingHistory = currentSession.getHistory(link);
                        if (existingHistory != null && !existingHistory.dettach(candidate, result)) {
                            logger.severe("Could not detach from History: " + candidate);
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                    currentSession.getControllers().remove(singleDownloadController);
                    for (DownloadWatchDogJob job : singleDownloadController.getJobsAfterDetach()) {
                        try {
                            job.execute(currentSession);
                        } catch (Throwable e) {
                            logger.log(e);
                        }
                    }
                    HashSet<DownloadLink> finalLinkStateLinks = finalizeConditionalSkipReasons(currentSession);
                    /* after each download, the order/position of next downloadCandidate could have changed */
                    currentSession.refreshCandidates();
                    singleDownloadController.getDownloadLink().getFilePackage().getView().requestUpdate();
                    eventSender.fireEvent(new DownloadWatchdogEvent(this, DownloadWatchdogEvent.Type.LINK_STOPPED, singleDownloadController));
                    handleFinalLinkStates(finalLinkStateLinks, currentSession, logger, singleDownloadController);
                } catch (final Throwable e) {
                    logger.log(e);
                } finally {
                    if (logger != null) logger.close();
                }
                return;
            }

            @Override
            public void interrupt() {
            }
        });
    }

    private void handleFinalLinkStates(HashSet<DownloadLink> links, DownloadSession session, final LogSource logger, final SingleDownloadController singleDownloadController) {
        if ((links == null || links.size() == 0) && singleDownloadController == null) return;
        if (links == null) links = new HashSet<DownloadLink>();
        DownloadLink singleDownloadControllerLink = null;
        if (singleDownloadController != null) {
            singleDownloadControllerLink = singleDownloadController.getDownloadLinkCandidate().getLink();
            links.add(singleDownloadControllerLink);
        }
        Iterator<DownloadLink> it = links.iterator();
        final CleanAfterDownloadAction cleanupAction = org.jdownloader.settings.staticreferences.CFG_GENERAL.CFG.getCleanupAfterDownloadAction();
        final boolean cleanupFileExists = JsonConfig.create(GeneralSettings.class).getCleanupFileExists();

        final ArrayList<DownloadLink> cleanupLinks = new ArrayList<DownloadLink>();
        final HashSet<FilePackage> cleanupPackages = new HashSet<FilePackage>();
        while (it.hasNext()) {
            final DownloadLink next = it.next();
            FinalLinkState state = next.getFinalLinkState();
            if (FinalLinkState.FAILED_EXISTS.equals(state)) {
                session.removeActivationRequest(next);
                session.removeHistory(next);
                it.remove();
                if (cleanupFileExists) {
                    switch (cleanupAction) {
                    case CLEANUP_IMMEDIATELY:
                        cleanupLinks.add(next);
                        break;
                    case CLEANUP_AFTER_PACKAGE_HAS_FINISHED:
                        cleanupPackages.add(next.getFilePackage());
                        break;
                    }
                }
            } else if (FinalLinkState.CheckFinished(state)) {
                session.removeActivationRequest(next);
                session.removeHistory(next);
                it.remove();
                switch (cleanupAction) {
                case CLEANUP_IMMEDIATELY:
                    cleanupLinks.add(next);
                    break;
                case CLEANUP_AFTER_PACKAGE_HAS_FINISHED:
                    cleanupPackages.add(next.getFilePackage());
                    break;
                }
                if (next == singleDownloadControllerLink) {
                    TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                        @Override
                        protected Void run() throws RuntimeException {
                            FileCreationManager.getInstance().getEventSender().fireEvent(new FileCreationEvent(singleDownloadController, FileCreationEvent.Type.NEW_FILES, new File[] { new File(next.getFileOutput()) }));
                            return null;
                        }
                    });
                }
            }
        }

        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                switch (cleanupAction) {
                case CLEANUP_IMMEDIATELY:
                    if (cleanupLinks.size() > 0) {
                        DownloadController.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

                            @Override
                            protected Void run() throws RuntimeException {
                                for (DownloadLink cleanupLink : cleanupLinks) {
                                    String name = cleanupLink.getName();
                                    logger.info("Remove Link " + name + " because " + cleanupLink.getFinalLinkState() + " and CleanupImmediately!");
                                    java.util.List<DownloadLink> remove = new ArrayList<DownloadLink>();
                                    remove.add(cleanupLink);
                                    if (DownloadController.getInstance().askForRemoveVetos(remove)) {
                                        DownloadController.getInstance().removeChildren(remove);
                                    } else {
                                        logger.info("Remove Link " + name + " failed because of removeVetos!");
                                    }
                                }
                                return null;
                            }

                        });
                    }
                    break;
                case CLEANUP_AFTER_PACKAGE_HAS_FINISHED:
                    if (cleanupPackages.size() > 0) {
                        for (FilePackage filePackage : cleanupPackages) {
                            DownloadController.removePackageIfFinished(logger, filePackage);
                        }
                    }
                    break;
                }
                return null;
            }
        });
    }

    private DownloadLinkCandidateResult handleReturnState(LogSource logger, SingleDownloadController singleDownloadController, SingleDownloadReturnState result) {
        Throwable throwable = result.getCaughtThrowable();
        DownloadLinkCandidate candidate = singleDownloadController.getDownloadLinkCandidate();
        DownloadLink link = candidate.getLink();
        long sizeChange = Math.max(0, link.getDownloadCurrent() - singleDownloadController.getSizeBefore());
        Account account = singleDownloadController.getAccount();
        if (account != null && sizeChange > 0) {
            /* updates traffic available for used account */
            final AccountInfo ai = account.getAccountInfo();
            if (ai != null && !ai.isUnlimitedTraffic()) {
                long left = Math.max(0, ai.getTrafficLeft() - sizeChange);
                ai.setTrafficLeft(left);
                if (left == 0) {
                    if (ai.isSpecialTraffic()) {
                        logger.severe("Account: " + account.getUser() + ": Traffic Limit could be reached, but SpecialTraffic might be available!");
                    } else {
                        logger.severe("Account: " + account.getUser() + ": Traffic Limit reached");
                        account.setTempDisabled(true);
                    }
                }
            }
        }
        SkipReason skipReason = null;
        PluginException pluginException = null;
        ConditionalSkipReason conditionalSkipReason = null;
        String pluginHost = null;
        PluginForHost latestPlugin = null;
        if ((latestPlugin = result.getLatestPlugin()) != null) {
            pluginHost = latestPlugin.getLazyP().getHost();
        }
        if (throwable != null) {
            if (throwable instanceof PluginException) {
                pluginException = (PluginException) throwable;
            } else if (throwable instanceof SkipReasonException) {
                skipReason = ((SkipReasonException) throwable).getSkipReason();
            } else if (throwable instanceof ConditionalSkipReasonException) {
                conditionalSkipReason = ((ConditionalSkipReasonException) throwable).getConditionalSkipReason();
            } else if (throwable instanceof UnknownHostException) {
                DownloadLinkCandidateResult ret = new DownloadLinkCandidateResult(RESULT.CONNECTION_ISSUES);
                ret.setWaitTime(JsonConfig.create(GeneralSettings.class).getNetworkIssuesTimeout());
                ret.setMessage(_JDT._.plugins_errors_nointernetconn());
                return ret;
            } else if (throwable instanceof SocketTimeoutException) {
                DownloadLinkCandidateResult ret = new DownloadLinkCandidateResult(RESULT.CONNECTION_ISSUES);
                ret.setWaitTime(JsonConfig.create(GeneralSettings.class).getNetworkIssuesTimeout());
                ret.setMessage(_JDT._.plugins_errors_hosteroffline());
                return ret;
            } else if (throwable instanceof SocketException) {
                DownloadLinkCandidateResult ret = new DownloadLinkCandidateResult(RESULT.CONNECTION_ISSUES);
                ret.setWaitTime(JsonConfig.create(GeneralSettings.class).getNetworkIssuesTimeout());
                ret.setMessage(_JDT._.plugins_errors_disconnect());
                return ret;
            } else if (throwable instanceof ProxyAuthException) {
                HTTPProxy p = result.getController().getCurrentProxy();
                if (p != null && p instanceof ProxyInfo) {
                    if (p != ProxyController.getInstance().getNone()) {
                        ProxyController.getInstance().setproxyRotationEnabled((ProxyInfo) p, false);
                        if (ProxyController.getInstance().hasRotation() == false) {
                            ProxyController.getInstance().setproxyRotationEnabled(ProxyController.getInstance().getNone(), true);
                        }
                    }
                }
                DownloadLinkCandidateResult ret = new DownloadLinkCandidateResult(RESULT.CONNECTION_ISSUES);
                ret.setWaitTime(JsonConfig.create(GeneralSettings.class).getDownloadUnknownIOExceptionWaittime());
                ret.setMessage(_JDT._.plugins_errors_proxy_auth());
                return ret;
            } else if (throwable instanceof IOException) {
                DownloadLinkCandidateResult ret = new DownloadLinkCandidateResult(RESULT.CONNECTION_ISSUES);
                ret.setWaitTime(JsonConfig.create(GeneralSettings.class).getDownloadUnknownIOExceptionWaittime());
                ret.setMessage(_JDT._.plugins_errors_hosterproblem());
                return ret;
            } else if (throwable instanceof InterruptedException) {
                if (result.getController().isAborting()) { return new DownloadLinkCandidateResult(RESULT.STOPPED); }
                pluginException = new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _JDT._.plugins_errors_error() + "Interrupted");
            } else {
                pluginException = new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _JDT._.plugins_errors_error() + "Throwable");
            }
        }

        if (skipReason != null) {
            switch (skipReason) {
            case NO_ACCOUNT:
                return new DownloadLinkCandidateResult(RESULT.ACCOUNT_REQUIRED);
            default:
                return new DownloadLinkCandidateResult(skipReason);
            }
        }
        if (conditionalSkipReason != null) { return new DownloadLinkCandidateResult(conditionalSkipReason); }
        if (pluginException != null) {
            DownloadLinkCandidateResult ret = null;
            String message = null;
            long waitTime = -1;
            switch (pluginException.getLinkStatus()) {
            case LinkStatus.ERROR_RETRY:
                ret = new DownloadLinkCandidateResult(RESULT.RETRY);
                message = pluginException.getErrorMessage();
                break;
            case LinkStatus.ERROR_CAPTCHA:
                ret = new DownloadLinkCandidateResult(RESULT.CAPTCHA);
                break;
            case LinkStatus.FINISHED:
                ret = new DownloadLinkCandidateResult(RESULT.FINISHED_EXISTS);
                break;
            case LinkStatus.ERROR_FILE_NOT_FOUND:
                if (link.getHost().equals(pluginHost)) {
                    ret = new DownloadLinkCandidateResult(RESULT.OFFLINE_TRUSTED);
                } else {
                    ret = new DownloadLinkCandidateResult(RESULT.OFFLINE_UNTRUSTED);
                }
                break;
            case LinkStatus.ERROR_PLUGIN_DEFECT:
                if (latestPlugin != null) latestPlugin.errLog(throwable, null, link);
                ret = new DownloadLinkCandidateResult(RESULT.PLUGIN_DEFECT);
                break;
            case LinkStatus.ERROR_FATAL:
                ret = new DownloadLinkCandidateResult(RESULT.FATAL_ERROR);
                message = pluginException.getErrorMessage();
                break;
            case LinkStatus.ERROR_IP_BLOCKED:
                ret = new DownloadLinkCandidateResult(RESULT.IP_BLOCKED);
                message = pluginException.getErrorMessage();
                waitTime = 3600000l;
                if (pluginException.getValue() > 0) waitTime = pluginException.getValue();
                break;
            case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
                ret = new DownloadLinkCandidateResult(RESULT.FILE_UNAVAILABLE);
                message = pluginException.getErrorMessage();
                if (pluginException.getValue() > 0) {
                    waitTime = pluginException.getValue();
                } else if (pluginException.getValue() <= 0) {
                    waitTime = JsonConfig.create(GeneralSettings.class).getDownloadTempUnavailableRetryWaittime();
                }
                break;
            case LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE:
                ret = new DownloadLinkCandidateResult(RESULT.HOSTER_UNAVAILABLE);
                message = pluginException.getErrorMessage();
                if (pluginException.getValue() > 0) {
                    waitTime = pluginException.getValue();
                } else if (pluginException.getValue() <= 0) {
                    waitTime = JsonConfig.create(GeneralSettings.class).getDownloadHostUnavailableRetryWaittime();
                }
                break;
            case LinkStatus.ERROR_PREMIUM:
                if (pluginException.getValue() == PluginException.VALUE_ID_PREMIUM_ONLY) {
                    ret = new DownloadLinkCandidateResult(RESULT.ACCOUNT_REQUIRED);
                } else if (pluginException.getValue() == PluginException.VALUE_ID_PREMIUM_DISABLE) {
                    ret = new DownloadLinkCandidateResult(RESULT.ACCOUNT_INVALID);
                } else if (pluginException.getValue() == PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE) {
                    ret = new DownloadLinkCandidateResult(RESULT.ACCOUNT_UNAVAILABLE);
                } else {
                    ret = new DownloadLinkCandidateResult(RESULT.ACCOUNT_INVALID);
                }
                break;
            case LinkStatus.ERROR_DOWNLOAD_FAILED:
                if (pluginException.getValue() == LinkStatus.VALUE_LOCAL_IO_ERROR) {
                    ret = new DownloadLinkCandidateResult(SkipReason.INVALID_DESTINATION);
                } else if (pluginException.getValue() == LinkStatus.VALUE_TIMEOUT_REACHED) {
                    ret = new DownloadLinkCandidateResult(RESULT.CONNECTION_ISSUES);
                } else {
                    ret = new DownloadLinkCandidateResult(RESULT.FAILED);
                }
                break;
            case LinkStatus.ERROR_DOWNLOAD_INCOMPLETE:
                ret = new DownloadLinkCandidateResult(RESULT.FAILED_INCOMPLETE);
                break;
            case LinkStatus.ERROR_ALREADYEXISTS:
                ret = new DownloadLinkCandidateResult(RESULT.FAILED_EXISTS);
                break;
            }
            if (ret == null) ret = new DownloadLinkCandidateResult(RESULT.PLUGIN_DEFECT);
            ret.setWaitTime(waitTime);
            ret.setMessage(message);
            return ret;
        }
        if (result.getController().isAborting()) {
            if (result.getController().getLinkStatus().getStatus() == LinkStatus.FINISHED) {
                return new DownloadLinkCandidateResult(RESULT.FINISHED);
            } else {
                return new DownloadLinkCandidateResult(RESULT.STOPPED);
            }
        }
        if (result.getController().getLinkStatus().getStatus() == LinkStatus.FINISHED) { return new DownloadLinkCandidateResult(RESULT.FINISHED); }
        return new DownloadLinkCandidateResult(RESULT.PLUGIN_DEFECT);
    }

    private HashSet<DownloadLink> finalizeConditionalSkipReasons(DownloadSession currentSession) {
        HashSet<DownloadLink> ret = new HashSet<DownloadLink>();
        boolean again = true;
        while (again) {
            again = false;
            for (DownloadLink link : new ConcatIterator<DownloadLink>(currentSession.getForcedLinks().iterator(), currentSession.getActivationRequests().iterator())) {
                ConditionalSkipReason conditionalSkipReason = link.getConditionalSkipReason();
                if (conditionalSkipReason != null && conditionalSkipReason.isConditionReached()) {
                    again = true;
                    link.setConditionalSkipReason(null);
                    conditionalSkipReason.finalize(link);
                    if (link.getFinalLinkState() != null) ret.add(link);
                }
            }
        }
        return ret;
    }

    private synchronized void startDownloadJobExecuter() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                this.setName("WatchDog: jobExecuter");
                try {
                    while (true) {
                        synchronized (WATCHDOGWAITLOOP) {
                            if (!isWatchDogThread()) return;
                            /* clear interrupted flag in case someone interrupted a DownloadWatchDogJob */
                            interrupted();
                            if (watchDogJobs.size() == 0) WATCHDOGWAITLOOP.wait();
                            if (!isWatchDogThread()) return;
                        }
                        DownloadWatchDogJob job = watchDogJobs.poll();
                        if (job != null) {
                            WATCHDOGWAITLOOP.decrementAndGet();
                            try {
                                currentWatchDogJob.set(job);
                                job.execute(getSession());
                            } catch (final Throwable e) {
                                logger.log(e);
                            } finally {
                                currentWatchDogJob.set(null);
                            }
                        }
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                } finally {
                    currentWatchDogThread.compareAndSet(Thread.currentThread(), null);
                }
            }
        };
        thread.setDaemon(true);
        currentWatchDogThread.set(thread);
        thread.start();
    }

    private boolean isReconnectPossible() {
        if (CFG_RECONNECT.CFG.isReconnectAllowedToInterruptResumableDownloads()) {
            for (final SingleDownloadController con : getSession().getControllers()) {
                if (!con.getDownloadLink().isResumeable()) return false;
            }
            return true;
        } else {
            return getSession().getControllers().size() == 0;
        }
    }

    private void validateProxyInfoHistory() {
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(DownloadSession currentSession) {
                ProxyInfoHistory proxyInfoHistory = currentSession.getProxyInfoHistory();
                proxyInfoHistory.validate();
                Set<ProxyInfo> reconnects = proxyInfoHistory.list(WaitingSkipReason.CAUSE.IP_BLOCKED);
                boolean reconnectRequested = false;
                if (reconnects != null && reconnects.size() > 0) {
                    for (ProxyInfo proxyInfo : reconnects) {
                        if (!proxyInfo.isReconnectSupported()) continue;
                        reconnectRequested = true;
                        break;
                    }
                }
                if (reconnectRequested && isAutoReconnectEnabled()) {
                    currentSession.compareAndSetSessionState(DownloadSession.SessionState.NORMAL, DownloadSession.SessionState.RECONNECT_REQUESTED);
                    if (currentSession.getSessionState() == DownloadSession.SessionState.RECONNECT_REQUESTED && isReconnectPossible()) {
                        currentSession.setSessionState(DownloadSession.SessionState.RECONNECT_RUNNING);
                        invokeReconnect();
                    }
                } else {
                    currentSession.compareAndSetSessionState(DownloadSession.SessionState.RECONNECT_REQUESTED, DownloadSession.SessionState.NORMAL);
                }
            }

            @Override
            public void interrupt() {
            }
        });
    }

    private ReconnectThread invokeReconnect() {
        while (true) {
            Thread ret = reconnectThread.get();
            if (ret != null) return (ReconnectThread) ret;
            ret = new ReconnectThread();
            if (reconnectThread.compareAndSet(null, ret)) {
                final DownloadWatchDogJob job = new DownloadWatchDogJob() {

                    @Override
                    public void execute(DownloadSession currentSession) {
                        currentSession.setSessionState(DownloadSession.SessionState.RECONNECT_RUNNING);
                        if (currentSession.getControllers().size() > 0) {
                            for (SingleDownloadController con : getSession().getControllers()) {
                                if (con.isAlive() && !con.isAborting()) {
                                    con.abort();
                                }
                            }
                            enqueueJob(this);
                        } else {
                            reconnectThread.get().start();
                        }
                    }

                    @Override
                    public void interrupt() {
                    }
                };
                enqueueJob(job);
            }
        }
    }

    public Reconnecter.ReconnectResult requestReconnect(boolean waitForResult) throws InterruptedException {
        ReconnectThread thread = invokeReconnect();
        if (waitForResult) { return thread.waitForResult(); }
        return null;
    }

    protected DownloadWatchDogJob getCurrentDownloadWatchDogJob() {
        return currentWatchDogJob.get();
    }

    public void unSkipAllSkipped() {
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void interrupt() {
            }

            @Override
            public void execute(DownloadSession currentSession) {
                /* reset skipReasons */
                List<DownloadLink> unSkip = DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {

                    @Override
                    public int returnMaxResults() {
                        return 0;
                    }

                    @Override
                    public boolean acceptNode(DownloadLink node) {
                        return node.isSkipped();
                    }
                });
                unSkip(unSkip);
            }
        });
    }

    private synchronized void startDownloadWatchDog() {
        stateMachine.setStatus(RUNNING_STATE);
        Thread thread = new Thread() {

            protected void processJobs() {
                try {
                    setTempWatchDogJobThread(Thread.currentThread());
                    ArrayList<DownloadWatchDogJob> jobs = new ArrayList<DownloadWatchDogJob>();
                    watchDogJobs.drainTo(jobs);
                    for (DownloadWatchDogJob job : jobs) {
                        if (job != null) {
                            WATCHDOGWAITLOOP.decrementAndGet();
                            try {
                                currentWatchDogJob.set(job);
                                job.execute(getSession());
                            } catch (final Throwable e) {
                                logger.log(e);
                            } finally {
                                currentWatchDogJob.set(null);
                            }
                        }
                    }
                } finally {
                    setTempWatchDogJobThread(null);
                }
            }

            final int maxWaitTimeout = 5000;

            private void removeLink(final DownloadLink link, DownloadSession session) {
                session.removeActivationRequest(link);
                SingleDownloadController con = link.getDownloadLinkController();
                if (con != null) {
                    con.getJobsAfterDetach().add(new DownloadWatchDogJob() {

                        @Override
                        public void execute(DownloadSession currentSession) {
                            link.setConditionalSkipReason(null);
                            currentSession.removeHistory(link);
                        }

                        @Override
                        public void interrupt() {
                        }
                    });
                    con.abort();
                } else {
                    link.setConditionalSkipReason(null);
                    session.removeHistory(link);
                }
            }

            @Override
            public void run() {
                this.setName("WatchDog: downloadWatchDog");
                DownloadControllerListener listener = null;
                AccountControllerListener accListener = null;
                HosterRuleControllerListener hrcListener = null;
                final AtomicLong lastStructureChange = new AtomicLong(-1l);
                final AtomicLong lastActivatorRequestRebuild = new AtomicLong(-1l);
                long waitedForNewActivationRequests = 0;
                try {
                    unSkipAllSkipped();
                    HosterRuleController.getInstance().getEventSender().addListener(hrcListener = new HosterRuleControllerListener() {

                        private void removeAccountCache(final String host) {
                            if (host == null) return;
                            enqueueJob(new DownloadWatchDogJob() {

                                @Override
                                public void execute(DownloadSession currentSession) {
                                    currentSession.removeAccountCache(host);
                                }

                                @Override
                                public void interrupt() {
                                }
                            });
                        }

                        @Override
                        public void onRuleAdded(AccountUsageRule parameter) {
                            if (parameter == null) return;
                            removeAccountCache(parameter.getHoster());
                        }

                        @Override
                        public void onRuleDataUpdate(AccountUsageRule parameter) {
                            if (parameter == null) return;
                            removeAccountCache(parameter.getHoster());
                        }

                        @Override
                        public void onRuleRemoved(final AccountUsageRule parameter) {
                            if (parameter == null) return;
                            removeAccountCache(parameter.getHoster());
                        }

                    }, true);

                    AccountController.getInstance().getBroadcaster().addListener(accListener = new AccountControllerListener() {

                        @Override
                        public void onAccountControllerEvent(final AccountControllerEvent event) {
                            enqueueJob(new DownloadWatchDogJob() {

                                @Override
                                public void execute(DownloadSession currentSession) {
                                    currentSession.removeAccountCache(event.getParameter().getHoster());
                                }

                                @Override
                                public void interrupt() {
                                }
                            });
                        }
                    }, true);

                    DownloadController.getInstance().addListener(listener = new DownloadControllerListener() {

                        @Override
                        public void onDownloadControllerAddedPackage(FilePackage pkg) {
                            lastStructureChange.set(-1l);
                        }

                        @Override
                        public void onDownloadControllerStructureRefresh(FilePackage pkg) {
                            lastStructureChange.set(-1l);
                        }

                        @Override
                        public void onDownloadControllerStructureRefresh() {
                            lastStructureChange.set(-1l);
                        }

                        @Override
                        public void onDownloadControllerStructureRefresh(AbstractNode node, Object param) {
                            lastStructureChange.set(-1l);
                        }

                        @Override
                        public void onDownloadControllerRemovedPackage(final FilePackage fp) {
                            lastStructureChange.set(-1l);
                            enqueueJob(new DownloadWatchDogJob() {
                                @Override
                                public void execute(DownloadSession currentSession) {
                                    currentSession.setOnFileExistsAction(fp, null);
                                }

                                @Override
                                public void interrupt() {
                                }
                            });
                        }

                        @Override
                        public void onDownloadControllerRemovedLinklist(final List<DownloadLink> list) {
                            lastStructureChange.set(-1l);
                            enqueueJob(new DownloadWatchDogJob() {
                                @Override
                                public void execute(DownloadSession currentSession) {
                                    for (DownloadLink item : list) {
                                        if (item instanceof DownloadLink) {
                                            removeLink((DownloadLink) item, currentSession);
                                        }
                                    }
                                }

                                @Override
                                public void interrupt() {
                                }
                            });
                        }

                        @Override
                        public void onDownloadControllerUpdatedData(DownloadLink dl, DownloadLinkProperty dlProperty) {
                            lastStructureChange.set(-1l);

                            if (dl != null) {

                                final DownloadLink link = dlProperty.getDownloadLink();
                                switch (dlProperty.getProperty()) {
                                case PRIORITY:
                                    enqueueJob(new DownloadWatchDogJob() {
                                        @Override
                                        public void execute(DownloadSession currentSession) {
                                            if (link.isEnabled()) {
                                                currentSession.incrementActivatorRebuildRequest();
                                            }
                                        }

                                        @Override
                                        public void interrupt() {
                                        }
                                    });
                                    break;
                                case ENABLED:
                                    enqueueJob(new DownloadWatchDogJob() {
                                        @Override
                                        public void execute(DownloadSession currentSession) {
                                            if (!link.isEnabled()) {
                                                removeLink(link, currentSession);
                                            } else {
                                                currentSession.incrementActivatorRebuildRequest();
                                            }
                                        }

                                        @Override
                                        public void interrupt() {
                                        }
                                    });
                                    break;
                                case SKIPPED:
                                    enqueueJob(new DownloadWatchDogJob() {
                                        @Override
                                        public void execute(DownloadSession currentSession) {
                                            if (link.getSkipReason() != null) {
                                                removeLink(link, currentSession);
                                            } else {
                                                currentSession.incrementActivatorRebuildRequest();
                                            }
                                        }

                                        @Override
                                        public void interrupt() {
                                        }
                                    });
                                    break;
                                case FINAL_STATE:
                                    enqueueJob(new DownloadWatchDogJob() {
                                        @Override
                                        public void execute(DownloadSession currentSession) {
                                            if (link.getFinalLinkState() != null) {
                                                removeLink(link, currentSession);
                                            } else {
                                                currentSession.incrementActivatorRebuildRequest();
                                            }
                                        }

                                        @Override
                                        public void interrupt() {
                                        }
                                    });
                                    break;
                                }
                            }

                        }

                        @Override
                        public void onDownloadControllerUpdatedData(FilePackage pkg, FilePackageProperty property) {
                            lastStructureChange.set(-1l);
                        }

                        @Override
                        public void onDownloadControllerUpdatedData(DownloadLink downloadlink, LinkStatusProperty property) {
                            lastStructureChange.set(-1l);
                        }

                        @Override
                        public void onDownloadControllerUpdatedData(DownloadLink downloadlink) {
                            lastStructureChange.set(-1l);
                        }

                        @Override
                        public void onDownloadControllerUpdatedData(FilePackage pkg) {
                            lastStructureChange.set(-1l);
                        }
                    }, true);
                    while (DownloadWatchDog.this.stateMachine.isState(DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.PAUSE_STATE)) {

                        long currentStructure = DownloadController.getInstance().getPackageControllerChanges();
                        long currentActivatorRequestRebuild = getSession().getActivatorRebuildRequest();
                        if (lastStructureChange.getAndSet(currentStructure) != currentStructure || lastActivatorRequestRebuild.getAndSet(currentActivatorRequestRebuild) != currentActivatorRequestRebuild) {
                            final AtomicInteger skippedLinksCounterTmp = new AtomicInteger(0);
                            List<DownloadLink> links = DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {

                                @Override
                                public int returnMaxResults() {
                                    return 0;
                                }

                                @Override
                                public boolean acceptNode(DownloadLink node) {
                                    if (node.isSkipped()) {
                                        skippedLinksCounterTmp.incrementAndGet();
                                        return false;
                                    }
                                    if (canRemove(node, false)) {
                                        node.setConditionalSkipReason(null);
                                        return false;
                                    }
                                    return true;
                                }
                            });
                            getSession().setSkipCounter(skippedLinksCounterTmp.get());
                            getSession().setActivationRequests(new CopyOnWriteArrayList<DownloadLink>(sortActivationRequests(links)));
                            eventSender.fireEvent(new DownloadWatchdogEvent(this, DownloadWatchdogEvent.Type.DATA_UPDATE));
                        }
                        try {
                            validateProxyInfoHistory();
                            processJobs();
                            HashSet<DownloadLink> finalLinkStateLinks = finalizeConditionalSkipReasons(getSession());
                            handleFinalLinkStates(finalLinkStateLinks, getSession(), logger, null);
                            if (newDLStartAllowed(getSession())) {
                                DownloadWatchDog.this.activateDownloads();
                            }
                            if (getSession().getControllers().size() == 0 && getSession().isStopMarkReached()) {
                                logger.info("Wait at least " + Math.max(0, (maxWaitTimeout - waitedForNewActivationRequests)) + " for new change in StopMark(reached)");
                            } else if (getSession().getControllers().size() > 0 || lastActivatorRequestRebuild.get() != getSession().getActivatorRebuildRequest() || lastStructureChange.get() != DownloadController.getInstance().getPackageControllerChanges() || getSession().isActivationRequestsWaiting()) {
                                waitedForNewActivationRequests = 0;
                            } else {
                                logger.info("Wait at least " + Math.max(0, (maxWaitTimeout - waitedForNewActivationRequests)) + " for new ActivationRequests");
                            }
                            if (waitedForNewActivationRequests > maxWaitTimeout) {
                                logger.info("Waited " + waitedForNewActivationRequests + " but no new ActivationRequests are available->Stop DownloadWatchDog");
                                break;
                            }
                        } catch (final Exception e) {
                            logger.log(e);
                        }
                        try {
                            int round = 0;
                            synchronized (WATCHDOGWAITLOOP) {
                                while (WATCHDOGWAITLOOP.get() == 0 && ++round < 4 && DownloadWatchDog.this.stateMachine.isState(DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.PAUSE_STATE)) {
                                    long currentTimeStamp = System.currentTimeMillis();
                                    WATCHDOGWAITLOOP.wait(1250);
                                    waitedForNewActivationRequests += System.currentTimeMillis() - currentTimeStamp;
                                    if ((getSession().isActivationRequestsWaiting() == false && DownloadWatchDog.this.getActiveDownloads() == 0)) {
                                        /*
                                         * it's important that this if statement gets checked after wait!, else we will loop through without waiting for new
                                         * links/user interaction
                                         */
                                        break;
                                    }
                                }
                            }
                        } catch (final InterruptedException e) {
                            logger.log(e);
                        }
                    }
                    enqueueJob(new DownloadWatchDogJob() {

                        @Override
                        public void execute(DownloadSession currentSession) {
                            stateMachine.setStatus(STOPPING_STATE);
                        }

                        @Override
                        public void interrupt() {
                        }
                    });
                    DownloadController.getInstance().removeListener(listener);
                    AccountController.getInstance().getBroadcaster().removeListener(accListener);
                    HosterRuleController.getInstance().getEventSender().removeListener(hrcListener);
                    logger.info("DownloadWatchDog: stopping");
                    synchronized (DownloadWatchDog.this) {
                        startDownloadJobExecuter();
                    }
                    /* stop all remaining downloads */
                    abortAllSingleDownloadControllers();
                    /* clear Status */
                    clearDownloadListStatus();
                    /* clear blocked Accounts */
                    // TODO:AccountController.getInstance().removeAccountBlocked(null);
                    /* unpause downloads */
                    pauseDownloadWatchDog(false);
                    if (getSession().isStopMarkReached()) {
                        /* remove stopsign if it has been reached */
                        getSession().setStopMark(STOPMARK.NONE);
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                    enqueueJob(new DownloadWatchDogJob() {

                        @Override
                        public void execute(DownloadSession currentSession) {
                            stateMachine.setStatus(STOPPING_STATE);
                        }

                        @Override
                        public void interrupt() {
                        }
                    });
                } finally {
                    synchronized (DownloadWatchDog.this) {
                        startDownloadJobExecuter();
                    }
                    /* full stop reached */
                    logger.info("DownloadWatchDog: stopped");
                    /* clear session */
                    final DownloadSession latestSession = getSession();
                    enqueueJob(new DownloadWatchDogJob() {

                        @Override
                        public void execute(DownloadSession currentSession) {
                            stateMachine.setStatus(STOPPED_STATE);
                            latestSession.removeHistory((DownloadLink) null);
                            latestSession.removeAccountCache(null);
                            latestSession.getActivationPluginCache().clear();
                            latestSession.getActivationRequests().clear();
                            latestSession.getForcedLinks().clear();
                        }

                        @Override
                        public void interrupt() {
                        }
                    });
                }
            }
        };
        thread.setDaemon(true);
        currentWatchDogThread.set(thread);
        thread.start();
    }

    /**
     * tell the DownloadWatchDog to stop all running Downloads
     */
    public void stopDownloads() {
        DownloadWatchDogJob job = getCurrentDownloadWatchDogJob();
        if (job != null) job.interrupt();
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(DownloadSession currentSession) {
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

            @Override
            public void interrupt() {
            }
        });
    }

    /**
     * toggles between start/stop states
     */
    public void toggleStartStop() {
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(DownloadSession currentSession) {
                if (stateMachine.isStartState() || stateMachine.isFinal()) {
                    /* download is in idle or stopped state */
                    DownloadWatchDog.this.startDownloads();
                } else {
                    /* download can be stopped */
                    DownloadWatchDog.this.stopDownloads();
                }
            }

            @Override
            public void interrupt() {
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
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(DownloadSession currentSession) {
                if (stateMachine.isState(RUNNING_STATE, PAUSE_STATE)) {
                    config.setClosedWithRunningDownloads(true);
                } else {
                    config.setClosedWithRunningDownloads(false);
                }
                stopDownloads();
            }

            @Override
            public void interrupt() {
            }
        });
        while (isIdle() == false) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
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
                    LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(LinkOrigin.DOWNLOADED_CONTAINER, sb.toString()));
                }

            }.start();
        }
    }

    public void localFileCheck(final SingleDownloadController controller, final ExceptionRunnable runOkay, final ExceptionRunnable runFailed) throws Exception {
        final NullsafeAtomicReference<Object> asyncResult = new NullsafeAtomicReference<Object>(null);
        enqueueJob(new DownloadWatchDogJob() {

            private void check(DownloadSession session, SingleDownloadController controller) throws Exception {
                if (controller.isAborting()) throw new InterruptedException("Controller is aborted");
                DownloadLink downloadLink = controller.getDownloadLink();
                String localCheck = downloadLink.getFileOutput(false, true);
                File fileOutput = new File(localCheck);
                if (fileOutput.isDirectory()) {
                    controller.getLogger().severe("fileOutput is a directory " + fileOutput);
                    throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
                }
                boolean fileExists = fileOutput.exists();
                if (!fileExists) {
                    File path = null;
                    if ((path = validateDestination(fileOutput)) != null) {
                        controller.getLogger().severe("not allowed to create path " + path);
                        throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
                    }
                    if (fileOutput.getParentFile() == null) {
                        controller.getLogger().severe("has no parentFile?! " + fileOutput);
                        throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
                    }
                    if (!fileOutput.getParentFile().exists()) {
                        if (!fileOutput.getParentFile().mkdirs()) {
                            controller.getLogger().severe("could not mkdirs parentFile " + fileOutput.getParent());
                            throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
                        }
                    }
                    File writeTest = new File(fileOutput.getParentFile(), "jd_accessCheck_" + System.currentTimeMillis());
                    try {
                        if (writeTest.exists() == false) {
                            if (!writeTest.createNewFile()) throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
                        } else {
                            if (!writeTest.canWrite()) throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
                        }
                    } catch (Throwable e) {
                        LogSource.exception(controller.getLogger(), e);
                        if (e instanceof SkipReasonException) throw (SkipReasonException) e;
                        throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
                    } finally {
                        writeTest.delete();
                    }
                }
                if (controller.getDownloadInstance() == null) {
                    /* we are outside DownloadInterface */
                    String localCheck2 = downloadLink.getFileOutput(true, true);
                    if (localCheck2 == null) {
                        /*
                         * dont proceed when we do not have a finalFilename yet
                         */
                        return;
                    }
                }
                boolean fileInProgress = false;
                if (!fileExists) {
                    for (SingleDownloadController downloadController : session.getControllers()) {
                        if (downloadController == controller) continue;
                        DownloadLink block = downloadController.getDownloadLink();
                        if (block == downloadLink) continue;
                        if (session.getFileAccessManager().isLockedBy(fileOutput, downloadController)) {
                            /* fileOutput is already locked */
                            if (isMirrorCandidate(downloadLink, localCheck, block) && block.getFilePackage() == downloadLink.getFilePackage()) {
                                /* only throw ConditionalSkipReasonException when file is from same package */
                                throw new ConditionalSkipReasonException(new MirrorLoading(block));
                            } else {
                                fileInProgress = true;
                                break;
                            }
                        }
                    }
                }
                if (fileExists || fileInProgress) {
                    IfFileExistsAction doAction = JsonConfig.create(GeneralSettings.class).getIfFileExistsAction();
                    if (doAction == null || IfFileExistsAction.ASK_FOR_EACH_FILE == doAction) {
                        DownloadSession currentSession = getSession();
                        doAction = currentSession.getOnFileExistsAction(downloadLink.getFilePackage());
                        if (doAction == null || doAction == IfFileExistsAction.ASK_FOR_EACH_FILE) {
                            IfFileExistsDialogInterface io = new IfFileExistsDialog(downloadLink).show();
                            if (io.getCloseReason() == CloseReason.TIMEOUT) { throw new SkipReasonException(SkipReason.FILE_EXISTS); }
                            if (io.getCloseReason() == CloseReason.INTERRUPT) { throw new InterruptedException("IFFileExistsDialog Interrupted"); }
                            if (io.getCloseReason() != CloseReason.OK) {
                                doAction = IfFileExistsAction.SKIP_FILE;
                            } else {
                                doAction = io.getAction();
                            }
                            if (doAction == null) doAction = IfFileExistsAction.SKIP_FILE;
                            if (io.isDontShowAgainSelected() && io.getCloseReason() == CloseReason.OK) {
                                currentSession.setOnFileExistsAction(downloadLink.getFilePackage(), doAction);
                            } else {
                                currentSession.setOnFileExistsAction(downloadLink.getFilePackage(), null);
                            }
                        }
                    }
                    switch (doAction) {
                    case SKIP_FILE:
                        switch (CFG_GENERAL.CFG.getOnSkipDueToAlreadyExistsAction()) {
                        case SET_FILE_TO_SUCCESSFUL:
                            throw new PluginException(LinkStatus.FINISHED);
                        default:
                            throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
                        }
                    case OVERWRITE_FILE:
                        if (fileInProgress) {
                            /* we cannot overwrite a file that is currently in progress */
                            controller.getLogger().severe("Cannot not overwrite file in progress! " + fileOutput);
                            throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
                        }
                        if (!fileOutput.delete()) {
                            controller.getLogger().severe("Could not overwrite file! " + fileOutput);
                            throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
                        }
                        break;
                    case AUTO_RENAME:
                        String splitName[] = CrossSystem.splitFileName(fileOutput.getName());
                        String downloadPath = fileOutput.getParent();
                        String extension = splitName[1];
                        if (extension == null) {
                            extension = "";
                        } else {
                            extension = "." + extension;
                        }
                        String name = splitName[0];
                        long duplicateFilenameCounter = 2;
                        String alreadyDuplicated = new Regex(name, ".*_(\\d+)$").getMatch(0);
                        if (alreadyDuplicated != null) {
                            /* it seems the file already got auto renamed! */
                            duplicateFilenameCounter = Long.parseLong(alreadyDuplicated) + 1;
                            name = new Regex(name, "(.*)_\\d+$").getMatch(0);
                        }
                        String newName = null;
                        try {
                            newName = name + "_" + duplicateFilenameCounter + extension;
                            while (new File(downloadPath, newName).exists()) {
                                newName = name + "_" + (++duplicateFilenameCounter) + extension;
                            }
                            downloadLink.forceFileName(newName);
                            downloadLink.setChunksProgress(null);
                        } catch (Throwable e) {
                            LogSource.exception(controller.getLogger(), e);
                            downloadLink.forceFileName(null);
                            throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
                        }
                        break;
                    default:
                        throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
                    }
                }
                File partFile = new File(fileOutput.getAbsolutePath() + ".part");
                // we should not use downloadLink.getDownloadCurrent() here. downloadLink.getDownloadCurrent() returns the amout of loaded
                // bytes, but NOT the size of the partfile.

                DISKSPACECHECK check = checkFreeDiskSpace(fileOutput.getParentFile(), controller, (downloadLink.getDownloadSize() - (partFile.exists() ? partFile.length() : 0)));
                switch (check) {
                case FAILED:
                    throw new SkipReasonException(SkipReason.DISK_FULL);
                case INVALIDFOLDER:
                    throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
                }

                return;
            }

            @Override
            public void execute(DownloadSession currentSession) {
                try {
                    check(currentSession, controller);
                    if (runOkay != null) runOkay.run();
                    synchronized (asyncResult) {
                        asyncResult.set(asyncResult);
                        asyncResult.notifyAll();
                    }
                } catch (final Exception e) {
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

            @Override
            public void interrupt() {
                Thread watchDogThread = getWatchDogThread();
                if (getCurrentDownloadWatchDogJob() == this && watchDogThread != null) watchDogThread.interrupt();
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
                    DownloadInterface dl = con.getDownloadInstance();
                    if (dl != null && !con.getDownloadLink().isResumeable()) {
                        dialogTitle = _JDT._.DownloadWatchDog_onShutdownRequest_nonresumable();
                        break;
                    }
                }
            }
            if (dialogTitle != null) {
                if (request.isSilent() == false) {
                    if (JDGui.bugme(WarnLevel.NORMAL)) {
                        if (UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, dialogTitle, _JDT._.DownloadWatchDog_onShutdownRequest_msg(), NewTheme.I().getIcon("download", 32), _JDT._.literally_yes(), null)) { return; }
                    } else {
                        return;
                    }
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

    public long getNonResumableBytes(SelectionInfo<FilePackage, DownloadLink> selection) {
        long i = 0;
        if (this.stateMachine.isState(RUNNING_STATE, PAUSE_STATE, STOPPING_STATE)) {
            for (final SingleDownloadController con : getSession().getControllers()) {
                DownloadInterface dl = con.getDownloadInstance();
                if (dl != null && !con.getDownloadLink().isResumeable() && selection.contains(con.getDownloadLink())) {
                    i += con.getDownloadLink().getDownloadCurrent();
                }
            }
        }
        return i;
    }

    public int getNonResumableRunningCount() {
        int i = 0;
        if (this.stateMachine.isState(RUNNING_STATE, PAUSE_STATE, STOPPING_STATE)) {
            for (final SingleDownloadController con : getSession().getControllers()) {
                if (!con.getDownloadLink().isResumeable()) i++;
            }
        }
        return i;
    }

    public long getNonResumableBytes() {
        long i = 0;
        if (this.stateMachine.isState(RUNNING_STATE, PAUSE_STATE, STOPPING_STATE)) {
            for (final SingleDownloadController con : getSession().getControllers()) {
                DownloadInterface dl = con.getDownloadInstance();
                if (dl != null && !con.getDownloadLink().isResumeable()) {
                    i += con.getDownloadLink().getDownloadCurrent();
                }
            }
        }
        return i;
    }

    public boolean isLinkForced(DownloadLink dlLink) {
        List<DownloadLink> links = getSession().getForcedLinks();
        return links.size() > 0 && links.contains(dlLink);
    }

    public boolean isRunning() {
        return stateMachine.isState(RUNNING_STATE, PAUSE_STATE);
    }

    public boolean isIdle() {
        return stateMachine.isState(IDLE_STATE, STOPPED_STATE);
    }

    public boolean isStopping() {
        return stateMachine.isState(STOPPING_STATE);

    }

    @Override
    public void onShutdown(ShutdownRequest request) {
    }

    @Override
    public void onShutdownVeto(ShutdownRequest request) {
    }

    @Override
    public void onDownloadControllerAddedPackage(FilePackage pkg) {

    }

    @Override
    public void onDownloadControllerStructureRefresh(FilePackage pkg) {
    }

    @Override
    public void onDownloadControllerStructureRefresh() {
    }

    @Override
    public void onDownloadControllerStructureRefresh(AbstractNode node, Object param) {
    }

    @Override
    public void onDownloadControllerRemovedPackage(final FilePackage pkg) {
        DownloadSession session = getSession();
        if (session.isStopMarkSet() == false) return;
        if (session.getStopMark() == STOPMARK.HIDDEN) return;
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(DownloadSession currentSession) {
                if (currentSession.isStopMarkSet() == false) return;
                Object stopMark = currentSession.getStopMark();
                if (stopMark == STOPMARK.HIDDEN) return;

                if (stopMark == pkg) {
                    /* now the stopmark is hidden */
                    currentSession.setStopMark(STOPMARK.HIDDEN);
                    return;
                }
            }

            @Override
            public void interrupt() {
            }
        });
    }

    @Override
    public void onDownloadControllerRemovedLinklist(final List<DownloadLink> list) {
        DownloadSession session = getSession();
        if (session.isStopMarkSet() == false) return;
        if (session.getStopMark() == STOPMARK.HIDDEN) return;
        enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(DownloadSession currentSession) {
                if (currentSession.isStopMarkSet() == false) return;
                Object stopMark = currentSession.getStopMark();
                if (stopMark == STOPMARK.HIDDEN) return;

                for (DownloadLink l : list) {
                    if (stopMark == l) {
                        currentSession.setStopMark(STOPMARK.HIDDEN);
                        return;
                    }
                }

            }

            @Override
            public void interrupt() {
            }
        });
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink, DownloadLinkProperty property) {
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg, FilePackageProperty property) {
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink, LinkStatusProperty property) {
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink) {
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg) {
    }

}