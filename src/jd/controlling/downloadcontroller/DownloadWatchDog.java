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
import java.io.RandomAccessFile;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.controlling.TaskQueue;
import jd.controlling.captcha.CaptchaSettings;
import jd.controlling.downloadcontroller.AccountCache.CachedAccount;
import jd.controlling.downloadcontroller.DiskSpaceManager.DISKSPACERESERVATIONRESULT;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult.RESULT;
import jd.controlling.downloadcontroller.DownloadLinkCandidateSelector.CachedAccountPermission;
import jd.controlling.downloadcontroller.DownloadLinkCandidateSelector.DownloadLinkCandidatePermission;
import jd.controlling.downloadcontroller.DownloadSession.STOPMARK;
import jd.controlling.downloadcontroller.DownloadSession.SessionState;
import jd.controlling.downloadcontroller.ProxyInfoHistory.WaitingSkipReasonContainer;
import jd.controlling.downloadcontroller.event.DownloadWatchdogEvent;
import jd.controlling.downloadcontroller.event.DownloadWatchdogEventSender;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcollector.LinkOriginDetails;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.ProxyEvent;
import jd.controlling.reconnect.Reconnecter;
import jd.controlling.reconnect.Reconnecter.ReconnectResult;
import jd.controlling.reconnect.ReconnecterEvent;
import jd.controlling.reconnect.ReconnecterListener;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;
import jd.http.NoGateWayException;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.CandidateResultProvider;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageProperty;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.Downloadable;
import jd.plugins.download.HashInfo;
import jd.plugins.download.HashResult;
import jd.plugins.download.raf.FileBytesCache;

import org.appwork.controlling.State;
import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.controlling.StateMachine;
import org.appwork.controlling.StateMachineInterface;
import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.uio.CloseReason;
import org.appwork.uio.ExceptionDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.ConcatIterator;
import org.appwork.utils.IO;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.ProxyAuthException;
import org.appwork.utils.net.httpconnection.ProxyConnectException;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.ExceptionDialog;
import org.jdownloader.captcha.blacklist.CaptchaBlackList;
import org.jdownloader.controlling.DownloadLinkWalker;
import org.jdownloader.controlling.FileCreationEvent;
import org.jdownloader.controlling.FileCreationListener;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.FileCreationManager.DeleteOption;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.domainrules.DomainRuleController;
import org.jdownloader.controlling.domainrules.event.DomainRuleControllerListener;
import org.jdownloader.controlling.download.DownloadControllerListener;
import org.jdownloader.controlling.hosterrule.AccountUsageRule;
import org.jdownloader.controlling.hosterrule.HosterRuleController;
import org.jdownloader.controlling.hosterrule.HosterRuleControllerListener;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.ConditionalSkipReason;
import org.jdownloader.plugins.ConditionalSkipReasonException;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.plugins.IgnorableConditionalSkipReason;
import org.jdownloader.plugins.MirrorLoading;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.plugins.TimeOutCondition;
import org.jdownloader.plugins.ValidatableConditionalSkipReason;
import org.jdownloader.plugins.WaitForAccountSkipReason;
import org.jdownloader.plugins.WaitWhileWaitingSkipReasonIsSet;
import org.jdownloader.plugins.WaitingSkipReason;
import org.jdownloader.plugins.WaitingSkipReason.CAUSE;
import org.jdownloader.plugins.controller.container.ContainerPluginController;
import org.jdownloader.settings.CleanAfterDownloadAction;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.IfFileExistsAction;
import org.jdownloader.settings.MirrorDetectionDecision;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;
import org.jdownloader.settings.staticreferences.CFG_RECONNECT;
import org.jdownloader.translate._JDT;
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
                    if (finished.get() == false) {
                        return Reconnecter.ReconnectResult.RUNNING;
                    }
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

    public static final State                              IDLE_STATE            = new State("IDLE");
    public static final State                              RUNNING_STATE         = new State("RUNNING");
    public static final State                              PAUSE_STATE           = new State("PAUSE");
    public static final State                              STOPPING_STATE        = new State("STOPPING");
    public static final State                              STOPPED_STATE         = new State("STOPPED_STATE");
    static {
        IDLE_STATE.addChildren(RUNNING_STATE);
        RUNNING_STATE.addChildren(STOPPING_STATE, PAUSE_STATE);
        PAUSE_STATE.addChildren(RUNNING_STATE, STOPPING_STATE);
        STOPPING_STATE.addChildren(STOPPED_STATE);
    }
    protected final NullsafeAtomicReference<Thread>        currentWatchDogThread = new NullsafeAtomicReference<Thread>(null);
    protected final NullsafeAtomicReference<Thread>        reconnectThread       = new NullsafeAtomicReference<Thread>(null);
    protected final NullsafeAtomicReference<Thread>        tempWatchDogJobThread = new NullsafeAtomicReference<Thread>(null);
    protected NullsafeAtomicReference<DownloadWatchDogJob> currentWatchDogJob    = new NullsafeAtomicReference<DownloadWatchDogJob>(null);
    private final LinkedList<DownloadWatchDogJob>          watchDogJobs          = new LinkedList<DownloadWatchDogJob>();
    private final StateMachine                             stateMachine;
    private final DownloadSpeedManager                     dsm;
    private final GeneralSettings                          config;
    private final static DownloadWatchDog                  INSTANCE              = new DownloadWatchDog();
    private final LogSource                                logger;
    private final Object                                   WATCHDOGLOCK          = new Object();
    private final AtomicBoolean                            autoReconnectEnabled  = new AtomicBoolean(false);
    private final DownloadWatchdogEventSender              eventSender;
    private final boolean                                  isForceMirrorDetectionCaseInsensitive;

    public boolean isForceMirrorDetectionCaseInsensitive() {
        return isForceMirrorDetectionCaseInsensitive;
    }

    public DownloadWatchdogEventSender getEventSender() {
        return eventSender;
    }

    public static DownloadWatchDog getInstance() {
        return INSTANCE;
    }

    private DownloadWatchDog() {
        logger = LogController.CL();
        config = JsonConfig.create(GeneralSettings.class);
        isForceMirrorDetectionCaseInsensitive = config.isForceMirrorDetectionCaseInsensitive();
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
        /* changes in max simultaneous downloads */
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

                    @Override
                    public boolean isHighPriority() {
                        return false;
                    }
                });
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Integer> keyHandler, Integer invalidValue, ValidationException validateException) {
            }
        }, false);
        CFG_CAPTCHA.CAPTCHA_MODE.getEventSender().addListener(new GenericConfigEventListener<Enum>() {
            @Override
            public void onConfigValueModified(KeyHandler<Enum> keyHandler, final Enum newValue) {
                enqueueJob(new DownloadWatchDogJob() {
                    @Override
                    public void execute(DownloadSession currentSession) {
                        currentSession.setCaptchaMode((CaptchaSettings.MODE) newValue);
                    }

                    @Override
                    public void interrupt() {
                    }

                    @Override
                    public boolean isHighPriority() {
                        return true;
                    }
                });
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Enum> keyHandler, Enum invalidValue, ValidationException validateException) {
            }
        });
        initSession.setCaptchaMode(CFG_CAPTCHA.CFG.getCaptchaMode());
        /* changes in max simultaneous downloads per host */
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

                        @Override
                        public boolean isHighPriority() {
                            return false;
                        }
                    });
                }
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Integer> keyHandler, Integer invalidValue, ValidationException validateException) {
            }
        }, false);
        DomainRuleController.getInstance().getEventSender().addListener(new DomainRuleControllerListener() {
            @Override
            public void onDomainRulesUpdated() {
                enqueueJob(new DownloadWatchDogJob() {
                    @Override
                    public void execute(DownloadSession currentSession) {
                        currentSession.refreshCandidates();
                    }

                    @Override
                    public void interrupt() {
                    }

                    @Override
                    public boolean isHighPriority() {
                        return false;
                    }
                });
            }
        });
        /* max simultaneous downloads per host enabled? */
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

                    @Override
                    public boolean isHighPriority() {
                        return false;
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
        /* use accounts enabled? */
        CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {
            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, final Boolean newValue) {
                enqueueJob(new DownloadWatchDogJob() {
                    @Override
                    public void execute(DownloadSession currentSession) {
                        final boolean useAccounts = Boolean.TRUE.equals(newValue);
                        logger.info("USE_AVAILABLE_ACCOUNTS: " + useAccounts);
                        currentSession.setUseAccountsEnabled(useAccounts);
                        currentSession.removeAccountCache(null);
                    }

                    @Override
                    public void interrupt() {
                    }

                    @Override
                    public boolean isHighPriority() {
                        return true;
                    }
                });
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        }, false);
        final boolean useAccounts = CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.isEnabled();
        logger.info("USE_AVAILABLE_ACCOUNTS: " + useAccounts);
        initSession.setUseAccountsEnabled(useAccounts);
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
                                    if (reconnect.getProxySelector().isReconnectSupported()) {
                                        reconnect.invalidate();
                                    }
                                }
                            }
                        }
                        currentSession.compareAndSetSessionState(DownloadSession.SessionState.RECONNECT_RUNNING, DownloadSession.SessionState.NORMAL);
                    }

                    @Override
                    public void interrupt() {
                    }

                    @Override
                    public boolean isHighPriority() {
                        return true;
                    }
                });
                if (isAutoReconnectEnabled() && Reconnecter.getFailedCounter() > 5) {
                    switch (event.getResult()) {
                    case FAILED:
                        CFG_RECONNECT.AUTO_RECONNECT_ENABLED.setValue(false);
                        UserIO.getInstance().requestMessageDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, _JDT.T.jd_controlling_reconnect_Reconnector_progress_failed2());
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

    public final boolean isWatchDogThread() {
        final Thread current = Thread.currentThread();
        return current == getWatchDogThread();
    }

    private final Thread getWatchDogThread() {
        final Thread current = tempWatchDogJobThread.get();
        if (current != null) {
            return current;
        }
        return currentWatchDogThread.get();
    }

    /**
     * reset LinkStatus for files where it is useful and needed
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
        if (linksForce == null || linksForce.size() == 0) {
            return;
        }
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
                     * no downloads are running, so we will force only the selected links to get started by setting stopmark to first forced
                     * link
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

            @Override
            public boolean isHighPriority() {
                return true;
            }
        });
    }

    private void enqueueForcedDownloads(final List<DownloadLink> linksForce) {
        enqueueJob(new DownloadWatchDogJob() {
            @Override
            public void execute(DownloadSession currentSession) {
                final Set<DownloadLink> oldList = new LinkedHashSet<DownloadLink>(currentSession.getForcedLinks());
                oldList.addAll(linksForce);
                currentSession.setForcedLinks(new CopyOnWriteArrayList<DownloadLink>(sortActivationRequests(oldList)));
            }

            @Override
            public void interrupt() {
            }

            @Override
            public boolean isHighPriority() {
                return true;
            }
        });
    }

    private List<DownloadLink> sortActivationRequests(Iterable<DownloadLink> links) {
        HashMap<Priority, java.util.List<DownloadLink>> optimizedList = new HashMap<Priority, java.util.List<DownloadLink>>();
        int count = 0;
        for (DownloadLink link : links) {
            count++;
            Priority prio = link.getPriorityEnum();
            if (Priority.DEFAULT.equals(prio)) {
                prio = link.getFilePackage().getPriorityEnum();
            }
            java.util.List<DownloadLink> list = optimizedList.get(prio);
            if (list == null) {
                list = new ArrayList<DownloadLink>();
                optimizedList.put(prio, list);
            }
            list.add(link);
        }
        List<DownloadLink> newList = new ArrayList<DownloadLink>(count);
        for (Priority prio : Priority.values()) {
            List<DownloadLink> ret = optimizedList.remove(prio);
            if (ret != null) {
                newList.addAll(ret);
            }
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

    public void validateDestination(File file) throws BadDestinationException {
        if (!file.exists()) {
            File checking = null;
            try {
                String[] folders;
                switch (CrossSystem.getOSFamily()) {
                case LINUX:
                    folders = CrossSystem.getPathComponents(file);
                    if (folders.length >= 3) {
                        final String userName = System.getProperty("user.name");
                        if (folders.length >= 4 && "run".equals(folders[1]) && "media".equals(folders[2]) && folders[3].equals(userName)) {
                            /* 0:/ | 1:run | 2:media | 3:user | 4:mounted volume */
                            checking = new File("/run/media/" + userName + "/" + folders[4]);
                        } else if ("media".equals(folders[1])) {
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
                        // old windows API does not allow paths longer than that (this api is even used in the windows 7 explorer and other
                        // tools like ffmpeg)
                        checking = file;
                        throw new PathTooLongException(file);
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
                if (checking != null && checking.exists() && checking.isDirectory()) {
                    checking = null;
                }
            } catch (BadDestinationException e) {
                throw e;
            } catch (Throwable e) {
                logger.log(e);
            }
            if (checking != null) {
                logger.info("DownloadFolderRoot: " + checking + " for " + file + " is invalid! Missing or not a directory!");
                throw new BadDestinationException(checking);
            }
        }
    }

    private DownloadLinkCandidate next(DownloadLinkCandidateSelector selector) {
        final DownloadSession currentSession = selector.getSession();
        final HashMap<String, Boolean> destinationValidationCache = new HashMap<String, Boolean>();
        while (newDLStartAllowed(currentSession)) {
            final List<DownloadLinkCandidate> nextCandidates = nextDownloadLinkCandidates(selector);
            if (nextCandidates != null && nextCandidates.size() > 0) {
                DownloadLinkCandidate nextCandidate = findFinalCandidate(selector, nextCandidates);
                if (nextCandidate != null) {
                    final String destination = nextCandidate.getLink().getFilePackage().getDownloadDirectory();
                    boolean validationOk = false;
                    try {
                        final Boolean cachedValidationResult = destinationValidationCache.get(destination);
                        if (cachedValidationResult == null) {
                            validateDestination(new File(destination));
                            validationOk = true;
                        } else {
                            if (cachedValidationResult.booleanValue() == true) {
                                validationOk = true;
                            } else {
                                for (final DownloadLinkCandidate candidate : nextCandidates) {
                                    selector.addExcluded(candidate, new DownloadLinkCandidateResult(SkipReason.INVALID_DESTINATION, null, null));
                                }
                            }
                        }
                    } catch (PathTooLongException e) {
                        for (final DownloadLinkCandidate candidate : nextCandidates) {
                            selector.addExcluded(candidate, new DownloadLinkCandidateResult(SkipReason.INVALID_DESTINATION, null, null));
                        }
                    } catch (BadDestinationException e) {
                        for (final DownloadLinkCandidate candidate : nextCandidates) {
                            selector.addExcluded(candidate, new DownloadLinkCandidateResult(SkipReason.INVALID_DESTINATION, null, null));
                        }
                    } finally {
                        destinationValidationCache.put(destination, Boolean.valueOf(validationOk));
                    }
                    if (validationOk) {
                        if (DISKSPACERESERVATIONRESULT.FAILED.equals(validateDiskFree(nextCandidates))) {
                            for (final DownloadLinkCandidate candidate : nextCandidates) {
                                selector.addExcluded(candidate, new DownloadLinkCandidateResult(SkipReason.DISK_FULL, null, null));
                            }
                        } else {
                            try {
                                if (PluginForHost.implementsSortDownloadLink(nextCandidate.getCachedAccount().getPlugin())) {
                                    final ArrayList<DownloadLink> mirrors = new ArrayList<DownloadLink>();
                                    mirrors.add(0, nextCandidate.getLink());
                                    for (final DownloadLink mirror : findDownloadLinkMirrors(nextCandidate.getLink(), MirrorDetectionDecision.SAFE, false)) {
                                        if (nextCandidate.getCachedAccount().canHandle(mirror) && (mirror.getFinalLinkState() == null || !FinalLinkState.OFFLINE.equals(mirror.getFinalLinkState()))) {
                                            final DownloadLinkCandidate candidate = new DownloadLinkCandidate(nextCandidate.getLink(), nextCandidate.isForced(), nextCandidate.getCachedAccount(), nextCandidate.getProxySelector(), nextCandidate.isCustomizedAccount());
                                            final DownloadLinkCandidatePermission permission = selector.getDownloadLinkCandidatePermission(candidate);
                                            switch (permission) {
                                            case OK:
                                            case OK_FORCED:
                                            case OK_SPEED_EXTENSION:
                                                if (selector.validateDownloadLinkCandidate(candidate)) {
                                                    mirrors.add(mirror);
                                                }
                                                break;
                                            default:
                                                break;
                                            }
                                        }
                                    }
                                    final List<DownloadLink> sortedMirrors = nextCandidate.getCachedAccount().getPlugin().sortDownloadLinks(nextCandidate.getCachedAccount().getAccount(), mirrors);
                                    if (sortedMirrors != null && sortedMirrors.size() > 0 && sortedMirrors.get(0) != nextCandidate.getLink()) {
                                        nextCandidate = new DownloadLinkCandidate(sortedMirrors.get(0), nextCandidate.isForced(), nextCandidate.getCachedAccount(), nextCandidate.getProxySelector(), nextCandidate.isCustomizedAccount());
                                    }
                                }
                            } catch (final Throwable e) {
                                logger.log(e);
                            }
                            selector.setExcluded(nextCandidate.getLink());
                            final MirrorLoading condition = new MirrorLoading(nextCandidate.getLink());
                            for (DownloadLink mirror : findDownloadLinkMirrors(nextCandidate.getLink(), config.getMirrorDetectionDecision(), true)) {
                                selector.setExcluded(mirror);
                                if (mirror.getFinalLinkState() == null || FinalLinkState.CheckFailed(mirror.getFinalLinkState())) {
                                    mirror.setConditionalSkipReason(condition);
                                }
                            }
                            return nextCandidate;
                        }
                    }
                }
            } else {
                break;
            }
        }
        return null;
    }

    private DownloadLinkCandidate findFinalCandidate(DownloadLinkCandidateSelector selector, List<DownloadLinkCandidate> candidates) {
        if (candidates == null || candidates.size() == 0) {
            return null;
        }
        final LinkedHashMap<DownloadLink, LinkedHashMap<String, ArrayList<DownloadLinkCandidate>>> preferredFiltered = new LinkedHashMap<DownloadLink, LinkedHashMap<String, ArrayList<DownloadLinkCandidate>>>();
        for (DownloadLinkCandidate nextCandidate : candidates) {
            final DownloadLink candidateLink = nextCandidate.getLink();
            LinkedHashMap<String, ArrayList<DownloadLinkCandidate>> map = preferredFiltered.get(candidateLink);
            if (map == null) {
                map = new LinkedHashMap<String, ArrayList<DownloadLinkCandidate>>();
                preferredFiltered.put(candidateLink, map);
            }
            final String host = nextCandidate.getCachedAccount().getPlugin().getHost();
            ArrayList<DownloadLinkCandidate> list = map.get(host);
            if (list == null) {
                list = new ArrayList<DownloadLinkCandidate>();
                map.put(host, list);
            }
            if (list.isEmpty() || !list.get(0).isCustomizedAccount()) {
                list.add(nextCandidate);
            }
        }
        candidates = new ArrayList<DownloadLinkCandidate>();
        final Iterator<Entry<DownloadLink, LinkedHashMap<String, ArrayList<DownloadLinkCandidate>>>> it = preferredFiltered.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<DownloadLink, LinkedHashMap<String, ArrayList<DownloadLinkCandidate>>> next = it.next();
            final DownloadLink downloadLink = next.getKey();
            final LinkedHashMap<String, ArrayList<DownloadLinkCandidate>> map = next.getValue();
            final Iterator<Entry<String, ArrayList<DownloadLinkCandidate>>> it2 = map.entrySet().iterator();
            while (it2.hasNext()) {
                final Entry<String, ArrayList<DownloadLinkCandidate>> next2 = it2.next();
                final ArrayList<DownloadLinkCandidate> list = next2.getValue();
                if (list.size() > 1) {
                    final ArrayList<DownloadLinkCandidate> newList = new ArrayList<DownloadLinkCandidate>();
                    try {
                        final ArrayList<Account> accList = new ArrayList<Account>();
                        final LinkedHashMap<Account, DownloadLinkCandidate> accMap = new LinkedHashMap<Account, DownloadLinkCandidate>();
                        for (final DownloadLinkCandidate candidate : list) {
                            accMap.put(candidate.getCachedAccount().getAccount(), candidate);
                            accList.add(candidate.getCachedAccount().getAccount());
                        }
                        for (Account account : list.get(0).getCachedAccount().getPlugin().sortAccounts(downloadLink, accList)) {
                            final DownloadLinkCandidate candidate = accMap.remove(account);
                            if (candidate != null) {
                                newList.add(candidate);
                            }
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                    if (newList.size() == list.size()) {
                        candidates.addAll(newList);
                        continue;
                    }
                }
                candidates.addAll(list);
            }
        }
        final LinkedHashMap<DownloadLink, DownloadLinkCandidate> bestCandidates = new LinkedHashMap<DownloadLink, DownloadLinkCandidate>();
        for (final DownloadLinkCandidate nextCandidate : candidates) {
            final DownloadLink candidateLink = nextCandidate.getLink();
            final DownloadLinkCandidate bestCandidate = bestCandidates.get(candidateLink);
            if (bestCandidate == null) {
                /* no bestCandidate yet */
                bestCandidates.put(candidateLink, nextCandidate);
            } else if (!bestCandidate.isCustomizedAccount()) {
                /* we have a bestCandidate, check if nextCandidate would be better */
                final boolean bestHasCaptcha = bestCandidate.getCachedAccount().hasCaptcha(candidateLink);
                final boolean nextHasCaptcha = nextCandidate.getCachedAccount().hasCaptcha(candidateLink);
                switch (bestCandidate.getCachedAccount().getType()) {
                case MULTI:
                    /* our bestCandidate is a multihost one */
                    switch (nextCandidate.getCachedAccount().getType()) {
                    case ORIGINAL:
                        if (nextHasCaptcha == false) {
                            /* we always prefer originalAccount if it does not have a captcha */
                            bestCandidates.put(candidateLink, nextCandidate);
                        }
                        break;
                    case MULTI:
                        if (bestHasCaptcha && nextHasCaptcha == false) {
                            /* we replace our bestCandidate because nextCandidate does not have a captcha */
                            bestCandidates.put(candidateLink, nextCandidate);
                        }
                        break;
                    case NONE:
                        if (false && bestHasCaptcha && nextHasCaptcha == false) {
                            /* TODO */
                            /* disabled because needs to be discussed if we prefer captchaless none over captcha original/multihost */
                            /* we replace our bestCandidate with NONE because nextCandidate does not have a captcha */
                            bestCandidates.put(candidateLink, nextCandidate);
                        }
                        break;
                    }
                    break;
                case ORIGINAL:
                    /* our bestCandidate is an original one */
                    switch (nextCandidate.getCachedAccount().getType()) {
                    case MULTI:
                    case ORIGINAL:
                        if (bestHasCaptcha && nextHasCaptcha == false) {
                            /* we only replace originalAccount in case bestCandidate does have a captcha and nextCandidate does not */
                            bestCandidates.put(candidateLink, nextCandidate);
                        }
                        break;
                    case NONE:
                        if (false && bestHasCaptcha && nextHasCaptcha == false) {
                            /* TODO */
                            /* disabled because needs to be discussed if we prefer captchaless none over captcha original/multihost */
                            /* we replace our bestCandidate with NONE because nextCandidate does not have a captcha */
                            bestCandidates.put(candidateLink, nextCandidate);
                        }
                        break;
                    }
                    break;
                case NONE:
                    /* our bestCandidate is without an account */
                    switch (nextCandidate.getCachedAccount().getType()) {
                    case NONE:
                        if (bestHasCaptcha && nextHasCaptcha == false) {
                            /* we replace our bestCandidate because nextCandidate does not have a captcha */
                            bestCandidates.put(candidateLink, nextCandidate);
                        }
                        break;
                    case MULTI:
                    case ORIGINAL:
                        if (nextHasCaptcha == false) {
                            /* we replace our bestCandidate because nextCandidate does not have a captcha */
                            bestCandidates.put(candidateLink, nextCandidate);
                        } else {
                            /* we replace our bestCandidate because nextCandidate is original/multihost */
                            bestCandidates.put(candidateLink, nextCandidate);
                        }
                        break;
                    }
                    break;
                }
            }
        }
        ArrayList<DownloadLinkCandidate> finalCandidates = new ArrayList<DownloadLinkCandidate>(bestCandidates.values());
        try {
            Collections.sort(finalCandidates, new Comparator<DownloadLinkCandidate>() {
                public int compareDown(boolean x, boolean y) {
                    return (x == y) ? 0 : (x ? 1 : -1);
                }

                @Override
                public int compare(DownloadLinkCandidate x, DownloadLinkCandidate y) {
                    final int ret = x.getCachedAccount().getType().compareTo(y.getCachedAccount().getType());
                    if (ret != 0) {
                        return ret;
                    }
                    final boolean xCaptcha = x.getCachedAccount().hasCaptcha(x.getLink());
                    final boolean yCaptcha = y.getCachedAccount().hasCaptcha(y.getLink());
                    return compareDown(xCaptcha, yCaptcha);
                }
            });
        } catch (final Throwable e) {
            logger.log(e);
        }
        return finalCandidates.get(0);
    }

    private boolean isMirrorCandidate(DownloadLink linkCandidate, String cachedLinkCandidateName, DownloadLink mirrorCandidate, MirrorDetectionDecision mirrorDetectionDecision) {
        String cachedLinkMirrorID = linkCandidate.getDefaultPlugin().getMirrorID(linkCandidate);
        String mirrorCandidateMirrorID = mirrorCandidate.getDefaultPlugin().getMirrorID(mirrorCandidate);
        if (cachedLinkMirrorID != null && mirrorCandidateMirrorID != null) {
            return StringUtils.equals(cachedLinkMirrorID, mirrorCandidateMirrorID);
        }
        if (cachedLinkCandidateName == null) {
            cachedLinkCandidateName = linkCandidate.getView().getDisplayName();
        }
        final boolean sameName;
        String mirrorCandidateName = mirrorCandidate.getView().getDisplayName();
        if (CrossSystem.isWindows() || isForceMirrorDetectionCaseInsensitive()) {
            sameName = cachedLinkCandidateName.equalsIgnoreCase(mirrorCandidateName);
        } else {
            sameName = cachedLinkCandidateName.equals(mirrorCandidateName);
        }
        switch (mirrorDetectionDecision) {
        case SAFE: {
            final Boolean sameSizeResult = hasSameSize(linkCandidate, mirrorCandidate, mirrorDetectionDecision);
            if (sameSizeResult != null && Boolean.FALSE.equals(sameSizeResult)) {
                return false;
            }
            final Boolean sameHashResult = hasSameHash(linkCandidate, mirrorCandidate);
            if (sameHashResult != null && Boolean.FALSE.equals(sameHashResult)) {
                return sameHashResult;
            }
            return sameName;
        }
        case AUTO: {
            final Boolean sameSizeResult = hasSameSize(linkCandidate, mirrorCandidate, mirrorDetectionDecision);
            if (sameSizeResult != null && Boolean.FALSE.equals(sameSizeResult)) {
                return false;
            }
            final Boolean sameHashResult = hasSameHash(linkCandidate, mirrorCandidate);
            if (sameHashResult != null) {
                return sameHashResult;
            }
            return sameName;
        }
        case FILENAME:
            return sameName;
        case FILENAME_FILESIZE: {
            final Boolean sameSizeResult = hasSameSize(linkCandidate, mirrorCandidate, mirrorDetectionDecision);
            return sameName && sameSizeResult != null && Boolean.TRUE.equals(sameSizeResult);
        }
        }
        return false;
    }

    private Boolean hasSameSize(DownloadLink linkCandidate, DownloadLink mirrorCandidate, MirrorDetectionDecision mirrorDetectionDecision) {
        final int fileSizeEquality = config.getMirrorDetectionFileSizeEquality();
        final long verifiedFileSizeA = linkCandidate.getView().getBytesTotalVerified();
        final long verifiedFileSizeB = mirrorCandidate.getView().getBytesTotalVerified();
        if (fileSizeEquality == 10000 || MirrorDetectionDecision.SAFE.equals(mirrorDetectionDecision)) {
            /* 100 percent sure, only use verifiedFileSizes */
            if (verifiedFileSizeA >= 0 && verifiedFileSizeB >= 0) {
                return verifiedFileSizeA == verifiedFileSizeB;
            }
        } else {
            /* we use knownDownloadSize for check */
            final long sizeA = linkCandidate.getView().getBytesTotal();
            final long sizeB = mirrorCandidate.getView().getBytesTotal();
            if (sizeA >= 0 && sizeB >= 0) {
                final long diff = Math.abs(sizeA - sizeB);
                final int maxDiffPercent = 10000 - Math.max(1, Math.min(9999, fileSizeEquality));
                final long maxDiff = (sizeA * maxDiffPercent) / 10000;
                return diff <= maxDiff;
            }
        }
        if (config.isForceMirrorDetectionFileSizeCheck() || MirrorDetectionDecision.SAFE.equals(mirrorDetectionDecision)) {
            return false;
        } else {
            return null;
        }
    }

    private Boolean hasSameHash(final DownloadLink linkCandidate, final DownloadLink mirrorCandidate) {
        final String md5A = linkCandidate.getMD5Hash();
        final String md5B = mirrorCandidate.getMD5Hash();
        if (md5A != null && md5B != null) {
            return md5A.equalsIgnoreCase(md5B);
        }
        final String sha1A = linkCandidate.getSha1Hash();
        final String sha1B = mirrorCandidate.getSha1Hash();
        if (sha1A != null && sha1B != null) {
            return sha1A.equalsIgnoreCase(sha1B);
        }
        final String sha256A = linkCandidate.getSha256Hash();
        final String sha256B = mirrorCandidate.getSha256Hash();
        if (sha256A != null && sha256B != null) {
            return sha256A.equalsIgnoreCase(sha256B);
        }
        return null;
    }

    private List<DownloadLink> findDownloadLinkMirrors(final DownloadLink link, final MirrorDetectionDecision mirrorDetectionDecision, final boolean includeDisabledLinks) {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final FilePackage fp = link.getFilePackage();
        final String name = link.getView().getDisplayName();
        fp.getModifyLock().runReadLock(new Runnable() {
            @Override
            public void run() {
                for (final DownloadLink mirror : fp.getChildren()) {
                    if (mirror != link && (includeDisabledLinks || mirror.isEnabled()) && isMirrorCandidate(link, name, mirror, mirrorDetectionDecision)) {
                        ret.add(mirror);
                    }
                }
            }
        });
        return ret;
    }

    private void printDownloadLinkCandidateHistory(DownloadLinkCandidate candidate) {
        try {
            if (candidate != null && candidate.getLink() != null && candidate.getCachedAccount() != null) {
                final DownloadSession currentSession = getSession();
                final DownloadLinkCandidateHistory history = currentSession.getHistory(candidate.getLink());
                if (history != null) {
                    final List<DownloadLinkCandidateResult> results = history.getResults(candidate);
                    if (results != null) {
                        for (final DownloadLinkCandidateResult result : results) {
                            switch (result.getResult()) {
                            case CONDITIONAL_SKIPPED:
                                logger.info(candidate + "->" + result.getResult() + "|" + result.getConditionalSkip());
                                break;
                            case SKIPPED:
                                logger.info(candidate + "->" + result.getResult() + "|" + result.getSkipReason());
                                break;
                            default:
                                logger.info(candidate + "->" + result.getResult());
                                break;
                            }
                        }
                    }
                }
            }
        } catch (final Throwable e) {
            logger.log(e);
        }
    }

    private void setFinalLinkStatus(DownloadLinkCandidate candidate, DownloadLinkCandidateResult value, SingleDownloadController singleDownloadController) {
        final DownloadLink link = candidate.getLink();
        final DownloadSession currentSession = getSession();
        final boolean onDetach = singleDownloadController != null;
        HashResult hashResult = null;
        if (onDetach) {
            hashResult = singleDownloadController.getHashResult();
        }
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
                if (conditionalSkipReason != null) {
                    link.setConditionalSkipReason(conditionalSkipReason);
                }
                return;
            }
            break;
        case CONDITIONAL_SKIPPED:
            ConditionalSkipReason conditionalSkipReason = value.getConditionalSkip();
            if (conditionalSkipReason != null) {
                link.setConditionalSkipReason(conditionalSkipReason);
            }
            return;
        case IP_BLOCKED:
            if (onDetach) {
                long remainingTime = value.getRemainingTime();
                if (remainingTime > 0) {
                    proxyHistory.putIntoHistory(candidate, new WaitingSkipReason(CAUSE.IP_BLOCKED, remainingTime, value.getMessage()));
                }
                return;
            }
            break;
        case HOSTER_UNAVAILABLE:
            if (onDetach) {
                long remainingTime = value.getRemainingTime();
                if (remainingTime > 0) {
                    proxyHistory.putIntoHistory(candidate, new WaitingSkipReason(CAUSE.HOST_TEMP_UNAVAILABLE, remainingTime, value.getMessage()));
                }
                return;
            }
            break;
        case FILE_UNAVAILABLE:
            if (!onDetach) {
                long remaining = value.getRemainingTime();
                if (remaining > 0) {
                    link.setConditionalSkipReason(new WaitingSkipReason(CAUSE.FILE_TEMP_UNAVAILABLE, remaining, value.getMessage()));
                }
            }
            return;
        case CONNECTION_ISSUES:
            if (!onDetach) {
                long remaining = value.getRemainingTime();
                if (remaining > 0) {
                    link.setConditionalSkipReason(new WaitingSkipReason(CAUSE.CONNECTION_TEMP_UNAVAILABLE, remaining, value.getMessage()));
                }
            }
            return;
        case SKIPPED:
            if (SkipReason.NO_ACCOUNT.equals(value.getSkipReason())) {
                printDownloadLinkCandidateHistory(candidate);
            }
            currentSession.removeHistory(link);
            candidate.getLink().setSkipReason(value.getSkipReason());
            return;
        case PLUGIN_DEFECT:
            if (!onDetach) {
                currentSession.removeHistory(link);
                candidate.getLink().setFinalLinkState(FinalLinkState.PLUGIN_DEFECT);
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
                if (hashResult != null && hashResult.match()) {
                    switch (hashResult.getHashInfo().getType()) {
                    case CRC32:
                        candidate.getLink().setFinalLinkState(FinalLinkState.FINISHED_CRC32);
                        break;
                    case MD5:
                        candidate.getLink().setFinalLinkState(FinalLinkState.FINISHED_MD5);
                        break;
                    case SHA1:
                        candidate.getLink().setFinalLinkState(FinalLinkState.FINISHED_SHA1);
                        break;
                    case SHA256:
                        candidate.getLink().setFinalLinkState(FinalLinkState.FINISHED_SHA256);
                        break;
                    default:
                        candidate.getLink().setFinalLinkState(FinalLinkState.FINISHED);
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
                if (hashResult != null && hashResult.match() == false) {
                    switch (hashResult.getHashInfo().getType()) {
                    case CRC32:
                        candidate.getLink().setFinalLinkState(FinalLinkState.FAILED_CRC32);
                        break;
                    case MD5:
                        candidate.getLink().setFinalLinkState(FinalLinkState.FAILED_MD5);
                        break;
                    case SHA1:
                        candidate.getLink().setFinalLinkState(FinalLinkState.FAILED_SHA1);
                        break;
                    case SHA256:
                        candidate.getLink().setFinalLinkState(FinalLinkState.FAILED_SHA256);
                        break;
                    default:
                        candidate.getLink().setFinalLinkState(FinalLinkState.FAILED);
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
        case ACCOUNT_ERROR:
        case ACCOUNT_INVALID:
        case ACCOUNT_UNAVAILABLE:
            if (onDetach) {
                final Account account = candidate.getCachedAccount().getAccount();
                candidate.getCachedAccount().getPlugin().handleAccountException(account, null, value.getThrowable());
                return;
            }
            break;
        case ACCOUNT_REQUIRED:
            if (!onDetach) {
                printDownloadLinkCandidateHistory(candidate);
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
                candidate.getLink().setProperty(DownloadLink.PROPERTY_CUSTOM_MESSAGE, value.getMessage());
                candidate.getLink().setFinalLinkState(FinalLinkState.FAILED_FATAL);
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

    protected DISKSPACERESERVATIONRESULT validateDiskFree(final List<DownloadLinkCandidate> downloadLinkCandidates) {
        DownloadLink downloadLink = null;
        for (DownloadLinkCandidate downloadLinkCandidate : downloadLinkCandidates) {
            if (downloadLinkCandidate.getLink().getVerifiedFileSize() >= 0) {
                downloadLink = downloadLinkCandidate.getLink();
                break;
            }
        }
        if (downloadLink == null) {
            downloadLink = downloadLinkCandidates.get(0).getLink();
        }
        final File partFile = new File(downloadLink.getFileOutput() + ".part");
        final long doneSize = Math.max((partFile.exists() ? partFile.length() : 0l), downloadLink.getView().getBytesLoaded());
        final long remainingSize = downloadLink.getView().getBytesTotal() - Math.max(0, doneSize);
        final DownloadLink finalDownloadLink = downloadLink;
        return getSession().getDiskSpaceManager().check(new DiskSpaceReservation() {
            @Override
            public File getDestination() {
                return partFile;
            }

            @Override
            public long getSize() {
                final PluginForHost plugin = finalDownloadLink.getDefaultPlugin();
                if (plugin != null) {
                    return remainingSize + Math.max(0, plugin.calculateAdditionalRequiredDiskSpace(finalDownloadLink));
                }
                return remainingSize;
            }
        });
    }

    protected DISKSPACERESERVATIONRESULT validateDiskFree(DiskSpaceReservation reservation) {
        return getSession().getDiskSpaceManager().check(reservation);
    }

    private List<DownloadLinkCandidate> nextDownloadLinkCandidates(DownloadLinkCandidateSelector selector) {
        final DownloadSession currentSession = selector.getSession();
        final boolean skipAllCaptchas = CaptchaSettings.MODE.SKIP_ALL.equals(selector.getSession().getCaptchaMode());
        while (!selector.isStopMarkReached() && newDLStartAllowed(currentSession)) {
            final DownloadLinkCandidate nextSelectedCandidate = nextDownloadLinkCandidate(selector);
            if (nextSelectedCandidate == null) {
                break;
            }
            final List<DownloadLinkCandidate> nextSelectedCandidates = new ArrayList<DownloadLinkCandidate>();
            nextSelectedCandidates.add(nextSelectedCandidate);
            if (!nextSelectedCandidate.isForced() && selector.isMirrorManagement()) {
                nextSelectedCandidates.addAll(findDownloadLinkCandidateMirrors(selector, nextSelectedCandidate));
            }
            /**
             * filter captcha stuff and canHandle==false
             */
            final List<DownloadLinkCandidate> checkNextCandidatesStage1 = new ArrayList<DownloadLinkCandidate>();
            for (final DownloadLinkCandidate candidate : nextSelectedCandidates) {
                final DownloadLink link = candidate.getLink();
                selector.addExcluded(link);
                final AccountCache accountCache = currentSession.getAccountCache(link);
                boolean ok = false;
                CachedAccount waitForAccount = null;
                ConditionalSkipReason conditionalSkipReason = null;
                for (final CachedAccount cachedAccount : accountCache) {
                    final CachedAccountPermission permission = selector.getCachedAccountPermission(cachedAccount);
                    try {
                        if (CachedAccountPermission.OK.equals(permission)) {
                            if (cachedAccount.canHandle(link)) {
                                final DownloadLinkCandidate checkNextCandidate = new DownloadLinkCandidate(candidate, cachedAccount, accountCache.isCustomizedCache());
                                ok = true;
                                if (cachedAccount.hasCaptcha(link) && (skipAllCaptchas || CaptchaBlackList.getInstance().matches(new PrePluginCheckDummyChallenge(link)) != null)) {
                                    if (selector.validateDownloadLinkCandidate(checkNextCandidate)) {
                                        selector.addExcluded(candidate, new DownloadLinkCandidateResult(SkipReason.CAPTCHA, null, null));
                                    }
                                    continue;
                                }
                                checkNextCandidatesStage1.add(checkNextCandidate);
                            }
                        } else if (CachedAccountPermission.TEMP_DISABLED.equals(permission)) {
                            if (cachedAccount.canHandle(link)) {
                                if (waitForAccount == null || waitForAccount.getAccount().getTmpDisabledTimeout() > cachedAccount.getAccount().getTmpDisabledTimeout()) {
                                    waitForAccount = cachedAccount;
                                }
                            }
                        }
                    } catch (final ConditionalSkipReasonException e) {
                        final ConditionalSkipReason skipReason = e.getConditionalSkipReason();
                        if (skipReason != null) {
                            if (conditionalSkipReason == null) {
                                conditionalSkipReason = skipReason;
                            } else {
                                if (skipReason instanceof TimeOutCondition && conditionalSkipReason instanceof TimeOutCondition) {
                                    if (((TimeOutCondition) conditionalSkipReason).getTimeOutLeft() < ((TimeOutCondition) skipReason).getTimeOutLeft()) {
                                        conditionalSkipReason = skipReason;
                                    }
                                }
                            }
                        } else {
                            logger.log(e);
                        }
                    } catch (final Exception e) {
                        logger.log(e);
                    }
                }
                if (!ok) {
                    if (conditionalSkipReason != null) {
                        selector.addExcluded(candidate, new DownloadLinkCandidateResult(conditionalSkipReason, null, null));
                    } else if (waitForAccount != null) {
                        selector.addExcluded(candidate, new DownloadLinkCandidateResult(new WaitForAccountSkipReason(waitForAccount.getAccount()), null, null));
                    } else {
                        selector.addExcluded(candidate, new DownloadLinkCandidateResult(RESULT.ACCOUNT_REQUIRED, null, null));
                    }
                }
            }
            if (checkNextCandidatesStage1.size() > 0) {
                /**
                 * find all possible proxySelectors
                 */
                final List<DownloadLinkCandidate> checkNextCandidatesStage2 = new ArrayList<DownloadLinkCandidate>();
                final boolean[][] flags = new boolean[][] { { false, false }, { true, false }, { true, true } };
                for (boolean[] flag : flags) {
                    final Iterator<DownloadLinkCandidate> it = checkNextCandidatesStage1.iterator();
                    while (it.hasNext()) {
                        final DownloadLinkCandidate candidate = it.next();
                        final List<AbstractProxySelectorImpl> proxySelectors = selector.getProxies(candidate, flag[0], flag[1]);
                        if (proxySelectors.size() > 0) {
                            it.remove();
                            for (AbstractProxySelectorImpl proxySelector : proxySelectors) {
                                checkNextCandidatesStage2.add(new DownloadLinkCandidate(candidate, proxySelector));
                            }
                        }
                    }
                    if (checkNextCandidatesStage2.size() > 0) {
                        /**
                         * skip next flagset because we have already found possible proxySelectors
                         */
                        break;
                    }
                }
                /**
                 * handle all candidates without a proxySelector
                 */
                for (final DownloadLinkCandidate notPossibleCandidate : checkNextCandidatesStage1) {
                    try {
                        if (selector.validateDownloadLinkCandidate(notPossibleCandidate)) {
                            selector.addExcluded(notPossibleCandidate, new DownloadLinkCandidateResult(SkipReason.CONNECTION_UNAVAILABLE, null, null));
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                        selector.addExcluded(notPossibleCandidate, new DownloadLinkCandidateResult(RESULT.PLUGIN_DEFECT, null, null));
                    }
                }
                final List<DownloadLinkCandidate> nextCandidates = new ArrayList<DownloadLinkCandidate>();
                final Iterator<DownloadLinkCandidate> it = checkNextCandidatesStage2.iterator();
                while (it.hasNext()) {
                    final DownloadLinkCandidate candidate = it.next();
                    try {
                        final DownloadLinkCandidatePermission permission = selector.getDownloadLinkCandidatePermission(candidate);
                        switch (permission) {
                        case OK:
                        case OK_FORCED:
                        case OK_SPEED_EXTENSION:
                            if (selector.validateDownloadLinkCandidate(candidate)) {
                                nextCandidates.add(candidate);
                            }
                            break;
                        case CONCURRENCY_FORBIDDEN:
                        case CONCURRENCY_LIMIT:
                            if (selector.validateDownloadLinkCandidate(candidate)) {
                                selector.addExcluded(candidate, new DownloadLinkCandidateResult(RESULT.CONNECTION_TEMP_UNAVAILABLE, null, null));
                            }
                            break;
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                        selector.addExcluded(candidate, new DownloadLinkCandidateResult(RESULT.PLUGIN_DEFECT, null, null));
                    }
                }
                if (nextCandidates.size() > 0) {
                    return nextCandidates;
                }
            }
        }
        return null;
    }

    private List<DownloadLinkCandidate> findDownloadLinkCandidateMirrors(DownloadLinkCandidateSelector selector, DownloadLinkCandidate candidate) {
        final List<DownloadLinkCandidate> ret = new ArrayList<DownloadLinkCandidate>();
        if (candidate.isForced()) {
            return ret;
        }
        final DownloadLink link = candidate.getLink();
        final FilePackage filePackage = link.getFilePackage();
        final String name = link.getView().getDisplayName();
        final ArrayList<DownloadLink> removeList = new ArrayList<DownloadLink>();
        final MirrorDetectionDecision mirrorDetectionDecision = config.getMirrorDetectionDecision();
        for (DownloadLink next : getSession().getActivationRequests()) {
            if (next == link) {
                continue;
            }
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
            if (isMirrorCandidate(link, name, next, mirrorDetectionDecision)) {
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
        if (next.getAvailableStatus() == AvailableStatus.FALSE) {
            return true;
        }
        if (next.getFinalLinkState() != null) {
            return true;
        }
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
        if (next.getDownloadLinkController() != null) {
            /* download is in progress */
            return true;
        }
        ConditionalSkipReason conditionalSkipReason = next.getConditionalSkipReason();
        if (conditionalSkipReason != null) {
            if (conditionalSkipReason instanceof IgnorableConditionalSkipReason && ((IgnorableConditionalSkipReason) conditionalSkipReason).canIgnore()) {
                return true;
            }
            if (!(conditionalSkipReason instanceof ValidatableConditionalSkipReason) || ((ValidatableConditionalSkipReason) conditionalSkipReason).isValid()) {
                return true;
            }
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
                        if (selector.isExcluded(next)) {
                            continue;
                        }
                        if (!avoidReHandle.add(next)) {
                            continue;
                        }
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
        final DownloadWatchDogJob job;
        synchronized (watchDogJobs) {
            job = watchDogJobs.peek();
        }
        if (job != null && job.isHighPriority()) {
            return false;
        }
        final SessionState sessionState = session.getSessionState();
        if (sessionState == DownloadSession.SessionState.RECONNECT_RUNNING) {
            /* reconnect in progress */
            return false;
        }
        final boolean isForcedLinksWaiting = session.isForcedLinksWaiting();
        if (isForcedLinksWaiting == false) {
            if (!DownloadWatchDog.this.stateMachine.isState(DownloadWatchDog.RUNNING_STATE)) {
                /*
                 * only allow new downloads in running state
                 */
                return false;
            }
            if (sessionState == DownloadSession.SessionState.RECONNECT_REQUESTED && isAutoReconnectEnabled() && CFG_RECONNECT.CFG.isDownloadControllerPrefersReconnectEnabled()) {
                /*
                 * auto reconnect is enabled and downloads are waiting for reconnect and user set to wait for reconnect
                 */
                return false;
            }
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
                    if (beforeEnabled != null) {
                        config.setDownloadSpeedLimitEnabled(beforeEnabled);
                    }
                    if (stateMachine.isState(DownloadWatchDog.PAUSE_STATE)) {
                        /* we revert pause to running state */
                        stateMachine.setStatus(RUNNING_STATE);
                    }
                }
            }

            @Override
            public void interrupt() {
            }

            @Override
            public boolean isHighPriority() {
                return true;
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
                listener.onDownloadWatchDogPropertyChange(new DownloadWatchDogProperty(DownloadWatchDogProperty.Property.STOPSIGN, currentSession.getStopMark()));
            }

            @Override
            public void interrupt() {
            }

            @Override
            public boolean isHighPriority() {
                return true;
            }
        });
    }

    public long getDownloadSpeedbyFilePackage(FilePackage pkg) {
        long speed = -1;
        for (SingleDownloadController con : getSession().getControllers()) {
            if (con.getDownloadLink().getFilePackage() != pkg) {
                continue;
            }
            DownloadInterface dli = con.getDownloadInstance();
            if (dli == null) {
                continue;
            }
            speed += dli.getManagedConnetionHandler().getSpeed();
        }
        return speed;
    }

    public int getDownloadsbyFilePackage(FilePackage pkg) {
        int ret = 0;
        for (SingleDownloadController con : getSession().getControllers()) {
            if (con.getDownloadLink().getFilePackage() != pkg) {
                continue;
            }
            ret++;
        }
        return ret;
    }

    public Set<SingleDownloadController> getRunningDownloadLinks() {
        return getSession().getControllers();
    }

    public Set<FilePackage> getRunningFilePackages() {
        HashSet<FilePackage> ret = new HashSet<FilePackage>();
        for (SingleDownloadController con : getSession().getControllers()) {
            ret.add(con.getDownloadLink().getParentNode());
        }
        return ret;
    }

    public boolean hasRunningDownloads(FilePackage pkg) {
        for (SingleDownloadController con : getSession().getControllers()) {
            if (con.getDownloadLink().getFilePackage() == pkg) {
                return true;
            }
        }
        return false;
    }

    /**
     * aborts all running SingleDownloadControllers, NOTE: DownloadWatchDog is still running, new Downloads will can started after this call
     */
    public void abortAllSingleDownloadControllers() {
        if (isWatchDogThread()) {
            throw new WTFException("it is not possible to use this method inside WatchDogThread!");
        }
        while (true) {
            for (SingleDownloadController con : getSession().getControllers()) {
                if (con.isAlive() && !con.isAborting()) {
                    con.abort();
                }
            }
            synchronized (WATCHDOGLOCK) {
                if (getSession().getControllers().size() == 0) {
                    return;
                }
                try {
                    WATCHDOGLOCK.wait(1000);
                } catch (InterruptedException e) {
                    logger.log(e);
                    return;
                }
            }
        }
    }

    public void enqueueJob(final DownloadWatchDogJob job) {
        synchronized (watchDogJobs) {
            if (job.isHighPriority()) {
                final DownloadWatchDogJob first = watchDogJobs.peekFirst();
                final DownloadWatchDogJob last = watchDogJobs.peekLast();
                if (first == null || !first.isHighPriority()) {
                    watchDogJobs.offerFirst(job);// offerFirst because list is empty or first one is non high priority
                } else if (last.isHighPriority()) {
                    watchDogJobs.offerLast(job);// offerLast because last one is high priority
                } else {
                    boolean jobOffered = false;
                    final ListIterator<DownloadWatchDogJob> it = watchDogJobs.listIterator();
                    while (it.hasNext()) {
                        final DownloadWatchDogJob next = it.next();
                        if (next.isHighPriority()) {
                            continue;
                        } else {
                            // add after last(it.previous) high priority
                            it.previous();
                            it.add(job);
                            jobOffered = true;
                            break;
                        }
                    }
                    if (!jobOffered) {
                        watchDogJobs.offerLast(job);
                    }
                }
            } else {
                watchDogJobs.offerLast(job);
            }
        }
        if (!isWatchDogThread()) {
            synchronized (WATCHDOGLOCK) {
                WATCHDOGLOCK.notifyAll();
            }
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

            @Override
            public boolean isHighPriority() {
                return true;
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

            @Override
            public boolean isHighPriority() {
                return true;
            }
        });
    }

    public void reset(final List<DownloadLink> resetLinks) {
        if (resetLinks == null || resetLinks.size() == 0) {
            return;
        }
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

                            @Override
                            public boolean isHighPriority() {
                                return false;
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

            @Override
            public boolean isHighPriority() {
                return true;
            }
        });
    }

    private void resumeLink(DownloadLink link, DownloadSession session) {
        if (link.getDownloadLinkController() != null) {
            throw new IllegalStateException("Link is in progress! cannot resume!");
        }
        final FinalLinkState finalLinkState = link.getFinalLinkState();
        if (!FinalLinkState.CheckFinished(finalLinkState)) {
            if (FinalLinkState.CheckFailed(finalLinkState)) {
                link.setFinalLinkState(null);
            }
            unSkipLink(link, session);
            if (!link.isEnabled()) {
                link.setEnabled(true);
            }
        }
        final DownloadLinkCandidateHistory history = session.getHistory(link);
        final List<PluginForHost> plugins = getPluginsFromHistory(link, history);
        link.resume(new ArrayList<PluginForHost>(plugins));
    }

    private void unSkipLink(DownloadLink link, DownloadSession session) {
        if (link.isSkipped()) {
            if (SkipReason.FFMPEG_MISSING.equals(link.getSkipReason()) || SkipReason.UPDATE_RESTART_REQUIRED.equals(link.getSkipReason())) {
                getSession().removeProperty(org.jdownloader.controlling.ffmpeg.FFmpegProvider.FFMPEG_INSTALL_CHECK);
            }
            link.setSkipReason(null);
        }
        CaptchaBlackList.getInstance().addWhitelist(link);
    }

    private List<PluginForHost> getPluginsFromHistory(DownloadLink link, DownloadLinkCandidateHistory history) {
        final HashSet<PluginForHost> plugins = new HashSet<PluginForHost>();
        if (history != null) {
            for (DownloadLinkCandidate candidate : history.getHistory().keySet()) {
                plugins.add(candidate.getCachedAccount().getPlugin());
            }
        }
        final PluginForHost defaultPlugin = link.getDefaultPlugin();
        if (defaultPlugin != null) {
            plugins.add(defaultPlugin);
        }
        return new ArrayList<PluginForHost>(plugins);
    }

    private void resetLink(DownloadLink link, DownloadSession session) {
        if (link.getDownloadLinkController() != null) {
            throw new IllegalStateException("Link is in progress! cannot reset!");
        }
        final DownloadLinkCandidateHistory history = session.removeHistory(link);
        final List<WaitingSkipReasonContainer> list = session.getProxyInfoHistory().list(link.getHost());
        if (list != null) {
            for (WaitingSkipReasonContainer container : list) {
                container.invalidate();
            }
        }
        deleteFile(link, DeleteOption.NULL);
        unSkipLink(link, session);
        final List<PluginForHost> plugins = getPluginsFromHistory(link, history);
        link.reset(plugins);
    }

    public void delete(final List<DownloadLink> deleteFiles, final DeleteOption deleteTo) {
        delete(deleteFiles, deleteTo, false);
    }

    public Map<DownloadLink, Map<File, Boolean>> delete(final List<DownloadLink> deleteFiles, final DeleteOption deleteTo, final boolean waitForDeletion) {
        if (deleteFiles == null || deleteFiles.size() == 0) {
            return null;
        }
        final HashSet<DownloadLink> todo = new HashSet<DownloadLink>(deleteFiles);
        final Map<DownloadLink, Map<File, Boolean>> ret = new HashMap<DownloadLink, Map<File, Boolean>>();
        enqueueJob(new DownloadWatchDogJob() {
            @Override
            public void execute(DownloadSession currentSession) {
                if (currentSession != null) {
                    currentSession.getActivationRequests().removeAll(todo);
                    currentSession.getForcedLinks().removeAll(todo);
                }
                for (final DownloadLink link : todo) {
                    final SingleDownloadController con = link.getDownloadLinkController();
                    if (con == null || con.isAlive() == false) {
                        /* link has no/no alive singleDownloadController, so delete it now */
                        Map<File, Boolean> result = null;
                        try {
                            result = deleteFile(link, deleteTo);
                        } finally {
                            synchronized (ret) {
                                ret.put(link, result);
                                ret.notifyAll();
                            }
                        }
                    } else {
                        /* link has a running singleDownloadController, abort it and delete it after */
                        con.abort();
                        con.getJobsAfterDetach().add(new DownloadWatchDogJob() {
                            @Override
                            public void execute(DownloadSession currentSession) {
                                /* now we can delete the link */
                                Map<File, Boolean> result = null;
                                try {
                                    deleteFile(link, deleteTo);
                                } finally {
                                    synchronized (ret) {
                                        ret.put(link, result);
                                        ret.notifyAll();
                                    }
                                }
                                return;
                            }

                            @Override
                            public void interrupt() {
                            }

                            @Override
                            public boolean isHighPriority() {
                                return false;
                            }
                        });
                    }
                }
                return;
            }

            @Override
            public void interrupt() {
            }

            @Override
            public boolean isHighPriority() {
                return false;
            }
        });
        if (waitForDeletion) {
            while (true) {
                synchronized (ret) {
                    if (ret.size() != todo.size()) {
                        try {
                            ret.wait();
                        } catch (InterruptedException e) {
                            logger.log(e);
                            return new HashMap<DownloadLink, Map<File, Boolean>>(ret);
                        }
                    } else {
                        break;
                    }
                }
            }
            return ret;
        } else {
            return null;
        }
    }

    private Map<File, Boolean> deleteFile(DownloadLink link, DeleteOption deleteTo) {
        if (deleteTo == null) {
            deleteTo = DeleteOption.NULL;
        }
        if (DeleteOption.NO_DELETE == deleteTo) {
            return null;
        }
        final Map<File, Boolean> deleteFiles = new HashMap<File, Boolean>();
        try {
            for (final File deleteFile : link.getDefaultPlugin().listProcessFiles(link)) {
                if (deleteFile.exists() && deleteFile.isFile()) {
                    try {
                        getSession().getFileAccessManager().lock(deleteFile, this);
                        deleteFiles.put(deleteFile, null);
                    } catch (FileIsLockedException e) {
                        logger.log(e);
                    }
                }
            }
            for (final File deleteFile : new ArrayList<File>(deleteFiles.keySet())) {
                switch (deleteTo) {
                case NULL:
                    if (deleteFile.delete() == false) {
                        deleteFiles.put(deleteFile, Boolean.FALSE);
                        logger.info("Could not delete file: " + deleteFile + " for " + link + " exists: " + deleteFile.exists());
                    } else {
                        deleteFiles.put(deleteFile, Boolean.TRUE);
                        logger.info("Deleted file: " + deleteFile + " for " + link);
                    }
                    break;
                case RECYCLE:
                    try {
                        JDFileUtils.moveToTrash(deleteFile);
                        deleteFiles.put(deleteFile, Boolean.TRUE);
                        logger.info("Recycled file: " + deleteFile + " for " + link);
                    } catch (IOException e) {
                        deleteFiles.put(deleteFile, Boolean.FALSE);
                        logger.log(e);
                        logger.info("Could not recycle file: " + deleteFile + " for " + link + " exists: " + deleteFile.exists());
                    }
                    break;
                case NO_DELETE:
                    return null;
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
            return deleteFiles;
        } finally {
            for (File deleteFile : deleteFiles.keySet()) {
                getSession().getFileAccessManager().unlock(deleteFile, this);
            }
        }
    }

    public void abort(final List<DownloadLink> abortLinks) {
        enqueueJob(new DownloadWatchDogJob() {
            @Override
            public void execute(DownloadSession currentSession) {
                for (final DownloadLink link : abortLinks) {
                    final SingleDownloadController con = link.getDownloadLinkController();
                    if (con != null) {
                        con.abort();
                    }
                }
                return;
            }

            @Override
            public void interrupt() {
            }

            @Override
            public boolean isHighPriority() {
                return true;
            }
        });
    }

    /**
     * activates new Downloads as long as possible and returns how many got activated
     *
     * @param downloadLinksWithConditionalSkipReasons
     *
     * @return
     **/
    private List<SingleDownloadController> activateDownloads(final List<DownloadLink> downloadLinksWithConditionalSkipReasons) throws Exception {
        final ArrayList<SingleDownloadController> ret = new ArrayList<SingleDownloadController>();
        final DownloadSession session = getSession();
        final DownloadLinkCandidateSelector selector = new DownloadLinkCandidateSelector(session);
        int maxConcurrentNormal = Math.max(1, config.getMaxSimultaneDownloads());
        maxConcurrentNormal = Math.max(maxConcurrentNormal, DomainRuleController.getInstance().getMaxSimultanDownloads());
        int maxConcurrentForced = maxConcurrentNormal + Math.max(0, config.getMaxForcedDownloads());
        int loopCounter = 0;
        final boolean invalidate = session.setCandidatesRefreshRequired(false);
        try {
            if (invalidate && downloadLinksWithConditionalSkipReasons != null) {
                for (final DownloadLink candidate : downloadLinksWithConditionalSkipReasons) {
                    final ConditionalSkipReason con = candidate.getConditionalSkipReason();
                    if (con != null && con instanceof ValidatableConditionalSkipReason) {
                        ((ValidatableConditionalSkipReason) con).invalidate();
                    }
                }
            }
            if (session.isForcedLinksWaiting()) {
                /* first process forcedLinks */
                selector.setForcedOnly(true);
                loopCounter = 0;
                while (this.newDLStartAllowed(session) && session.isForcedLinksWaiting() && (getActiveDownloads() < maxConcurrentForced) && loopCounter < maxConcurrentForced) {
                    try {
                        final DownloadLinkCandidate candidate = this.next(selector);
                        if (candidate != null) {
                            ret.add(attach(candidate));
                        } else {
                            break;
                        }
                    } finally {
                        loopCounter++;
                    }
                }
            }
            if (!session.isStopMarkReached()) {
                selector.setForcedOnly(false);
                /* then process normal activationRequests */
                final boolean canExceed = DomainRuleController.getInstance().getMaxSimultanDownloads() > 0;
                // heckForAdditionalDownloadSlots(session)
                while (this.newDLStartAllowed(session)) {
                    if (!canExceed) {
                        // no rules that may exceed the global download limits
                        if (getActiveDownloads() >= maxConcurrentNormal && !checkForAdditionalDownloadSlots(session)) {
                            break;
                        }
                    }
                    final DownloadLinkCandidate candidate = this.next(selector);
                    if (candidate != null) {
                        ret.add(attach(candidate));
                    } else {
                        break;
                    }
                }
            }
        } finally {
            finalize(selector, downloadLinksWithConditionalSkipReasons);
        }
        return ret;
    }

    public boolean checkForAdditionalDownloadSlots(final DownloadSession session) {
        final long autoMaxDownloadSpeedLimit = config.getAutoMaxDownloadsSpeedLimit();
        final int autoMaxDownloadsMaxDownloads = config.getAutoMaxDownloadsSpeedLimitMaxDownloads();
        final int autoMaxDownloadsMinDelay = config.getAutoMaxDownloadsSpeedLimitMinDelay();
        final Set<SingleDownloadController> controllers = session.getControllers();
        if (autoMaxDownloadSpeedLimit > 0 && (autoMaxDownloadsMaxDownloads == 0 || controllers.size() < autoMaxDownloadsMaxDownloads)) {
            try {
                final long currentDownloadSpeed = getDownloadSpeedManager().getSpeedMeter().getSpeedMeter();
                if (currentDownloadSpeed < autoMaxDownloadSpeedLimit) {
                    final int speedlimit = config.isDownloadSpeedLimitEnabled() ? config.getDownloadSpeedLimit() : 0;
                    if (speedlimit > 0 && currentDownloadSpeed > speedlimit * 0.8) {
                        // do not start a new download: speedlimit is set, and the speed is almost at the limit
                        return false;
                    }
                    long latestStart = 0;
                    for (final SingleDownloadController s : controllers) {
                        latestStart = Math.max(latestStart, s.getStartTimestamp());
                        long left = s.getDownloadLink().getView().getBytesTotal() - s.getDownloadLink().getView().getBytesLoaded();
                        if (left <= 0) {
                            // download done - like mega.
                            continue;
                        }
                        if (s.getDownloadLink().getView().getSpeedBps() <= 0) {
                            // do not start a new download: not all downloads are running.
                            return false;
                        }
                    }
                    if (System.currentTimeMillis() - latestStart < autoMaxDownloadsMinDelay) {
                        // do not start a new download: latest start is less then 5 secs ago
                        return false;
                    }
                    return true;
                }
                // just to be sure
            } catch (Throwable e) {
                logger.log(e);
            }
        }
        // do not start a new download.
        return false;
    }

    private void finalize(final DownloadLinkCandidateSelector selector, final List<DownloadLink> downloadLinksWithConditionalSkipReasons) {
        if (downloadLinksWithConditionalSkipReasons != null) {
            for (final DownloadLink link : downloadLinksWithConditionalSkipReasons) {
                final ConditionalSkipReason conditionalSkipReason = link.getConditionalSkipReason();
                if (conditionalSkipReason != null) {
                    if (conditionalSkipReason instanceof ValidatableConditionalSkipReason) {
                        if (!((ValidatableConditionalSkipReason) conditionalSkipReason).isValid()) {
                            link.setConditionalSkipReason(null);
                        }
                    }
                    if (conditionalSkipReason.isConditionReached()) {
                        link.setConditionalSkipReason(null);
                        conditionalSkipReason.finalize(link);
                    }
                }
            }
        }
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

            @Override
            public boolean isHighPriority() {
                return true;
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
        logger.info("Start new Download: Use Premium: " + getSession().isUseAccountsEnabled() + " Host:" + candidate);
        DownloadLinkCandidateHistory history = getSession().buildHistory(candidate.getLink());
        if (history == null || !history.attach(candidate)) {
            logger.severe("Could not attach to History: " + candidate);
        }
        final SingleDownloadController con = new SingleDownloadController(candidate, this);
        DownloadController.getInstance().getQueue().addWait(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                boolean ignoreUnsafe = true;
                String downloadTo = candidate.getLink().getFileOutput(ignoreUnsafe, false);
                if (StringUtils.isEmpty(downloadTo)) {
                    ignoreUnsafe = false;
                    downloadTo = candidate.getLink().getFileOutput(ignoreUnsafe, false);
                }
                String customDownloadTo = candidate.getLink().getFileOutput(ignoreUnsafe, true);
                if (ignoreUnsafe) {
                    logger.info("Download To: " + downloadTo);
                } else {
                    logger.info("Download To(Unsafe): " + downloadTo);
                }
                if (!StringUtils.equalsIgnoreCase(downloadTo, customDownloadTo)) {
                    logger.info("Download To(custom): " + customDownloadTo);
                }
                con.setSessionDownloadDirectory(candidate.getLink().getParentNode().getDownloadDirectory());
                return null;
            }
        });
        con.setSessionDownloadFilename(candidate.getLink().getForcedFileName());
        if (candidate.getProxySelector() != null) {
            candidate.getProxySelector().add(con);
        }
        candidate.getLink().setEnabled(true);
        getSession().getControllers().add(con);
        con.start();
        return con;
    }

    protected void detach(final SingleDownloadController singleDownloadController, final SingleDownloadReturnState returnState) {
        enqueueJob(new DownloadWatchDogJob() {
            @Override
            public void execute(DownloadSession currentSession) {
                LogSource logger = null;
                try {
                    if (singleDownloadController.getLogger() instanceof LogSource) {
                        logger = (LogSource) singleDownloadController.getLogger();
                    }
                    if (logger == null) {
                        logger = new LogSource("Dummy") {
                            @Override
                            public synchronized void clear() {
                            }

                            public synchronized void log(java.util.logging.LogRecord record) {
                                DownloadWatchDog.this.logger.log(record);
                            };
                        };
                    }
                    final DownloadLinkCandidate candidate = singleDownloadController.getDownloadLinkCandidate();
                    final DownloadLink link = candidate.getLink();
                    if (candidate.getProxySelector() != null) {
                        candidate.getProxySelector().remove(singleDownloadController);
                    }
                    DownloadLinkCandidateResult result = null;
                    try {
                        try {
                            result = handleReturnState(logger, singleDownloadController, returnState);
                            result.setStartTime(singleDownloadController.getStartTimestamp());
                            result.setFinishTime(returnState.getTimeStamp());
                            final DownloadLinkCandidateHistory existingHistory = currentSession.getHistory(link);
                            setFinalLinkStatus(candidate, result, singleDownloadController);
                            if (existingHistory != null && !existingHistory.dettach(candidate, result)) {
                                DownloadWatchDog.this.logger.severe("Could not detach from History: " + candidate);
                            }
                        } catch (final Throwable e) {
                            DownloadWatchDog.this.logger.log(e);
                        }
                        currentSession.getControllers().remove(singleDownloadController);
                        for (final DownloadWatchDogJob job : singleDownloadController.getJobsAfterDetach()) {
                            try {
                                job.execute(currentSession);
                            } catch (Throwable e) {
                                DownloadWatchDog.this.logger.log(e);
                            }
                        }
                        try {
                            logger.info("Rename after Download?");
                            final File desiredPath = new File(link.getFileOutput(false, false));
                            logger.info("Desired Path: " + desiredPath);
                            final File usedPath = singleDownloadController.getFileOutput(false, false);
                            logger.info("Actually Used path: " + usedPath);
                            if (!desiredPath.equals(usedPath)) {
                                logger.info("Move");
                                move(link, usedPath.getParent(), usedPath.getName(), desiredPath.getParent(), desiredPath.getName());
                            }
                        } catch (final Throwable e) {
                            DownloadWatchDog.this.logger.log(e);
                        }
                        /* after each download, the order/position of next downloadCandidate could have changed */
                        currentSession.refreshCandidates();
                        final FilePackage fp = link.getLastValidFilePackage();
                        try {
                            fp.getView().requestUpdate();
                        } catch (final Throwable e) {
                            /* link can already be removed->nullpointer exception */
                        }
                        final Set<DownloadLink> finalizedLinks;
                        if (fp != null && !FilePackage.isDefaultFilePackage(fp)) {
                            final boolean readL = fp.getModifyLock().readLock();
                            try {
                                finalizedLinks = finalizeConditionalSkipReasons(fp.getChildren(), null);
                            } finally {
                                fp.getModifyLock().readUnlock(readL);
                            }
                        } else {
                            finalizedLinks = null;
                        }
                        try {
                            eventSender.fireEvent(new DownloadWatchdogEvent(this, DownloadWatchdogEvent.Type.LINK_STOPPED, singleDownloadController, candidate, result));
                        } catch (final Throwable e) {
                            DownloadWatchDog.this.logger.log(e);
                        }
                        handleFinalLinkStates(finalizedLinks, currentSession, DownloadWatchDog.this.logger, singleDownloadController);
                    } finally {
                        if (result != null) {
                            // cleanup
                            //
                            // result.setThrowable(null);
                        }
                    }
                } catch (final Throwable e) {
                    DownloadWatchDog.this.logger.log(e);
                    if (logger != null) {
                        logger.log(e);
                    }
                } finally {
                    if (logger != null) {
                        logger.close();
                    }
                }
                return;
            }

            @Override
            public void interrupt() {
            }

            @Override
            public boolean isHighPriority() {
                return false;
            }
        });
    }

    private void handleFinalLinkStates(Set<DownloadLink> links, DownloadSession session, final LogSource logger, final SingleDownloadController singleDownloadController) {
        if ((links == null || links.size() == 0) && singleDownloadController == null) {
            return;
        }
        if (links == null) {
            links = new HashSet<DownloadLink>();
        }
        final DownloadLink singleDownloadControllerLink;
        if (singleDownloadController != null) {
            singleDownloadControllerLink = singleDownloadController.getDownloadLinkCandidate().getLink();
            links.add(singleDownloadControllerLink);
        } else {
            singleDownloadControllerLink = null;
        }
        final Iterator<DownloadLink> it = links.iterator();
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
        if (cleanupLinks.size() > 0 || cleanupPackages.size() > 0) {
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
                                        if (DownloadController.getInstance() == cleanupLink.getFilePackage().getControlledBy()) {
                                            String name = cleanupLink.getView().getDisplayName();
                                            logger.info("Remove Link " + name + " because " + cleanupLink.getFinalLinkState() + " and CleanupImmediately!");
                                            List<DownloadLink> remove = new ArrayList<DownloadLink>();
                                            remove.add(cleanupLink);
                                            remove = DownloadController.getInstance().askForRemoveVetos(singleDownloadController, remove);
                                            if (remove.size() > 0) {
                                                DownloadController.getInstance().removeChildren(remove);
                                            } else {
                                                logger.info("Remove Link " + name + " failed because of removeVetos!");
                                            }
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
                                DownloadController.removePackageIfFinished(singleDownloadController, logger, filePackage);
                            }
                        }
                        break;
                    }
                    return null;
                }
            });
        }
    }

    private DownloadLinkCandidateResult handleReturnState(LogSource logger, SingleDownloadController singleDownloadController, SingleDownloadReturnState result) {
        final Throwable throwable = result.getCaughtThrowable();
        final DownloadLinkCandidate candidate = singleDownloadController.getDownloadLinkCandidate();
        final DownloadLink link = candidate.getLink();
        long sizeChange = Math.max(0, link.getView().getBytesLoaded() - singleDownloadController.getSizeBefore());
        final Account account = singleDownloadController.getAccount();
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
        PluginForHost latestPlugin = null;
        final String pluginHost;
        if ((latestPlugin = result.getLatestPlugin()) != null) {
            pluginHost = latestPlugin.getLazyP().getHost();
        } else {
            pluginHost = candidate.getCachedAccount().getPlugin().getLazyP().getHost();
        }
        if (throwable != null) {
            if (throwable instanceof PluginException) {
                pluginException = (PluginException) throwable;
            } else if (throwable instanceof SkipReasonException) {
                skipReason = ((SkipReasonException) throwable).getSkipReason();
            } else if (throwable instanceof ConditionalSkipReasonException) {
                conditionalSkipReason = ((ConditionalSkipReasonException) throwable).getConditionalSkipReason();
            } else if (throwable instanceof NoInternetConnection) {
                DownloadLinkCandidateResult ret = new DownloadLinkCandidateResult(RESULT.CONNECTION_ISSUES, throwable, pluginHost);
                ret.setWaitTime(JsonConfig.create(GeneralSettings.class).getNetworkIssuesTimeout());
                ret.setMessage(_JDT.T.plugins_errors_nointernetconn());
                return ret;
            } else if (throwable instanceof UnknownHostException) {
                DownloadLinkCandidateResult ret = new DownloadLinkCandidateResult(RESULT.CONNECTION_ISSUES, throwable, pluginHost);
                ret.setWaitTime(JsonConfig.create(GeneralSettings.class).getNetworkIssuesTimeout());
                ret.setMessage(_JDT.T.plugins_errors_nointernetconn());
                return ret;
            } else if (throwable instanceof SocketTimeoutException) {
                DownloadLinkCandidateResult ret = new DownloadLinkCandidateResult(RESULT.CONNECTION_ISSUES, throwable, pluginHost);
                ret.setWaitTime(JsonConfig.create(GeneralSettings.class).getNetworkIssuesTimeout());
                ret.setMessage(_JDT.T.plugins_errors_hosteroffline());
                return ret;
            } else if (throwable instanceof SocketException) {
                DownloadLinkCandidateResult ret = new DownloadLinkCandidateResult(RESULT.CONNECTION_ISSUES, throwable, pluginHost);
                ret.setWaitTime(JsonConfig.create(GeneralSettings.class).getNetworkIssuesTimeout());
                ret.setMessage(_JDT.T.plugins_errors_disconnect());
                return ret;
            } else if (throwable instanceof NoGateWayException) {
                DownloadLinkCandidateResult ret = new DownloadLinkCandidateResult(RESULT.CONNECTION_ISSUES, throwable, pluginHost);
                ret.setWaitTime(10 * 1000l);
                ret.setMessage(_JDT.T.plugins_errors_proxy_connection());
                return ret;
            } else if (throwable instanceof ProxyConnectException) {
                DownloadLinkCandidateResult ret = new DownloadLinkCandidateResult(RESULT.CONNECTION_ISSUES, throwable, pluginHost);
                ret.setWaitTime(10 * 1000l);
                ret.setMessage(_JDT.T.plugins_errors_proxy_connection());
                return ret;
            } else if (throwable instanceof ProxyAuthException) {
                DownloadLinkCandidateResult ret = new DownloadLinkCandidateResult(RESULT.CONNECTION_ISSUES, throwable, pluginHost);
                ret.setWaitTime(30 * 1000l);
                ret.setMessage(_JDT.T.plugins_errors_proxy_auth());
                return ret;
            } else if (throwable instanceof IOException) {
                DownloadLinkCandidateResult ret = new DownloadLinkCandidateResult(RESULT.CONNECTION_ISSUES, throwable, pluginHost);
                ret.setWaitTime(JsonConfig.create(GeneralSettings.class).getDownloadUnknownIOExceptionWaittime());
                ret.setMessage(_JDT.T.plugins_errors_hosterproblem());
                return ret;
            } else if (throwable instanceof InterruptedException) {
                if (result.getController().isAborting()) {
                    return new DownloadLinkCandidateResult(RESULT.STOPPED, throwable, pluginHost);
                }
                pluginException = new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _JDT.T.plugins_errors_error() + "Interrupted");
            } else {
                pluginException = new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _JDT.T.plugins_errors_error() + "Throwable");
            }
        }
        if (skipReason != null) {
            switch (skipReason) {
            case NO_ACCOUNT:
                return new DownloadLinkCandidateResult(RESULT.ACCOUNT_REQUIRED, throwable, pluginHost);
            default:
                return new DownloadLinkCandidateResult(skipReason, throwable, pluginHost);
            }
        }
        if (conditionalSkipReason != null) {
            return new DownloadLinkCandidateResult(conditionalSkipReason, throwable, pluginHost);
        }
        if (pluginException instanceof CandidateResultProvider) {
            DownloadLinkCandidateResult ret = ((CandidateResultProvider) pluginException).createCandidateResult(candidate, pluginHost);
            if (ret != null) {
                return ret;
            }
        } else if (pluginException != null) {
            DownloadLinkCandidateResult ret = null;
            String message = null;
            long waitTime = -1;
            switch (pluginException.getLinkStatus()) {
            case LinkStatus.ERROR_RETRY:
                ret = new DownloadLinkCandidateResult(RESULT.RETRY, throwable, pluginHost);
                message = pluginException.getLocalizedMessage();
                break;
            case LinkStatus.ERROR_CAPTCHA:
                ret = new DownloadLinkCandidateResult(RESULT.CAPTCHA, throwable, pluginHost);
                break;
            case LinkStatus.FINISHED:
                ret = new DownloadLinkCandidateResult(RESULT.FINISHED_EXISTS, throwable, pluginHost);
                break;
            case LinkStatus.ERROR_FILE_NOT_FOUND:
                ret = new DownloadLinkCandidateResult(RESULT.OFFLINE_TRUSTED, throwable, pluginHost);
                break;
            case LinkStatus.ERROR_PLUGIN_DEFECT:
                if (latestPlugin != null) {
                    latestPlugin.errLog(throwable, null, link);
                }
                logger.info("Plugin Defect.1");
                ret = new DownloadLinkCandidateResult(RESULT.PLUGIN_DEFECT, throwable, pluginHost);
                break;
            case LinkStatus.ERROR_FATAL:
                ret = new DownloadLinkCandidateResult(RESULT.FATAL_ERROR, throwable, pluginHost);
                message = pluginException.getLocalizedMessage();
                break;
            case LinkStatus.ERROR_IP_BLOCKED:
                ret = new DownloadLinkCandidateResult(RESULT.IP_BLOCKED, throwable, pluginHost);
                message = pluginException.getLocalizedMessage();
                waitTime = 3600000l;
                if (pluginException.getValue() > 0) {
                    waitTime = pluginException.getValue();
                }
                break;
            case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
                ret = new DownloadLinkCandidateResult(RESULT.FILE_UNAVAILABLE, throwable, pluginHost);
                message = pluginException.getLocalizedMessage();
                if (pluginException.getValue() > 0) {
                    waitTime = pluginException.getValue();
                } else if (pluginException.getValue() <= 0) {
                    waitTime = JsonConfig.create(GeneralSettings.class).getDownloadTempUnavailableRetryWaittime();
                }
                break;
            case LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE:
                ret = new DownloadLinkCandidateResult(RESULT.HOSTER_UNAVAILABLE, throwable, pluginHost);
                message = pluginException.getLocalizedMessage();
                if (pluginException.getValue() > 0) {
                    waitTime = pluginException.getValue();
                } else if (pluginException.getValue() <= 0) {
                    waitTime = JsonConfig.create(GeneralSettings.class).getDownloadHostUnavailableRetryWaittime();
                }
                break;
            case LinkStatus.ERROR_PREMIUM:
                if (pluginException.getValue() == PluginException.VALUE_ID_PREMIUM_ONLY) {
                    ret = new DownloadLinkCandidateResult(RESULT.ACCOUNT_REQUIRED, throwable, pluginHost);
                } else if (pluginException.getValue() == PluginException.VALUE_ID_PREMIUM_DISABLE) {
                    ret = new DownloadLinkCandidateResult(RESULT.ACCOUNT_INVALID, throwable, pluginHost);
                } else if (pluginException.getValue() == PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE) {
                    ret = new DownloadLinkCandidateResult(RESULT.ACCOUNT_UNAVAILABLE, throwable, pluginHost);
                } else {
                    ret = new DownloadLinkCandidateResult(RESULT.ACCOUNT_ERROR, throwable, pluginHost);
                }
                break;
            case LinkStatus.ERROR_DOWNLOAD_FAILED:
                if (pluginException.getValue() == LinkStatus.VALUE_LOCAL_IO_ERROR) {
                    ret = new DownloadLinkCandidateResult(SkipReason.INVALID_DESTINATION, throwable, pluginHost);
                } else if (pluginException.getValue() == LinkStatus.VALUE_NETWORK_IO_ERROR) {
                    ret = new DownloadLinkCandidateResult(RESULT.CONNECTION_ISSUES, throwable, pluginHost);
                } else {
                    ret = new DownloadLinkCandidateResult(RESULT.FAILED, throwable, pluginHost);
                }
                break;
            case LinkStatus.ERROR_DOWNLOAD_INCOMPLETE:
                if (pluginException.getValue() == LinkStatus.VALUE_NETWORK_IO_ERROR) {
                    ret = new DownloadLinkCandidateResult(RESULT.CONNECTION_ISSUES, throwable, pluginHost);
                } else {
                    ret = new DownloadLinkCandidateResult(RESULT.FAILED_INCOMPLETE, throwable, pluginHost);
                }
                break;
            case LinkStatus.ERROR_ALREADYEXISTS:
                ret = new DownloadLinkCandidateResult(RESULT.FAILED_EXISTS, throwable, pluginHost);
                break;
            }
            if (ret == null) {
                logger.info("Plugin Defect.2");
                ret = new DownloadLinkCandidateResult(RESULT.PLUGIN_DEFECT, throwable, pluginHost);
            }
            ret.setWaitTime(waitTime);
            ret.setMessage(message);
            return ret;
        }
        if (result.getController().isAborting()) {
            if (result.getController().getLinkStatus().getStatus() == LinkStatus.FINISHED) {
                return new DownloadLinkCandidateResult(RESULT.FINISHED, throwable, pluginHost);
            } else {
                return new DownloadLinkCandidateResult(RESULT.STOPPED, throwable, pluginHost);
            }
        }
        if (result.getController().getLinkStatus().getStatus() == LinkStatus.FINISHED) {
            return new DownloadLinkCandidateResult(RESULT.FINISHED, throwable, pluginHost);
        }
        logger.info("Plugin Defect.3");
        return new DownloadLinkCandidateResult(RESULT.PLUGIN_DEFECT, throwable, pluginHost);
    }

    private Set<DownloadLink> finalizeConditionalSkipReasons(final DownloadSession currentSession, final List<DownloadLink> downloadLinksWithConditionalSkipReasons) {
        return finalizeConditionalSkipReasons(new ConcatIterator<DownloadLink>(currentSession.getForcedLinks().iterator(), currentSession.getActivationRequests().iterator()), downloadLinksWithConditionalSkipReasons);
    }

    private Set<DownloadLink> finalizeConditionalSkipReasons(final Iterable<DownloadLink> links, final List<DownloadLink> downloadLinksWithConditionalSkipReasons) {
        final HashSet<DownloadLink> ret = new HashSet<DownloadLink>();
        final ArrayList<DownloadLink> again = new ArrayList<DownloadLink>();
        for (final DownloadLink link : links) {
            final ConditionalSkipReason conditionalSkipReason = link.getConditionalSkipReason();
            if (conditionalSkipReason != null) {
                if (conditionalSkipReason.isConditionReached()) {
                    link.setConditionalSkipReason(null);
                    conditionalSkipReason.finalize(link);
                    if (link.getFinalLinkState() != null) {
                        ret.add(link);
                    }
                } else {
                    again.add(link);
                }
            }
        }
        final int size = again.size();
        while (size > 0) {
            int finalized = 0;
            for (int index = 0; index < size; index++) {
                final DownloadLink link = again.get(index);
                final ConditionalSkipReason conditionalSkipReason = link.getConditionalSkipReason();
                if (conditionalSkipReason != null && conditionalSkipReason.isConditionReached()) {
                    link.setConditionalSkipReason(null);
                    conditionalSkipReason.finalize(link);
                    finalized++;
                    if (link.getFinalLinkState() != null) {
                        ret.add(link);
                    }
                }
            }
            if (finalized == 0) {
                break;
            }
        }
        if (downloadLinksWithConditionalSkipReasons != null) {
            for (int index = 0; index < size; index++) {
                final DownloadLink link = again.get(index);
                final ConditionalSkipReason conditionalSkipReason = link.getConditionalSkipReason();
                if (conditionalSkipReason != null) {
                    downloadLinksWithConditionalSkipReasons.add(link);
                }
            }
        }
        return ret;
    }

    private synchronized void startDownloadJobExecuter() {
        Thread thread = new Thread() {
            @Override
            public void interrupt() {
                logger.log(new RuntimeException("WatchDog(" + getName() + ") interrupted!"));
                super.interrupt();
            }

            @Override
            public void run() {
                this.setName("WatchDog: jobExecuter");
                try {
                    while (true) {
                        synchronized (WATCHDOGLOCK) {
                            if (!isWatchDogThread()) {
                                return;
                            }
                            /* clear interrupted flag in case someone interrupted a DownloadWatchDogJob */
                            interrupted();
                            if (!hasWaitingJobs()) {
                                WATCHDOGLOCK.wait();
                            }
                            if (!isWatchDogThread()) {
                                return;
                            }
                        }
                        final DownloadWatchDogJob job;
                        synchronized (watchDogJobs) {
                            job = watchDogJobs.poll();
                        }
                        if (job != null) {
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

    private boolean isReconnectPossible(List<WaitingSkipReasonContainer> reconnectRequests) {
        if (CFG_RECONNECT.CFG.isReconnectAllowedToInterruptResumableDownloads()) {
            for (final SingleDownloadController con : getSession().getControllers()) {
                if (!con.getDownloadLink().isResumeable()) {
                    /*
                     * running downloadLink is not resumable
                     */
                    return false;
                }
                for (WaitingSkipReasonContainer reconnectRequest : reconnectRequests) {
                    if (StringUtils.equals(reconnectRequest.getDlHost(), con.getDownloadLink().getHost())) {
                        /*
                         * running downloadLink is from same host as reconnectRequest (avoid loops from free/free registered)
                         */
                        return false;
                    }
                }
            }
            return true;
        } else {
            return getSession().getControllers().size() == 0;
        }
    }

    private boolean isReconnectRequired(DownloadSession currentSession, WaitingSkipReasonContainer reconnectRequest) {
        if (!reconnectRequest.getProxySelector().isReconnectSupported()) {
            return false;
        }
        HashSet<DownloadLink> alreadyChecked = new HashSet<DownloadLink>();
        for (DownloadLink link : currentSession.getForcedLinks()) {
            if (alreadyChecked.add(link) == false || canRemove(link, true)) {
                continue;
            }
            ConditionalSkipReason conditionalSkipReason = link.getConditionalSkipReason();
            if (conditionalSkipReason != null) {
                if (conditionalSkipReason instanceof WaitWhileWaitingSkipReasonIsSet) {
                    conditionalSkipReason = ((WaitWhileWaitingSkipReasonIsSet) conditionalSkipReason).getConditionalSkipReason();
                }
                if (conditionalSkipReason instanceof WaitingSkipReason) {
                    if (StringUtils.equals(reconnectRequest.getDlHost(), link.getHost())) {
                        return true;
                    }
                }
            }
        }
        if (currentSession.isForcedOnlyModeEnabled() == false) {
            for (DownloadLink link : currentSession.getActivationRequests()) {
                if (alreadyChecked.add(link) == false || canRemove(link, false)) {
                    continue;
                }
                ConditionalSkipReason conditionalSkipReason = link.getConditionalSkipReason();
                if (conditionalSkipReason != null) {
                    if (conditionalSkipReason instanceof WaitWhileWaitingSkipReasonIsSet) {
                        conditionalSkipReason = ((WaitWhileWaitingSkipReasonIsSet) conditionalSkipReason).getConditionalSkipReason();
                    }
                    if (conditionalSkipReason instanceof WaitingSkipReason) {
                        if (StringUtils.equals(reconnectRequest.getDlHost(), link.getHost())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void validateProxyInfoHistory() {
        enqueueJob(new DownloadWatchDogJob() {
            @Override
            public void execute(DownloadSession currentSession) {
                if (isAutoReconnectEnabled()) {
                    ProxyInfoHistory proxyInfoHistory = currentSession.getProxyInfoHistory();
                    proxyInfoHistory.validate();
                    List<WaitingSkipReasonContainer> reconnectRequests = proxyInfoHistory.list(WaitingSkipReason.CAUSE.IP_BLOCKED, null);
                    if (reconnectRequests != null) {
                        for (WaitingSkipReasonContainer reconnectRequest : reconnectRequests) {
                            if (isReconnectRequired(currentSession, reconnectRequest)) {
                                currentSession.compareAndSetSessionState(DownloadSession.SessionState.NORMAL, DownloadSession.SessionState.RECONNECT_REQUESTED);
                                if (currentSession.getSessionState() == DownloadSession.SessionState.RECONNECT_REQUESTED && isReconnectPossible(reconnectRequests)) {
                                    currentSession.setSessionState(DownloadSession.SessionState.RECONNECT_RUNNING);
                                    invokeReconnect();
                                }
                                return;
                            }
                        }
                    }
                }
                currentSession.compareAndSetSessionState(DownloadSession.SessionState.RECONNECT_REQUESTED, DownloadSession.SessionState.NORMAL);
            }

            @Override
            public void interrupt() {
            }

            @Override
            public boolean isHighPriority() {
                return false;
            }
        });
    }

    private ReconnectThread invokeReconnect() {
        while (true) {
            Thread ret = reconnectThread.get();
            if (ret != null) {
                return (ReconnectThread) ret;
            }
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

                    @Override
                    public boolean isHighPriority() {
                        return false;
                    }
                };
                enqueueJob(job);
            }
        }
    }

    public Reconnecter.ReconnectResult requestReconnect(boolean waitForResult) throws InterruptedException {
        ReconnectThread thread = invokeReconnect();
        if (waitForResult) {
            return thread.waitForResult();
        }
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

            @Override
            public boolean isHighPriority() {
                return true;
            }
        });
    }

    protected boolean hasWaitingJobs() {
        synchronized (watchDogJobs) {
            return watchDogJobs.peek() != null;
        }
    }

    private synchronized void startDownloadWatchDog() {
        stateMachine.setStatus(RUNNING_STATE);
        Thread thread = new Thread() {
            @Override
            public void interrupt() {
                logger.log(new RuntimeException("WatchDog(" + getName() + ") interrupted!"));
                super.interrupt();
            }

            protected final void processJobs() {
                try {
                    setTempWatchDogJobThread(Thread.currentThread());
                    DownloadWatchDogJob peekLast = null;
                    while (true) {
                        final DownloadWatchDogJob job;
                        synchronized (watchDogJobs) {
                            if (peekLast == null) {
                                peekLast = watchDogJobs.peekLast();
                            }
                            job = watchDogJobs.poll();
                        }
                        if (job != null) {
                            try {
                                currentWatchDogJob.set(job);
                                job.execute(getSession());
                            } catch (final Throwable e) {
                                logger.log(e);
                            } finally {
                                currentWatchDogJob.set(null);
                            }
                            if (job == peekLast) {
                                // avoid loops for enqueueJob->enqueueJob->enqueueJob...
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                } finally {
                    setTempWatchDogJobThread(null);
                }
            }

            protected final int maxWaitTimeout = 5000;

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

                        @Override
                        public boolean isHighPriority() {
                            return false;
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
                boolean jobExecuterStarted = false;
                DownloadControllerListener listener = null;
                AccountControllerListener accListener = null;
                HosterRuleControllerListener hrcListener = null;
                DefaultEventListener<ProxyEvent<AbstractProxySelectorImpl>> proxyListener = null;
                final AtomicLong lastStructureChange = new AtomicLong(-1l);
                final AtomicLong lastActivatorRequestRebuild = new AtomicLong(-1l);
                long waitedForNewActivationRequests = 0;
                @SuppressWarnings("unused")
                final FileBytesCache hardReferenceFileBytesCache = DownloadSession.getDownloadWriteCache();
                try {
                    unSkipAllSkipped();
                    ProxyController.getInstance().getEventSender().addListener(proxyListener = new DefaultEventListener<ProxyEvent<AbstractProxySelectorImpl>>() {
                        final DelayedRunnable delayer = new DelayedRunnable(1000, 5000) {
                            @Override
                            public void delayedrun() {
                                enqueueJob(new DownloadWatchDogJob() {
                                    @Override
                                    public void interrupt() {
                                    }

                                    @Override
                                    public void execute(DownloadSession currentSession) {
                                        /* reset CONNECTION_UNAVAILABLE */
                                        final List<DownloadLink> unSkip = DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {
                                            @Override
                                            public int returnMaxResults() {
                                                return 0;
                                            }

                                            @Override
                                            public boolean acceptNode(DownloadLink node) {
                                                return SkipReason.CONNECTION_UNAVAILABLE.equals(node.getSkipReason());
                                            }
                                        });
                                        unSkip(unSkip);
                                    }

                                    @Override
                                    public boolean isHighPriority() {
                                        return false;
                                    }
                                });
                            }
                        };

                        @Override
                        public void onEvent(ProxyEvent<AbstractProxySelectorImpl> event) {
                            delayer.delayedrun();
                        }
                    }, true);
                    HosterRuleController.getInstance().getEventSender().addListener(hrcListener = new HosterRuleControllerListener() {
                        private void removeAccountCache(final String host) {
                            if (host != null) {
                                enqueueJob(new DownloadWatchDogJob() {
                                    @Override
                                    public void execute(DownloadSession currentSession) {
                                        currentSession.removeAccountCache(host);
                                    }

                                    @Override
                                    public void interrupt() {
                                    }

                                    @Override
                                    public boolean isHighPriority() {
                                        return false;
                                    }
                                });
                            }
                        }

                        @Override
                        public void onRuleAdded(AccountUsageRule parameter) {
                            if (parameter != null) {
                                removeAccountCache(parameter.getHoster());
                            }
                        }

                        @Override
                        public void onRuleDataUpdate(AccountUsageRule parameter) {
                            if (parameter != null) {
                                removeAccountCache(parameter.getHoster());
                            }
                        }

                        @Override
                        public void onRuleRemoved(final AccountUsageRule parameter) {
                            if (parameter != null) {
                                removeAccountCache(parameter.getHoster());
                            }
                        }

                        @Override
                        public void onRuleStructureUpdate() {
                            // nothing internal changed. just order update
                        }
                    }, true);
                    AccountController.getInstance().getEventSender().addListener(accListener = new AccountControllerListener() {
                        @Override
                        public void onAccountControllerEvent(final AccountControllerEvent event) {
                            enqueueJob(new DownloadWatchDogJob() {
                                @Override
                                public void execute(DownloadSession currentSession) {
                                    currentSession.removeAccountCache(event.getAccount().getHoster());
                                    final AccountInfo ai = event.getAccount().getAccountInfo();
                                    if (ai != null) {
                                        final List<String> multiHostList = ai.getMultiHostSupport();
                                        if (multiHostList != null) {
                                            for (final String multiHost : multiHostList) {
                                                currentSession.removeAccountCache(multiHost);
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void interrupt() {
                                }

                                @Override
                                public boolean isHighPriority() {
                                    return false;
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

                                @Override
                                public boolean isHighPriority() {
                                    return false;
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
                                        removeLink(item, currentSession);
                                    }
                                }

                                @Override
                                public void interrupt() {
                                }

                                @Override
                                public boolean isHighPriority() {
                                    return false;
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

                                        @Override
                                        public boolean isHighPriority() {
                                            return false;
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

                                        @Override
                                        public boolean isHighPriority() {
                                            return false;
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

                                        @Override
                                        public boolean isHighPriority() {
                                            return false;
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

                                        @Override
                                        public boolean isHighPriority() {
                                            return false;
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
                            final ArrayList<DownloadLink> downloadLinksWithConditionalSkipReasons = new ArrayList<DownloadLink>();
                            final Set<DownloadLink> finalLinkStateLinks = finalizeConditionalSkipReasons(getSession(), downloadLinksWithConditionalSkipReasons);
                            handleFinalLinkStates(finalLinkStateLinks, getSession(), logger, null);
                            if (newDLStartAllowed(getSession())) {
                                DownloadWatchDog.this.activateDownloads(downloadLinksWithConditionalSkipReasons);
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
                            synchronized (WATCHDOGLOCK) {
                                while (!hasWaitingJobs() && ++round < 4 && DownloadWatchDog.this.stateMachine.isState(DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.PAUSE_STATE)) {
                                    long currentTimeStamp = System.currentTimeMillis();
                                    WATCHDOGLOCK.wait(1250);
                                    waitedForNewActivationRequests += System.currentTimeMillis() - currentTimeStamp;
                                    if ((getSession().isActivationRequestsWaiting() == false && DownloadWatchDog.this.getActiveDownloads() == 0)) {
                                        /*
                                         * it's important that this if statement gets checked after wait!, else we will loop through without
                                         * waiting for new links/user interaction
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

                        @Override
                        public boolean isHighPriority() {
                            return true;
                        }
                    });
                    DownloadController.getInstance().removeListener(listener);
                    AccountController.getInstance().getEventSender().removeListener(accListener);
                    HosterRuleController.getInstance().getEventSender().removeListener(hrcListener);
                    ProxyController.getInstance().getEventSender().removeListener(proxyListener);
                    logger.info("DownloadWatchDog: stopping");
                    synchronized (DownloadWatchDog.this) {
                        startDownloadJobExecuter();
                        jobExecuterStarted = !isWatchDogThread();
                    }
                    /* stop all remaining downloads */
                    abortAllSingleDownloadControllers();
                    /* finalize links */
                    final Set<DownloadLink> finalLinkStateLinks = finalizeConditionalSkipReasons(getSession(), null);
                    handleFinalLinkStates(finalLinkStateLinks, getSession(), logger, null);
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

                        @Override
                        public boolean isHighPriority() {
                            return true;
                        }
                    });
                } finally {
                    if (jobExecuterStarted == false) {
                        synchronized (DownloadWatchDog.this) {
                            startDownloadJobExecuter();
                        }
                    }
                    /* full stop reached */
                    logger.info("DownloadWatchDog: stopped");
                    /* clear session */
                    final DownloadSession latestSession = getSession();
                    enqueueJob(new DownloadWatchDogJob() {
                        @Override
                        public void execute(DownloadSession currentSession) {
                            try {
                                latestSession.removeHistory((DownloadLink) null);
                                latestSession.removeAccountCache(null);
                                latestSession.clearPluginCache();
                                latestSession.getActivationRequests().clear();
                                latestSession.getForcedLinks().clear();
                                latestSession.setOnFileExistsAction(null, null);
                            } finally {
                                stateMachine.setStatus(STOPPED_STATE);
                            }
                        }

                        @Override
                        public void interrupt() {
                        }

                        @Override
                        public boolean isHighPriority() {
                            return true;
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
        final DownloadWatchDogJob job = getCurrentDownloadWatchDogJob();
        if (job != null) {
            job.interrupt();
        }
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

            @Override
            public boolean isHighPriority() {
                return true;
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

            @Override
            public boolean isHighPriority() {
                return true;
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

    public void onNewFile(Object obj, final File[] list) {
        if (JsonConfig.create(GeneralSettings.class).isAutoOpenContainerAfterDownload() && list != null && list.length > 0) {
            /* check if extracted files are container files */
            final ArrayList<String> files = new ArrayList<String>();
            for (final File file : list) {
                files.add(file.toURI().toString());
            }
            final String source;
            if (obj instanceof SingleDownloadController) {
                String url = ((SingleDownloadController) obj).getDownloadLink().getContentUrl();
                if (url == null) {
                    url = ((SingleDownloadController) obj).getDownloadLink().getContentUrl();
                }
                source = url;
            } else {
                source = null;
            }
            final HashSet<String> handled = new HashSet<String>();
            for (final PluginsC pCon : ContainerPluginController.getInstance().list()) {
                for (String file : files) {
                    if (pCon.canHandle(file) && handled.add(file)) {
                        TaskQueue.getQueue().addAsynch(new QueueAction<Void, RuntimeException>() {
                            @Override
                            protected Void run() throws RuntimeException {
                                StringBuilder sb = new StringBuilder();
                                for (final String file : files) {
                                    if (sb.length() > 0) {
                                        sb.append("\r\n");
                                    }
                                    sb.append(file);
                                }
                                LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(LinkOriginDetails.getInstance(LinkOrigin.DOWNLOADED_CONTAINER, source), sb.toString()));
                                return null;
                            }

                            @Override
                            protected boolean allowAsync() {
                                return true;
                            }
                        });
                    }
                }
            }
        }
    }

    private final static HashSet<String> accessChecks = new HashSet<String>();

    public void localFileCheck(final SingleDownloadController controller, final ExceptionRunnable runOkay, final ExceptionRunnable runFailed) throws Exception {
        final NullsafeAtomicReference<Object> asyncResult = new NullsafeAtomicReference<Object>(null);
        enqueueJob(new DownloadWatchDogJob() {
            private void check(DownloadSession session, final SingleDownloadController controller) throws Exception {
                if (controller.isAborting()) {
                    throw new InterruptedException("Controller is aborted");
                }
                final DownloadLink downloadLink = controller.getDownloadLink();
                final File fileOutput = controller.getFileOutput(false, true);
                if (fileOutput.isDirectory()) {
                    controller.getLogger().severe("fileOutput is a directory " + fileOutput);
                    throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
                }
                boolean fileExists = fileOutput.exists();
                if (!fileExists) {
                    try {
                        validateDestination(fileOutput);
                    } catch (PathTooLongException e) {
                        controller.getLogger().severe("not allowed to create path " + e.getFile());
                        throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
                    } catch (BadDestinationException e) {
                        controller.getLogger().severe("not allowed to create path " + e.getFile());
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
                    if (!accessChecks.contains(fileOutput.getParentFile().getAbsolutePath())) {
                        final File writeTest = new File(fileOutput.getParentFile(), "jd_accessCheck_" + new UniqueAlltimeID().getID());
                        try {
                            if (writeTest.exists() == false) {
                                try {
                                    final RandomAccessFile raf = IO.open(writeTest, "rw");
                                    raf.close();
                                } catch (IOException e) {
                                    throw new SkipReasonException(SkipReason.INVALID_DESTINATION, e);
                                }
                            } else {
                                if (!writeTest.canWrite()) {
                                    throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
                                }
                            }
                            accessChecks.add(fileOutput.getParentFile().getAbsolutePath());
                        } catch (Exception e) {
                            LogSource.exception(controller.getLogger(), e);
                            if (e instanceof SkipReasonException) {
                                throw (SkipReasonException) e;
                            }
                            throw new SkipReasonException(SkipReason.INVALID_DESTINATION, e);
                        } finally {
                            if (!writeTest.delete()) {
                                controller.getJobsAfterDetach().add(new DownloadWatchDogJob() {
                                    @Override
                                    public void interrupt() {
                                    }

                                    @Override
                                    public void execute(DownloadSession currentSession) {
                                        writeTest.delete();
                                    }

                                    @Override
                                    public boolean isHighPriority() {
                                        return false;
                                    }
                                });
                            }
                        }
                    }
                }
                final boolean insideDownloadInstance = controller.getDownloadInstance() != null;
                if (!insideDownloadInstance) {
                    /* we are outside DownloadInterface */
                    File localCheck2 = controller.getFileOutput(true, true);
                    if (localCheck2 == null) {
                        /*
                         * dont proceed when we do not have a finalFilename yet
                         */
                        return;
                    }
                }
                boolean fileInProgress = false;
                if (!fileExists) {
                    final MirrorDetectionDecision mirrorDetectionDecision = config.getMirrorDetectionDecision();
                    for (SingleDownloadController downloadController : session.getControllers()) {
                        if (downloadController == controller) {
                            continue;
                        }
                        DownloadLink block = downloadController.getDownloadLink();
                        if (block == downloadLink) {
                            continue;
                        }
                        final String localCheck = fileOutput.getAbsolutePath();
                        if (session.getFileAccessManager().isLockedBy(fileOutput, downloadController)) {
                            /* fileOutput is already locked */
                            if (block.getFilePackage() == downloadLink.getFilePackage() && isMirrorCandidate(downloadLink, localCheck, block, mirrorDetectionDecision)) {
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
                            if (io.getCloseReason() == CloseReason.TIMEOUT) {
                                throw new SkipReasonException(SkipReason.FILE_EXISTS);
                            }
                            if (io.getCloseReason() == CloseReason.INTERRUPT) {
                                throw new InterruptedException("IFFileExistsDialog Interrupted");
                            }
                            if (io.getCloseReason() != CloseReason.OK) {
                                doAction = IfFileExistsAction.SKIP_FILE;
                            } else {
                                doAction = io.getAction();
                            }
                            if (doAction == null) {
                                doAction = IfFileExistsAction.SKIP_FILE;
                            }
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
                        case SET_FILE_TO_SUCCESSFUL_MIRROR:
                            if (!fileInProgress) {
                                throw new DeferredRunnableException(new ExceptionRunnable() {
                                    @Override
                                    public void run() throws Exception {
                                        final PluginForHost plugin = controller.getProcessingPlugin();
                                        if (plugin != null) {
                                            final Downloadable downloadable = plugin.newDownloadable(downloadLink, null);
                                            if (downloadable != null) {
                                                switch (config.getMirrorDetectionDecision()) {
                                                case AUTO:
                                                    final HashInfo hashInfo = downloadable.getHashInfo();
                                                    if (hashInfo != null) {
                                                        final HashResult hashResult = downloadable.getHashResult(hashInfo, fileOutput);
                                                        if (hashResult != null && hashResult.match()) {
                                                            downloadable.setHashResult(hashResult);
                                                            downloadLink.setDownloadCurrent(fileOutput.length());
                                                            throw new PluginException(LinkStatus.FINISHED);
                                                        }
                                                    }
                                                case FILENAME_FILESIZE:
                                                    final long fileSize = downloadable.getVerifiedFileSize();
                                                    if (fileSize >= 0 && fileSize == fileOutput.length()) {
                                                        downloadLink.setDownloadCurrent(fileOutput.length());
                                                        throw new PluginException(LinkStatus.FINISHED);
                                                    }
                                                    break;
                                                case FILENAME:
                                                    // nothing
                                                }
                                            }
                                        }
                                        throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
                                    }
                                });
                            } else {
                                throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
                            }
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
                        try {
                            File check = null;
                            String newName = null;
                            while (true) {
                                newName = name + "_" + (duplicateFilenameCounter++) + extension;
                                check = new File(downloadPath, newName);
                                if (check.exists()) {
                                    check = null;
                                } else {
                                    for (SingleDownloadController downloadController : session.getControllers()) {
                                        if (downloadController == controller) {
                                            continue;
                                        }
                                        if (session.getFileAccessManager().isLockedBy(check, downloadController)) {
                                            check = null;
                                            break;
                                        }
                                    }
                                }
                                if (check != null) {
                                    break;
                                }
                            }
                            // we can do this, because the localFilecheck always runs BEFORE the download
                            // except for org.jdownloader.controlling.linkcrawler.GenericVariants.DEMUX_GENERIC_AUDIO
                            controller.setSessionDownloadFilename(newName);
                            downloadLink.setForcedFileName(newName);
                            downloadLink.setChunksProgress(null);
                        } catch (Throwable e) {
                            LogSource.exception(controller.getLogger(), e);
                            downloadLink.setForcedFileName(null);
                            throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
                        }
                        break;
                    default:
                        throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
                    }
                }
                return;
            }

            @Override
            public void execute(DownloadSession currentSession) {
                try {
                    check(currentSession, controller);
                    if (runOkay != null) {
                        runOkay.run();
                    }
                    synchronized (asyncResult) {
                        asyncResult.set(asyncResult);
                        asyncResult.notifyAll();
                    }
                } catch (final Exception e) {
                    try {
                        if (runFailed != null) {
                            runFailed.run();
                        }
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
                if (getCurrentDownloadWatchDogJob() == this && watchDogThread != null) {
                    watchDogThread.interrupt();
                }
            }

            @Override
            public boolean isHighPriority() {
                return false;
            }
        });
        Object ret = null;
        while (true) {
            synchronized (asyncResult) {
                ret = asyncResult.get();
                if (ret != null) {
                    break;
                }
                asyncResult.wait();
            }
        }
        if (asyncResult == ret) {
            return;
        }
        if (ret instanceof PluginException) {
            throw (PluginException) ret;
        }
        if (ret instanceof Exception) {
            throw (Exception) ret;
        }
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
                    if (con.isAlive() == false) {
                        continue;
                    }
                    dialogTitle = _JDT.T.DownloadWatchDog_onShutdownRequest_();
                    DownloadInterface dl = con.getDownloadInstance();
                    if (dl != null && !con.getDownloadLink().isResumeable()) {
                        dialogTitle = _JDT.T.DownloadWatchDog_onShutdownRequest_nonresumable();
                        break;
                    }
                }
            }
            if (dialogTitle != null) {
                if (request.isSilent() == false) {
                    if (JDGui.bugme(WarnLevel.NORMAL)) {
                        if (UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, dialogTitle, _JDT.T.DownloadWatchDog_onShutdownRequest_msg(), new AbstractIcon(IconKey.ICON_DOWNLOAD, 32), _JDT.T.literally_yes(), null)) {
                            return;
                        }
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
        config.setClosedWithRunningDownloads(isRunning());
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
        final DownloadSession session = getSession();
        if (session.isStopMarkSet() == false || session.getStopMark() == STOPMARK.HIDDEN) {
            return;
        }
        enqueueJob(new DownloadWatchDogJob() {
            @Override
            public void execute(DownloadSession currentSession) {
                final Object stopMark = currentSession.getStopMark();
                if (currentSession.isStopMarkSet() == false || stopMark == STOPMARK.HIDDEN) {
                    return;
                }
                if (stopMark == pkg) {
                    /* now the stopmark is hidden */
                    currentSession.setStopMark(STOPMARK.HIDDEN);
                }
            }

            @Override
            public void interrupt() {
            }

            @Override
            public boolean isHighPriority() {
                return false;
            }
        });
    }

    @Override
    public void onDownloadControllerRemovedLinklist(final List<DownloadLink> list) {
        final DownloadSession session = getSession();
        if (session.isStopMarkSet() == false || session.getStopMark() == STOPMARK.HIDDEN) {
            return;
        }
        enqueueJob(new DownloadWatchDogJob() {
            @Override
            public void execute(DownloadSession currentSession) {
                final Object stopMark = currentSession.getStopMark();
                if (currentSession.isStopMarkSet() == false || stopMark == STOPMARK.HIDDEN) {
                    return;
                }
                for (final DownloadLink l : list) {
                    if (stopMark == l) {
                        currentSession.setStopMark(STOPMARK.HIDDEN);
                        return;
                    }
                }
            }

            @Override
            public void interrupt() {
            }

            @Override
            public boolean isHighPriority() {
                return false;
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
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink) {
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg) {
    }

    public void renameLink(final DownloadLink downloadLink, final String value) {
        if (!StringUtils.equals(downloadLink.getForcedFileName(), value)) {
            // logger.log(new Exception("Rename"));
            logger.info("Requested Rename of " + downloadLink + " to " + value);
            enqueueJob(new DownloadWatchDogJob() {
                @Override
                public void execute(DownloadSession currentSession) {
                    if (!StringUtils.equals(downloadLink.getForcedFileName(), value)) {
                        if (downloadLink.getDownloadLinkController() != null) {
                            logger.info("Requested Rename of " + downloadLink + " to " + value + " DELAYED");
                            downloadLink.setForcedFileName(value);
                        } else if (downloadLink.getDefaultPlugin() != null) {
                            logger.info("Requested Rename of " + downloadLink + " to " + value + " NOW");
                            move(downloadLink, downloadLink.getParentNode().getDownloadDirectory(), downloadLink.getName(), downloadLink.getParentNode().getDownloadDirectory(), value);
                            downloadLink.setForcedFileName(value);
                        }
                    }
                }

                @Override
                public void interrupt() {
                }

                @Override
                public boolean isHighPriority() {
                    return false;
                }
            });
        }
    }

    public void setStopMark(final Object stopEntry) {
        enqueueJob(new DownloadWatchDogJob() {
            @Override
            public void interrupt() {
            }

            @Override
            public void execute(DownloadSession currentSession) {
                currentSession.setStopMark(stopEntry);
            }

            @Override
            public boolean isHighPriority() {
                return true;
            }
        });
    }

    public void handleMovedDownloadLinks(final FilePackage dest, final FilePackage source, final List<DownloadLink> links) {
        if (source != dest) {
            enqueueJob(new DownloadWatchDogJob() {
                //
                @Override
                public void execute(DownloadSession currentSession) {
                    for (DownloadLink downloadLink : links) {
                        if (downloadLink.getDownloadLinkController() != null) {
                            // running
                            // TODO
                            // if (DISKSPACERESERVATIONRESULT.FAILED.equals(validateDiskFree(nextSelectedCandidates))) {
                            // for (final DownloadLinkCandidate candidate : nextSelectedCandidates) {
                            // selector.addExcluded(candidate, new DownloadLinkCandidateResult(SkipReason.DISK_FULL, null, null));
                            // }
                            // }
                        } else if (downloadLink.getDefaultPlugin() != null) {
                            move(downloadLink, source.getDownloadDirectory(), downloadLink.getName(), dest.getDownloadDirectory(), null);
                        }
                    }
                }

                @Override
                public void interrupt() {
                }

                @Override
                public boolean isHighPriority() {
                    return false;
                }
            });
        }
    }

    public void setDownloadDirectory(final FilePackage pkg, final String path) {
        if (!new File(pkg.getDownloadDirectory()).equals(new File(path))) {
            enqueueJob(new DownloadWatchDogJob() {
                @Override
                public void execute(DownloadSession currentSession) {
                    final String old = pkg.getDownloadDirectory();
                    if (!new File(pkg.getDownloadDirectory()).equals(new File(path))) {
                        pkg.setDownloadDirectory(path);
                        boolean readL = pkg.getModifyLock().readLock();
                        try {
                            for (DownloadLink downloadLink : pkg.getChildren()) {
                                if (downloadLink.getDownloadLinkController() != null) {
                                    // running
                                } else if (downloadLink.getDefaultPlugin() != null) {
                                    move(downloadLink, old, downloadLink.getName(), path, null);
                                }
                            }
                        } finally {
                            pkg.getModifyLock().readUnlock(readL);
                        }
                    }
                }

                @Override
                public void interrupt() {
                }

                @Override
                public boolean isHighPriority() {
                    return false;
                }
            });
        }
    }

    protected void move(DownloadLink downloadLink, String oldDir, String oldName, String newDir, String newName) {
        try {
            ArrayList<DownloadLinkCandidate> lst = new ArrayList<DownloadLinkCandidate>();
            lst.add(new DownloadLinkCandidate(downloadLink, true));
            if (DISKSPACERESERVATIONRESULT.FAILED.equals(validateDiskFree(lst))) {
                throw new IOException(_GUI.T.DownloadWatchDog_move_exception_disk_full(downloadLink.getFileOutput()));
            }
            logger.info("Move " + downloadLink);
            logger.info("From " + oldDir + "/" + oldName + " to " + newDir + "/" + newName);
            if (!new File(oldDir).equals(new File(newDir)) && !CFG_GENERAL.CFG.isMoveFilesIfDownloadDestinationChangesEnabled()) {
                logger.info("Cancel isMoveFilesIfDownloadDestinationChanges is false");
                return;
            }
            if (new File(oldDir).equals(new File(newDir)) && !oldName.equals(newName) && !CFG_GENERAL.CFG.isRenameFilesIfDownloadLinkNameChangesEnabled()) {
                logger.info("Cancel isRenameFilesIfDownloadLinkNameChangesEnabled is false");
            }
            downloadLink.getDefaultPlugin().move(downloadLink, oldDir, oldName, newDir, newName);
            return;
        } catch (Throwable e) {
            logger.log(e);
            UIOManager.I().show(ExceptionDialogInterface.class, new ExceptionDialog(UIOManager.BUTTONS_HIDE_CANCEL, _GUI.T.lit_error_occured(), e.getMessage(), e, _GUI.T.lit_close(), null));
        }
    }

    @Override
    public void onNewFolder(Object caller, File folder) {
    }
}