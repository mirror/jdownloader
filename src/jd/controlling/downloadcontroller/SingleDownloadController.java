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
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.BrowserSettingsThread;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackageView;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.raf.HashResult;

import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;

public class SingleDownloadController extends BrowserSettingsThread {

    /**
     * signals that abort request has been received
     */
    private AtomicBoolean                          abortFlag        = new AtomicBoolean(false);
    /**
     * signals the activity of this SingleDownloadController
     */
    private AtomicBoolean                          activityFlag     = new AtomicBoolean(true);
    /**
     * signals the activity of the plugin in use
     */
    private NullsafeAtomicReference<PluginForHost> processingPlugin = new NullsafeAtomicReference<PluginForHost>(null);

    public static class WaitingQueueItem {
        public final AtomicLong                          lastStartTimestamp      = new AtomicLong(System.currentTimeMillis());
        public final AtomicLong                          lastConnectionTimestamp = new AtomicLong(System.currentTimeMillis());
        private final CopyOnWriteArrayList<DownloadLink> queueLinks              = new CopyOnWriteArrayList<DownloadLink>();

        public int indexOf(DownloadLink link) {
            return queueLinks.indexOf(link);
        }
    }

    private static final HashMap<String, WaitingQueueItem> LAST_DOWNLOAD_START_TIMESTAMPS = new HashMap<String, WaitingQueueItem>();

    private final DownloadLink                             downloadLink;
    private final Account                                  account;

    private long                                           startTimestamp                 = -1;
    private final DownloadLinkCandidate                    candidate;
    private final DownloadWatchDog                         watchDog;
    private final LinkStatus                               linkStatus;

    private HashResult                                     hashResult                     = null;
    private CopyOnWriteArrayList<DownloadWatchDogJob>      jobsAfterDetach                = new CopyOnWriteArrayList<DownloadWatchDogJob>();
    private WaitingQueueItem                               queueItem;
    private final long                                     sizeBefore;

    public WaitingQueueItem getQueueItem() {
        return queueItem;
    }

    public CopyOnWriteArrayList<DownloadWatchDogJob> getJobsAfterDetach() {
        return jobsAfterDetach;
    }

    @Override
    public synchronized void start() {
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
        if (plugin != null) return plugin.getDownloadInterface();
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
        super("Download");
        setPriority(Thread.MIN_PRIORITY);
        this.watchDog = watchDog;
        this.candidate = candidate;
        super.setCurrentProxy(candidate.getProxy());
        this.downloadLink = candidate.getLink();
        this.sizeBefore = Math.max(0, downloadLink.getDownloadCurrent());
        this.account = candidate.getCachedAccount().getAccount();
        String host = candidate.getCachedAccount().getPlugin().getHost();
        queueItem = LAST_DOWNLOAD_START_TIMESTAMPS.get(host);
        if (queueItem == null) {
            queueItem = new WaitingQueueItem();
            LAST_DOWNLOAD_START_TIMESTAMPS.put(host, queueItem);
        }
        queueItem.queueLinks.add(downloadLink);
        linkStatus = new LinkStatus(downloadLink);
        setName("Download: " + downloadLink.getName() + "_" + downloadLink.getHost());
    }

    @Override
    public void setCurrentProxy(final HTTPProxy proxy) {
        /* we dont allow external changes */
    }

    @Override
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
        return activityFlag.get();
    }

    protected void abort() {
        if (activityFlag.get() == false) {
            /* this singleDownloadController is no longer active */
            return;
        }
        if (abortFlag.compareAndSet(false, true)) {
            /* this is our initial abort request */
            Thread abortThread = new Thread() {

                @Override
                public void run() {
                    while (activityFlag.get()) {
                        try {
                            DownloadInterface dli = getDownloadInstance();
                            if (dli != null) {
                                dli.stopDownload();
                            }
                        } catch (final Throwable e) {
                            LogSource.exception(logger, e);
                        }
                        synchronized (activityFlag) {
                            if (activityFlag.get() == false) return;
                            try {
                                activityFlag.wait(1000);
                            } catch (final InterruptedException e) {
                            }
                        }
                    }
                }

            };
            abortThread.setDaemon(true);
            abortThread.setName("Abort: " + downloadLink.getName() + "_" + downloadLink.getUniqueID());
            abortThread.start();
            if (processingPlugin.get() != null) {
                /* do not interrupt the SingleDownloadController itself */
                interrupt();
            }
        }
    }

    public DownloadLink getDownloadLink() {
        return candidate.getLink();
    }

    private SingleDownloadReturnState download(LogSource downloadLogger) {
        PluginForHost livePlugin = null;
        PluginForHost originalPlugin = null;
        boolean validateChallenge = true;
        try {
            final PluginClassLoaderChild cl;
            this.setContextClassLoader(cl = PluginClassLoader.getInstance().getChild());
            livePlugin = candidate.getCachedAccount().getPlugin().getLazyP().newInstance(cl);
            livePlugin.setBrowser(new Browser());
            livePlugin.setLogger(downloadLogger);
            livePlugin.setDownloadLink(downloadLink);
            originalPlugin = livePlugin;
            switch (candidate.getCachedAccount().getType()) {
            case MULTI:
                originalPlugin = downloadLink.getDefaultPlugin().getLazyP().newInstance(cl);
                originalPlugin.setBrowser(new Browser());
                originalPlugin.setLogger(downloadLogger);
                originalPlugin.setDownloadLink(downloadLink);
                break;
            }
            downloadLink.setLivePlugin(livePlugin);
            watchDog.localFileCheck(this, null, null);
            try {
                startTimestamp = System.currentTimeMillis();
                switch (candidate.getCachedAccount().getType()) {
                case MULTI:
                    processingPlugin.set(originalPlugin);
                    originalPlugin.init();
                    if (AvailableStatus.FALSE == originalPlugin.requestFileInformation(downloadLink)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    break;
                }
                processingPlugin.set(livePlugin);
                livePlugin.init();
                livePlugin.handle(downloadLink, account);
                SingleDownloadReturnState ret = new SingleDownloadReturnState(this, null, processingPlugin.getAndSet(null));
                return ret;
            } catch (BrowserException browserException) {
                downloadLogger.log(browserException);
                try {
                    browserException.getConnection().disconnect();
                } catch (final Throwable ignore) {
                }
                if (browserException.getException() != null) {
                    throw browserException.getException();
                } else {
                    throw browserException;
                }
            }
        } catch (final Throwable e) {
            if (e instanceof PluginException && ((PluginException) e).getLinkStatus() == LinkStatus.ERROR_CAPTCHA) {
                validateChallenge = false;
            } else if (e instanceof SkipReasonException && ((SkipReasonException) e).getSkipReason() == SkipReason.CAPTCHA) {
                validateChallenge = false;
            }
            downloadLogger.log(e);
            SingleDownloadReturnState ret = new SingleDownloadReturnState(this, e, processingPlugin.getAndSet(null));
            return ret;
        } finally {
            queueItem.queueLinks.remove(downloadLink);
            downloadLink.setLivePlugin(null);
            finalizePlugins(downloadLogger, originalPlugin, livePlugin, validateChallenge);
            if (downloadLink.getFilePackage() != null) {
                // if we remove link without stopping them.. the filepackage may be the default package already here.

                FilePackageView view = downloadLink.getFilePackage().getView();
                if (view != null) {
                    view.requestUpdate();
                }
            }

        }
    }

    private void finalizePlugins(LogSource logger, PluginForHost originalPlugin, PluginForHost livePlugin, boolean valid) {
        switch (candidate.getCachedAccount().getType()) {
        case MULTI:
            if (originalPlugin != null) {
                try {
                    if (valid) {
                        originalPlugin.validateLastChallengeResponse();
                    } else {
                        originalPlugin.invalidateLastChallengeResponse();
                    }
                } catch (final Throwable ignore) {
                    logger.log(ignore);
                }
                try {
                    originalPlugin.clean();
                } catch (final Throwable ignore) {
                    logger.log(ignore);
                }
            }
        case ORIGINAL:
        case NONE:
            if (livePlugin != null) {
                try {
                    if (valid) {
                        livePlugin.validateLastChallengeResponse();
                    } else {
                        livePlugin.invalidateLastChallengeResponse();
                    }
                } catch (final Throwable ignore) {
                    logger.log(ignore);
                }
                try {
                    livePlugin.clean();
                } catch (final Throwable ignore) {
                    logger.log(ignore);
                }
            }
        }
    }

    @Override
    public void run() {
        LogSource downloadLogger = null;
        try {
            downloadLogger = LogController.getInstance().getLogger(downloadLink.getDefaultPlugin());
            downloadLogger.setAllowTimeoutFlush(false);
            downloadLogger.info("Start Download of " + downloadLink.getDownloadURL());
            super.setLogger(downloadLogger);
            SingleDownloadReturnState returnState = download(downloadLogger);
            if (isAborting()) {
                /* clear interrupted flag */
                interrupted();
            }
            watchDog.detach(this, returnState);
        } finally {
            synchronized (activityFlag) {
                activityFlag.set(false);
                activityFlag.notifyAll();
            }
        }
    }

    @Override
    public void setLogger(Logger logger) {
        /* we dont allow external changes */
    }

    /**
     * @return the sizeBefore
     */
    public long getSizeBefore() {
        return sizeBefore;
    }

}