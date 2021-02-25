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
import java.net.NoRouteToHostException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.downloadcontroller.DiskSpaceManager.DISKSPACERESERVATIONRESULT;
import jd.controlling.downloadcontroller.event.DownloadWatchdogEvent;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.controlling.proxy.SelectProxyByURLHook;
import jd.controlling.reconnect.ipcheck.BalancedWebIPCheck;
import jd.controlling.reconnect.ipcheck.IPCheckException;
import jd.controlling.reconnect.ipcheck.OfflineException;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.BrowserSettingsThread;
import jd.http.NoGateWayException;
import jd.http.ProxySelectorInterface;
import jd.http.StaticProxySelector;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageProperty;
import jd.plugins.FilePackageView;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.PluginProgress;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.HashResult;

import org.appwork.utils.Exceptions;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.StringUtils;
import org.appwork.utils.UniqueAlltimeID;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.NetworkInterfaceException;
import org.jdownloader.controlling.download.DownloadControllerListener;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.tasks.PluginProgressTask;
import org.jdownloader.plugins.tasks.PluginSubTask;

public class SingleDownloadController extends BrowserSettingsThread implements DownloadControllerListener {
    /**
     * signals that abort request has been received
     */
    private final AtomicBoolean                          abortFlag        = new AtomicBoolean(false);
    /**
     * signals the activity of the plugin in use
     */
    private final NullsafeAtomicReference<PluginForHost> processingPlugin = new NullsafeAtomicReference<PluginForHost>(null);

    public static class WaitingQueueItem {
        public final AtomicLong                          lastStartTimestamp      = new AtomicLong(0);
        public final AtomicLong                          lastConnectionTimestamp = new AtomicLong(System.currentTimeMillis());
        private final CopyOnWriteArrayList<DownloadLink> queueLinks              = new CopyOnWriteArrayList<DownloadLink>();

        public int indexOf(DownloadLink link) {
            return queueLinks.indexOf(link);
        }
    }

    private static final HashMap<String, WaitingQueueItem>  LAST_DOWNLOAD_START_TIMESTAMPS = new HashMap<String, WaitingQueueItem>();
    private final DownloadLink                              downloadLink;
    private final Account                                   account;
    private volatile long                                   startTimestamp                 = -1;
    private final DownloadLinkCandidate                     candidate;
    private final DownloadWatchDog                          watchDog;
    private final LinkStatus                                linkStatus;
    private volatile HashResult                             hashResult                     = null;
    private final CopyOnWriteArrayList<DownloadWatchDogJob> jobsAfterDetach                = new CopyOnWriteArrayList<DownloadWatchDogJob>();
    private final WaitingQueueItem                          queueItem;
    private final long                                      sizeBefore;
    private final ArrayList<PluginSubTask>                  tasks                          = new ArrayList<PluginSubTask>();
    private volatile HTTPProxy                              usedProxy;
    private volatile boolean                                resumed;
    private final DownloadSession                           session;
    private final AtomicBoolean                             finished                       = new AtomicBoolean(false);

    public WaitingQueueItem getQueueItem() {
        return queueItem;
    }

    public CopyOnWriteArrayList<DownloadWatchDogJob> getJobsAfterDetach() {
        return jobsAfterDetach;
    }

    @Override
    public synchronized void start() {
        queueItem.queueLinks.add(downloadLink);
        super.start();
    }

    public HashResult getHashResult() {
        return hashResult;
    }

    public void setHashResult(HashResult hashResult) {
        this.hashResult = hashResult;
    }

    public LinkStatus getLinkStatus() {
        return linkStatus;
    }

    public DownloadInterface getDownloadInstance() {
        PluginForHost plugin = processingPlugin.get();
        if (plugin != null) {
            return plugin.getDownloadInterface();
        }
        return null;
    }

    public DownloadLinkCandidate getDownloadLinkCandidate() {
        return candidate;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void lockFile(File file) throws FileIsLockedException {
        DownloadWatchDog.getInstance().getSession().getFileAccessManager().lock(file, this);
    }

    public boolean unlockFile(File file) {
        return DownloadWatchDog.getInstance().getSession().getFileAccessManager().unlock(file, this);
    }

    public DownloadSpeedManager getConnectionHandler() {
        return watchDog.getDownloadSpeedManager();
    }

    protected SingleDownloadController(DownloadLinkCandidate candidate, DownloadWatchDog watchDog) {
        super("Download: " + candidate.getLink().getView().getDisplayName() + "_" + candidate.getLink().getHost());
        setPriority(Thread.MIN_PRIORITY);
        this.watchDog = watchDog;
        this.candidate = candidate;
        super.setProxySelector(candidate.getProxySelector());
        this.downloadLink = candidate.getLink();
        this.sizeBefore = Math.max(0, downloadLink.getView().getBytesLoaded());
        this.account = candidate.getCachedAccount().getAccount();
        String host = candidate.getCachedAccount().getPlugin().getHost();
        WaitingQueueItem queueItem = LAST_DOWNLOAD_START_TIMESTAMPS.get(host);
        if (queueItem == null) {
            queueItem = new WaitingQueueItem();
            LAST_DOWNLOAD_START_TIMESTAMPS.put(host, queueItem);
        }
        this.queueItem = queueItem;
        linkStatus = new LinkStatus(downloadLink);
        session = watchDog.getSession();
        setName("Download: " + downloadLink.getView().getDisplayName() + "_" + downloadLink.getHost());
    }

    @Override
    public AbstractProxySelectorImpl getProxySelector() {
        return candidate.getProxySelector();
    }

    public boolean isDebug() {
        return true;
    }

    @Override
    public boolean isVerbose() {
        return true;
    }

    public Account getAccount() {
        return account;
    }

    public boolean isAborting() {
        return abortFlag.get();
    }

    public boolean isActive() {
        return processingPlugin.isValueSet();
    }

    private final Set<String> stackTraces = new HashSet<String>();

    @Override
    public void interrupt() {
        super.interrupt();
        final LogInterface logger = getLogger();
        if (logger != null) {
            final Exception exception = new Exception("SingleDownloadController.interrupt");
            final String stackTrace = Exceptions.getStackTrace(exception);
            synchronized (stackTraces) {
                if (stackTraces.add(stackTrace)) {
                    logger.log(exception);
                }
            }
        }
    }

    protected void abort() {
        if (!isActive()) {
            /* this singleDownloadController is no longer active */
            return;
        }
        if (abortFlag.compareAndSet(false, true)) {
            /* this is our initial abort request */
            final Thread abortThread = new Thread() {
                @Override
                public void run() {
                    while (isActive() && SingleDownloadController.this.isAlive()) {
                        try {
                            final DownloadInterface dli = getDownloadInstance();
                            if (dli != null) {
                                dli.stopDownload();
                            }
                        } catch (final Throwable e) {
                            LogSource.exception(logger, e);
                        }
                        synchronized (processingPlugin) {
                            if (!isActive() || SingleDownloadController.this.isAlive() == false) {
                                return;
                            }
                            // this is importent to interrupt dialogs, captcha windows and other processes. we have to ensure that all
                            // download steps support proper interruptions
                            SingleDownloadController.this.interrupt();
                            try {
                                processingPlugin.wait(1000);
                            } catch (final InterruptedException e) {
                            }
                        }
                    }
                }
            };
            abortThread.setDaemon(true);
            abortThread.setName("Abort: " + downloadLink.getView().getDisplayName() + "_" + downloadLink.getUniqueID());
            abortThread.start();
        }
        final LogInterface logger = getLogger();
        if (logger != null) {
            final Exception exception = new Exception("SingleDownloadController.abort");
            final String stackTrace = Exceptions.getStackTrace(exception);
            synchronized (stackTraces) {
                if (stackTraces.add(stackTrace)) {
                    logger.log(exception);
                }
            }
        }
    }

    public DownloadLink getDownloadLink() {
        return candidate.getLink();
    }

    private PluginForHost finalizeProcessingPlugin() {
        final PluginForHost plugin;
        synchronized (processingPlugin) {
            PluginClassLoader.setThreadPluginClassLoaderChild(null, null);
            plugin = processingPlugin.getAndClear();
            processingPlugin.notifyAll();
        }
        return plugin;
    }

    private Browser getPluginBrowser() {
        return new Browser();
    }

    protected void invalidateLastChallengeResponse(LogSource logger, PluginForHost plugin) {
        if (plugin != null) {
            try {
                plugin.invalidateLastChallengeResponse();
            } catch (final Throwable ignore) {
                logger.log(ignore);
            }
        }
    }

    protected void validateLastChallengeResponse(LogSource logger, PluginForHost plugin) {
        if (plugin != null) {
            try {
                plugin.validateLastChallengeResponse();
            } catch (final Throwable ignore) {
                logger.log(ignore);
            }
        }
    }

    private SingleDownloadReturnState download(final LogSource downloadLogger) {
        PluginForHost handlePlugin = null;
        try {
            downloadLogger.info("DownloadCandidate: " + candidate);
            PluginForHost linkPlugin = null;
            if (AccountCache.ACCOUNTTYPE.MULTI.equals(candidate.getCachedAccount().getType())) {
                final PluginClassLoaderChild defaultCL = session.getPluginClassLoaderChild(downloadLink.getDefaultPlugin());
                PluginClassLoader.setThreadPluginClassLoaderChild(defaultCL, defaultCL);
                // this.setContextClassLoader(defaultCL);
                linkPlugin = downloadLink.getDefaultPlugin().getLazyP().newInstance(defaultCL);
                linkPlugin.setBrowser(getPluginBrowser());
                linkPlugin.setLogger(downloadLogger);
                linkPlugin.setDownloadLink(downloadLink);
                linkPlugin.init();
                AvailableStatus availableStatus = downloadLink.getAvailableStatus();
                final long lastAvailableStatusChange = downloadLink.getLastAvailableStatusChange();
                final long availableStatusChangeTimeout = linkPlugin.getAvailableStatusTimeout(downloadLink, availableStatus);
                if (lastAvailableStatusChange + availableStatusChangeTimeout < System.currentTimeMillis()) {
                    try {
                        processingPlugin.set(linkPlugin);
                        try {
                            availableStatus = linkPlugin.checkLink(downloadLink);
                            if (AvailableStatus.FALSE == availableStatus) {
                                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            }
                        } catch (final Throwable e) {
                            downloadLogger.log(e);
                            throw e;
                        }
                    } catch (final BrowserException ignore) {
                    } catch (final SkipReasonException e) {
                        if (SkipReason.CAPTCHA.equals(e.getSkipReason())) {
                            invalidateLastChallengeResponse(downloadLogger, linkPlugin);
                        } else {
                            throw e;
                        }
                    } catch (final PluginException e) {
                        switch (e.getLinkStatus()) {
                        case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
                        case LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE:
                        case LinkStatus.ERROR_PREMIUM:
                            availableStatus = AvailableStatus.UNCHECKABLE;
                            break;
                        case LinkStatus.ERROR_FILE_NOT_FOUND:
                            availableStatus = AvailableStatus.FALSE;
                            throw e;
                        case LinkStatus.ERROR_CAPTCHA:
                            invalidateLastChallengeResponse(downloadLogger, linkPlugin);
                            break;
                        default:
                            availableStatus = AvailableStatus.UNCHECKABLE;
                            throw e;
                        }
                    } finally {
                        processingPlugin.set(null);
                        downloadLink.setAvailableStatus(availableStatus);
                        validateLastChallengeResponse(downloadLogger, linkPlugin);
                    }
                }
            }
            final PluginClassLoaderChild handleCL = session.getPluginClassLoaderChild(candidate.getCachedAccount().getPlugin());
            PluginClassLoader.setThreadPluginClassLoaderChild(handleCL, handleCL);
            // this.setContextClassLoader(handleCL);
            handlePlugin = candidate.getCachedAccount().getPlugin().getLazyP().newInstance(handleCL);
            handlePlugin.setBrowser(getPluginBrowser());
            handlePlugin.setLogger(downloadLogger);
            handlePlugin.setDownloadLink(downloadLink);
            handlePlugin.init();
            try {
                processingPlugin.set(handlePlugin);
                downloadLink.setLivePlugin(handlePlugin);
                try {
                    if (linkPlugin != null) {
                        linkPlugin.preHandle(downloadLink, account, handlePlugin);
                    }
                    final PluginForHost finalHandlePlugin = handlePlugin;
                    watchDog.localFileCheck(this, new ExceptionRunnable() {
                        @Override
                        public void run() throws Exception {
                            final File partFile = new File(downloadLink.getFileOutput() + ".part");
                            final long doneSize = Math.max((partFile.exists() ? partFile.length() : 0l), downloadLink.getView().getBytesLoaded());
                            final long remainingSize = downloadLink.getView().getBytesTotal() - Math.max(0, doneSize);
                            final DiskSpaceReservation reservation = new DiskSpaceReservation() {
                                @Override
                                public File getDestination() {
                                    return partFile;
                                }

                                @Override
                                public long getSize() {
                                    return remainingSize + Math.max(0, finalHandlePlugin.calculateAdditionalRequiredDiskSpace(downloadLink));
                                }

                                @Override
                                public Object getOwner() {
                                    return SingleDownloadController.this;
                                }

                                @Override
                                public LogInterface getLogger() {
                                    return downloadLogger;
                                }
                            };
                            final DISKSPACERESERVATIONRESULT result = watchDog.validateDiskFree(reservation);
                            switch (result) {
                            case FAILED:
                                throw new SkipReasonException(SkipReason.DISK_FULL);
                            case INVALIDDESTINATION:
                                throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
                            default:
                                break;
                            }
                        }
                    }, null);
                    startTimestamp = System.currentTimeMillis();
                    handlePlugin.handle(downloadLink, account);
                    if (linkPlugin != null) {
                        linkPlugin.postHandle(downloadLink, account, handlePlugin);
                    }
                } catch (final Throwable e) {
                    downloadLogger.log(e);
                    throw e;
                } finally {
                    try {
                        if (linkPlugin != null) {
                            linkPlugin.clean();
                        }
                    } catch (final Throwable ignore) {
                        downloadLogger.log(ignore);
                    }
                }
            } catch (DeferredRunnableException e) {
                if (e.getExceptionRunnable() != null) {
                    e.getExceptionRunnable().run();
                } else {
                    throw e;
                }
            }
            final SingleDownloadReturnState ret = new SingleDownloadReturnState(this, null, finalizeProcessingPlugin());
            return ret;
        } catch (Throwable throwable) {
            final PluginForHost lastPlugin = finalizeProcessingPlugin();
            try {
                throw throwable;
            } catch (BrowserException browserException) {
                if (isConnectionOffline(lastPlugin, browserException)) {
                    throwable = new NoInternetConnection(browserException).fillInStackTrace();
                } else if (browserException.getCause() != null) {
                    throwable = browserException.getCause();
                }
            } catch (SkipReasonException skipReasonException) {
                switch (skipReasonException.getSkipReason()) {
                case CAPTCHA:
                    invalidateLastChallengeResponse(downloadLogger, lastPlugin);
                    break;
                default:
                    break;
                }
            } catch (PluginException pluginException) {
                switch (pluginException.getLinkStatus()) {
                case LinkStatus.ERROR_CAPTCHA:
                    invalidateLastChallengeResponse(downloadLogger, lastPlugin);
                    break;
                default:
                    break;
                }
            } catch (Throwable unchanged) {
            }
            SingleDownloadReturnState ret = new SingleDownloadReturnState(this, throwable, lastPlugin);
            return ret;
        } finally {
            try {
                downloadLink.setLivePlugin(null);
                queueItem.queueLinks.remove(downloadLink);
                if (handlePlugin != null) {
                    if (!this.isAborting()) {
                        validateLastChallengeResponse(downloadLogger, handlePlugin);
                    }
                    final DownloadInterface di = handlePlugin.getDownloadInterface();
                    resumed = di != null && di.isResumedDownload();
                    try {
                        handlePlugin.clean();
                    } catch (final Throwable ignore) {
                        downloadLogger.log(ignore);
                    }
                }
                final FilePackage fp = downloadLink.getFilePackage();
                if (fp != null && !FilePackage.isDefaultFilePackage(fp)) {
                    // if we remove link without stopping them.. the filepackage may be the default package already here.
                    final FilePackageView view = fp.getView();
                    if (view != null) {
                        view.requestUpdate();
                    }
                }
            } catch (final Throwable e) {
                downloadLogger.log(e);
            }
        }
    }

    public PluginForHost getProcessingPlugin() {
        return processingPlugin.get();
    }

    public boolean isResumed() {
        return resumed;
    }

    public HTTPProxy getUsedProxy() {
        return usedProxy;
    }

    private boolean isConnectionOffline(final PluginForHost plugin, Throwable e) {
        if (Exceptions.getInstanceof(e, InterruptedException.class) != null) {
            return false;
        } else if (Exceptions.getInstanceof(e, NoRouteToHostException.class) != null) {
            // NoRouteToHostException -> not possible to connect to destination -> offline?!
            return true;
        } else if (Exceptions.getInstanceof(e, NetworkInterfaceException.class) != null) {
            // NetworkInterfaceException -> cannot find suitable/correct network device -> offline
            return true;
        } else if (Exceptions.getInstanceof(e, NoGateWayException.class) != null) {
            // NoGateWayException -> wrong connection settings? -> offline
            return true;
        } else if (isAborting()) {
            return false;
        }
        final BrowserException browserException = Exceptions.getInstanceof(e, BrowserException.class);
        HTTPProxy proxy = null;
        if (browserException != null) {
            final Throwable cause = browserException.getCause();
            if (cause == null) {
                // no cause -> no underlying IOException in browser -> not offline
                return false;
            } else if (browserException.getRequest() != null) {
                proxy = browserException.getRequest().getProxy();
            }
        } else {
            // no BrowserException -> not offline or developer has forgotten to forward causing BrowserException
            return false;
        }
        if (proxy == null && plugin != null && plugin.getBrowser() != null && plugin.getBrowser().getRequest() != null) {
            proxy = plugin.getBrowser().getRequest().getProxy();
        }
        if (proxy == null) {
            proxy = getUsedProxy();
        }
        final ProxySelectorInterface proxySelector;
        if (proxy != null) {
            proxySelector = new StaticProxySelector(proxy);
        } else {
            proxySelector = getProxySelector();
        }
        final BalancedWebIPCheck onlineCheck = new BalancedWebIPCheck(proxySelector);
        try {
            onlineCheck.getExternalIP();
        } catch (final OfflineException e2) {
            return true;
        } catch (final IPCheckException e2) {
        }
        return false;
    }

    @Override
    public void run() {
        LogSource downloadLogger = null;
        SelectProxyByURLHook hook = null;
        final PluginProgressTask task = new PluginProgressTask(null);
        final long id = UniqueAlltimeID.next();
        final AbstractProxySelectorImpl ps = getProxySelector();
        try {
            if (ps != null) {
                final Thread currentThread = Thread.currentThread();
                ps.addSelectProxyByUrlHook(hook = new SelectProxyByURLHook() {
                    @Override
                    public void onProxyChoosen(URL url, List<HTTPProxy> ret) {
                        if (currentThread == Thread.currentThread()) {
                            usedProxy = ret.get(0);
                        }
                    }
                });
            }
            String logID = downloadLink.getDefaultPlugin().getHost() + "_" + downloadLink.getDefaultPlugin().getLazyP().getClassName();
            if (AccountCache.ACCOUNTTYPE.MULTI.equals(candidate.getCachedAccount().getType())) {
                logID = logID + "_" + candidate.getCachedAccount().getPlugin().getHost();
            }
            downloadLogger = LogController.getFastPluginLogger(logID);
            downloadLogger.info("StartDownloadMarker:" + id);
            downloadLogger.info("Start Download of:" + downloadLink.getPluginPatternMatcher() + " | ID:" + id);
            super.setLogger(downloadLogger);
            try {
                watchDog.getEventSender().fireEvent(new DownloadWatchdogEvent(this, DownloadWatchdogEvent.Type.LINK_STARTED, this, candidate));
            } catch (final Throwable e) {
                downloadLogger.log(e);
            }
            task.open();
            addTask(task);
            SingleDownloadReturnState returnState = download(downloadLogger);
            if (isAborting()) {
                /* clear interrupted flag */
                interrupted();
            }
            watchDog.detach(this, returnState);
        } finally {
            try {
                if (ps != null && hook != null) {
                    ps.removeSelectProxyByUrlHook(hook);
                }
                task.reopen();
                task.close();
            } finally {
                finalizeProcessingPlugin();
                finished.set(true);
            }
            if (downloadLogger != null) {
                downloadLogger.info("StopDownloadMarker:" + id);
            }
        }
    }

    public boolean isFinished() {
        return finished.get();
    }

    @Override
    public void setLogger(LogInterface logger) {
        /* we dont allow external changes */
    }

    /**
     * @return the sizeBefore
     */
    public long getSizeBefore() {
        return sizeBefore;
    }

    public void addTask(final PluginSubTask subTask) {
        if (subTask != null) {
            synchronized (tasks) {
                tasks.add(subTask);
            }
        }
    }

    public void onDetach(DownloadLink downloadLink) {
        DownloadController.getInstance().getEventSender().removeListener(this);
        synchronized (tasks) {
            for (final PluginSubTask t : tasks) {
                t.close();
            }
        }
    }

    public void onAttach(DownloadLink downloadLink) {
        DownloadController.getInstance().getEventSender().addListener(this, true);
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
    public void onDownloadControllerRemovedPackage(FilePackage pkg) {
    }

    @Override
    public void onDownloadControllerRemovedLinklist(List<DownloadLink> list) {
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink, DownloadLinkProperty property) {
        try {
            if (downloadlink != this.downloadLink) {
                return;
            }
            if (property.getProperty() == DownloadLinkProperty.Property.PLUGIN_PROGRESS) {
                final PluginProgress newProgress = (PluginProgress) property.getValue();
                synchronized (tasks) {
                    PluginProgressTask task = null;
                    for (PluginSubTask t : tasks) {
                        if (t instanceof PluginProgressTask) {
                            if (((PluginProgressTask) t).getProgress() != newProgress) {
                                t.close();
                            } else {
                                task = (PluginProgressTask) t;
                            }
                        }
                    }
                    if (task == null) {
                        task = new PluginProgressTask(newProgress);
                        task.open();
                        addTask(task);
                    } else {
                        task.reopen();
                    }
                }
            }
        } catch (Throwable e) {
            // TODO: handle exception
        }
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

    public List<PluginSubTask> getTasks() {
        synchronized (tasks) {
            return new ArrayList<PluginSubTask>(tasks);
        }
    }

    private volatile String sessionDownloadDirectory;

    public void setSessionDownloadDirectory(String downloadDirectory) {
        this.sessionDownloadDirectory = downloadDirectory;
    }

    public String getSessionDownloadDirectory() {
        return sessionDownloadDirectory;
    }

    private volatile String sessionDownloadFilename;

    public void setSessionDownloadFilename(String downloadFilename) {
        if (StringUtils.equals(sessionDownloadFilename, downloadFilename)) {
            return;
        }
        this.sessionDownloadFilename = downloadFilename;
        if (downloadLink.hasNotificationListener()) {
            downloadLink.firePropertyChange(new DownloadLinkProperty(downloadLink, DownloadLinkProperty.Property.NAME, downloadFilename));
        }
    }

    public String getSessionDownloadFilename() {
        return sessionDownloadFilename;
    }

    public File getFileOutput(boolean ignoreUnsafe, boolean ignoreCustom) {
        String name = getSessionDownloadFilename();
        if (StringUtils.isEmpty(name)) {
            name = downloadLink.getName(ignoreUnsafe, true);
            if (name == null) {
                return null;
            }
        }
        if (!ignoreCustom) {
            String tmpName = downloadLink.getInternalTmpFilename();
            if (!StringUtils.isEmpty(tmpName)) {
                /* we have a customized fileOutputFilename */
                name = tmpName;
            }
            String customAppend = downloadLink.getInternalTmpFilenameAppend();
            if (!StringUtils.isEmpty(customAppend)) {
                name = name + customAppend;
            }
        }
        return new File(getSessionDownloadDirectory(), name);
    }
}