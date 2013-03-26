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
import java.util.logging.Logger;

import jd.controlling.AccountController;
import jd.controlling.proxy.ProxyInfo;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.BrowserSettingsThread;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageView;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

import org.appwork.controlling.StateMachine;
import org.appwork.controlling.StateMachineInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Regex;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.FileCreationEvent;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.CleanAfterDownloadAction;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.IfFileExistsAction;
import org.jdownloader.translate._JDT;

public class SingleDownloadController extends BrowserSettingsThread implements StateMachineInterface {

    private static final Object             DUPELOCK          = new Object();

    private boolean                         aborted           = false;
    private boolean                         handling          = false;

    private PluginForHost                   originalPlugin    = null;
    private PluginForHost                   livePlugin        = null;

    private DownloadLink                    downloadLink;

    private LinkStatus                      linkStatus;

    private Account                         account           = null;
    private SingleDownloadControllerHandler handler           = null;

    private StateMachine                    stateMachine;

    private DownloadSpeedManager            connectionHandler = null;

    private Throwable                       exception         = null;

    private LogSource                       downloadLogger    = null;
    private long                            startTimestamp    = -1;

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public DownloadSpeedManager getConnectionHandler() {
        return connectionHandler;
    }

    public static final org.appwork.controlling.State IDLE_STATE    = new org.appwork.controlling.State("IDLE");
    public static final org.appwork.controlling.State RUNNING_STATE = new org.appwork.controlling.State("RUNNING");
    public static final org.appwork.controlling.State FINAL_STATE   = new org.appwork.controlling.State("FINAL_STATE");
    static {
        IDLE_STATE.addChildren(RUNNING_STATE);
        RUNNING_STATE.addChildren(FINAL_STATE);
    }

    public SingleDownloadControllerHandler getHandler() {
        return handler;
    }

    public void setHandler(SingleDownloadControllerHandler handler) {
        this.handler = handler;
    }

    /**
     * Erstellt einen Thread zum Start des Downloadvorganges
     * 
     * @param controller
     *            Controller
     * @param dlink
     *            Link, der heruntergeladen werden soll
     */
    public SingleDownloadController(DownloadLink dlink, Account account) {
        this(dlink, account, null, null);
    }

    public SingleDownloadController(DownloadLink dlink, Account account, DownloadSpeedManager cmanager) {
        this(dlink, account, null, cmanager);
    }

    public SingleDownloadController(DownloadLink dlink, Account account, ProxyInfo proxy, DownloadSpeedManager manager) {
        super("Download: " + dlink.getName() + "_" + dlink.getHost());
        this.connectionHandler = manager;
        stateMachine = new StateMachine(this, IDLE_STATE, FINAL_STATE);
        downloadLink = dlink;
        linkStatus = downloadLink.getLinkStatus();
        /* mark link plugin active */
        linkStatus.setActive(true);
        setPriority(Thread.MIN_PRIORITY);
        downloadLink.setDownloadLinkController(this);
        this.account = account;
        this.setCurrentProxy(proxy);
        downloadLogger = LogController.getInstance().getLogger(dlink.getDefaultPlugin());
        downloadLogger.setAllowTimeoutFlush(false);
        downloadLogger.info("Start Download of " + downloadLink.getDownloadURL());
        super.setLogger(downloadLogger);
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

    /**
     * Bricht den Downloadvorgang ab.
     */
    public SingleDownloadController abortDownload() {
        if (aborted == true || !handling) return this;
        if (downloadLink.getLinkStatus().isPluginActive() == false) return this;
        aborted = true;
        Thread abortThread = new Thread() {

            @Override
            public void run() {
                while (downloadLink.getLinkStatus().isPluginActive()) {
                    DownloadInterface dli = downloadLink.getDownloadInstance();
                    if (dli != null && downloadLink.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                        /* finally a downloadInterface available to abort */
                        dli.stopDownload();
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        downloadLogger.log(e);
                    }
                }
            }

        };
        abortThread.setDaemon(true);
        abortThread.setName("Abort: " + downloadLink.getName() + "_" + downloadLink.getHost());
        abortThread.start();
        interrupt();
        return this;
    }

    public DownloadLink getDownloadLink() {
        return downloadLink;
    }

    private void handlePlugin() {
        try {
            startTimestamp = System.currentTimeMillis();
            linkStatus.setStatusText(_JDT._.gui_download_create_connection());
            if ((downloadLink.getLinkStatus().getRetryCount()) <= livePlugin.getMaxRetries(downloadLink, getAccount())) {
                final long sizeBefore = Math.max(0, downloadLink.getDownloadCurrent());
                long traffic = 0;
                try {
                    try {
                        handling = true;
                        try {
                            if (originalPlugin != livePlugin) {
                                /*
                                 * 2 different plugins -> multihoster
                                 * 
                                 * let's use original one to do a linkcheck and then use livePlugin(multihost service) to download the link
                                 */
                                originalPlugin.init();
                                originalPlugin.requestFileInformation(downloadLink);
                                if (AvailableStatus.FALSE == downloadLink.getAvailableStatus()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            }
                            livePlugin.init();
                            livePlugin.handle(downloadLink, account);
                        } catch (BrowserException e) {
                            try {
                                /* make sure we close current connection */
                                e.getConnection().disconnect();
                            } catch (final Throwable e3) {
                            }
                            /*
                             * damit browserexceptions korrekt weitergereicht werden
                             */
                            if (e.getException() != null) {
                                throw e.getException();
                            } else {
                                throw e;
                            }
                        }
                    } catch (final Throwable e) {
                        LogSource.exception(logger, e);
                        /* we keep the exception in case we need it later */
                        exception = e;
                        throw e;
                    } finally {
                        handling = false;
                    }
                } catch (PluginException e) {
                    e.fillLinkStatus(downloadLink.getLinkStatus());
                } catch (UnknownHostException e) {
                    linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                    linkStatus.setErrorMessage(_JDT._.plugins_errors_nointernetconn());
                    linkStatus.setValue(JsonConfig.create(GeneralSettings.class).getNetworkIssuesTimeout());
                } catch (SocketTimeoutException e) {
                    linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                    linkStatus.setErrorMessage(_JDT._.plugins_errors_hosteroffline());
                    linkStatus.setValue(JsonConfig.create(GeneralSettings.class).getNetworkIssuesTimeout());
                } catch (SocketException e) {
                    LogSource.exception(logger, e);
                    linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                    linkStatus.setErrorMessage(_JDT._.plugins_errors_disconnect());
                    linkStatus.setValue(JsonConfig.create(GeneralSettings.class).getNetworkIssuesTimeout());
                } catch (IOException e) {
                    LogSource.exception(logger, e);
                    linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                    linkStatus.setErrorMessage(_JDT._.plugins_errors_hosterproblem());
                    linkStatus.setValue(JsonConfig.create(GeneralSettings.class).getDownloadUnknownIOExceptionWaittime());
                } catch (InterruptedException e) {
                    linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFECT);
                    linkStatus.setErrorMessage(_JDT._.plugins_errors_error() + "Interrupted");
                } catch (Throwable e) {
                    LogSource.exception(logger, e);
                    linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFECT);
                    linkStatus.setErrorMessage(_JDT._.plugins_errors_error() + "Throwable");
                } finally {
                    traffic = Math.max(0, downloadLink.getDownloadCurrent() - sizeBefore);
                }
                /* now lets update the account if any was used */
                if (account != null) {
                    final AccountInfo ai = account.getAccountInfo();
                    /* check traffic of account (eg traffic limit reached) */
                    if (traffic > 0 && ai != null && !ai.isUnlimitedTraffic()) {
                        long left = Math.max(0, ai.getTrafficLeft() - traffic);
                        ai.setTrafficLeft(left);
                        if (left == 0) {
                            if (ai.isSpecialTraffic()) {
                                downloadLogger.severe("Premium Account " + account.getUser() + ": Traffic Limit could be reached, but SpecialTraffic might be available!");
                            } else {
                                downloadLogger.severe("Premium Account " + account.getUser() + ": Traffic Limit reached");
                                account.setTempDisabled(true);
                            }
                        }
                    }
                }
            }
            if (isAborted() && !linkStatus.isFinished()) {
                linkStatus.setErrorMessage(null);
                linkStatus.setStatus(LinkStatus.TODO);
                return;
            }
            if (linkStatus.isFailed()) {
                downloadLogger.warning("\r\nError occured- " + downloadLink.getLinkStatus());
            }
            if (handler != null) {
                /* special handler is used */
                if (handler.handleDownloadLink(downloadLink, account)) return;
            }
            switch (linkStatus.getLatestStatus()) {
            case LinkStatus.TEMP_IGNORE:
                onTempIgnore();
                break;
            case LinkStatus.ERROR_LOCAL_IO:
                onErrorLocalIO();
                break;
            case LinkStatus.ERROR_IP_BLOCKED:
                onErrorIPWaittime();
                break;
            case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
                onErrorDownloadTemporarilyUnavailable();
                break;
            case LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE:
                onErrorHostTemporarilyUnavailable();
                break;
            case LinkStatus.ERROR_FILE_NOT_FOUND:
                onErrorFileNotFound();
                break;
            case LinkStatus.ERROR_FATAL:
                onErrorFatal();
                break;
            case LinkStatus.ERROR_CAPTCHA:
                onErrorCaptcha();
                break;
            case LinkStatus.ERROR_PREMIUM:
                onErrorPremium();
                break;
            case LinkStatus.ERROR_DOWNLOAD_INCOMPLETE:
                onErrorIncomplete();
                break;
            case LinkStatus.ERROR_ALREADYEXISTS:
                onErrorFileExists();
                break;
            case LinkStatus.ERROR_DOWNLOAD_FAILED:
                onErrorDownloadFailed();
                break;
            case LinkStatus.ERROR_PLUGIN_DEFECT:
                onErrorPluginDefect();
                break;
            case LinkStatus.ERROR_NO_CONNECTION:
            case LinkStatus.ERROR_TIMEOUT_REACHED:
                onErrorNoConnection();
                break;
            default:
                if (linkStatus.hasStatus(LinkStatus.FINISHED)) {
                    onDownloadFinishedSuccessFull();
                } else {
                    retry();
                }
            }
        } catch (Throwable e) {
            downloadLogger.log(e);
            downloadLogger.severe("Error in Plugin Version: " + downloadLink.getLivePlugin().getVersion());
        }
    }

    private void onTempIgnore() {
    }

    protected void onErrorCaptcha() {
        downloadLogger.warning("Captcha not correct-> will retry!");
        retry();
    }

    protected void onErrorPluginDefect() {
        livePlugin.errLog(exception, livePlugin.getBrowser(), downloadLink);
        long rev = downloadLink.getLivePlugin() == null ? -1 : downloadLink.getLivePlugin().getVersion();
        downloadLogger.warning("The Plugin for " + livePlugin.getHost() + " seems to be out of date(rev" + rev + "). Please inform the Support-team http://jdownloader.org/support.");
        if (exception != null) {
            /* show stacktrace where the exception happened */
            downloadLogger.log(exception);
        }
        if (downloadLink.getLinkStatus().getErrorMessage() != null) {
            downloadLogger.warning(downloadLink.getLinkStatus().getErrorMessage());
        }
        /* show last browser request headers+content in logfile */
        try {
            downloadLogger.finest("\r\n" + livePlugin.getBrowser().getRequest().getHttpConnection());
        } catch (Throwable e2) {
        }
        try {
            downloadLogger.finest("\r\n" + livePlugin.getBrowser());
        } catch (Throwable e2) {
        }
        String orgMessage = downloadLink.getLinkStatus().getErrorMessage();
        downloadLink.getLinkStatus().setErrorMessage(_JDT._.controller_status_plugindefective() + (orgMessage == null ? "" : " " + orgMessage));
    }

    /**
     * download aborted by user?
     * 
     * @return
     */
    public boolean isAborted() {
        return aborted;
    }

    protected void onDownloadFinishedSuccessFull() {
    }

    /*
     * returns true if we can try again or false if maxRetries reached
     */
    protected boolean retry() {
        int r;
        if (linkStatus.getValue() > 0) {
            linkStatus.setStatusText(null);
        }
        if ((r = linkStatus.getRetryCount()) <= livePlugin.getMaxRetries(downloadLink, getAccount())) {
            linkStatus.reset();
            linkStatus.setRetryCount(r + 1);
            linkStatus.setErrorMessage(null);
            try {
                livePlugin.sleep(Math.max((int) linkStatus.getValue(), 2000), downloadLink);
            } catch (PluginException e) {
                linkStatus.setStatusText(null);
            }
            return true;
        } else {
            linkStatus.setErrorMessage("Download failed too often! Mark link as defect");
            linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFECT);
            return false;
        }

    }

    private void onErrorDownloadFailed() {
        if (linkStatus.getErrorMessage() == null) {
            linkStatus.setErrorMessage(_JDT._.plugins_error_downloadfailed());
        }
        if (linkStatus.getValue() != LinkStatus.VALUE_FAILED_HASH) {
            /* no HashCheck error happened, so we handle it here */
            if (retry()) {
                /* we can try again, let's wait 5 mins */
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                linkStatus.setWaitTime(JsonConfig.create(GeneralSettings.class).getDownloadHashCheckFailedRetryWaittime());
            }
        }
    }

    protected void onErrorFatal() {
    }

    private void onErrorFileExists() {
        IfFileExistsAction doAction = JsonConfig.create(GeneralSettings.class).getIfFileExistsAction();
        switch (doAction) {
        case ASK_FOR_EACH_FILE:
            try {
                doAction = UIOManager.I().show(IfFileExistsDialogInterface.class, new IfFileExistsDialog(downloadLink.getFileOutput(), downloadLink.getFilePackage().getName(), downloadLink.getFilePackage().getName() + "_" + downloadLink.getFilePackage().getCreated())).getAction();
            } catch (DialogNoAnswerException e1) {
                doAction = IfFileExistsAction.SKIP_FILE;
            }
            break;
        }
        synchronized (DUPELOCK) {
            /* we synchronize here to avoid ugly concurrency issues */
            switch (doAction) {
            case SKIP_FILE:
                linkStatus.setErrorMessage(_JDT._.controller_status_fileexists_skip());
                downloadLink.setEnabled(false);
                break;
            case AUTO_RENAME:
                // auto rename
                String splitName[] = CrossSystem.splitFileName(downloadLink.getName());
                String downloadPath = downloadLink.getFilePackage().getDownloadDirectory();
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
                } catch (Throwable e) {
                    downloadLogger.log(e);
                    downloadLink.forceFileName(null);
                }
                linkStatus.reset();
                break;
            default:
                if (new File(downloadLink.getFileOutput()).delete()) {
                    /* delete local file and retry = overwrite */
                    linkStatus.reset();
                } else {
                    /* delete failed */
                    linkStatus.setErrorMessage(_JDT._.controller_status_fileexists_overwritefailed() + downloadLink.getFileOutput());
                    downloadLink.setEnabled(false);
                }
            }
        }

    }

    protected void onErrorFileNotFound() {
        downloadLogger.severe("Link is no longer available:" + downloadLink.getDownloadURL());
        downloadLink.setAvailable(false);
        downloadLink.setEnabled(false);
    }

    protected void onErrorIncomplete() {
        retry();
    }

    protected void onErrorNoConnection() {
        downloadLogger.severe("Error occurred: No Server connection");
        if (exception != null) {
            /* show stacktrace where the exception happened */
            downloadLogger.log(exception);
        }
        linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        linkStatus.setWaitTime(JsonConfig.create(GeneralSettings.class).getWaittimeOnConnectionLoss());
        if (linkStatus.getErrorMessage() == null) {
            linkStatus.setErrorMessage(_JDT._.controller_status_connectionproblems());
        }
    }

    protected void onErrorPremium() {
        if (linkStatus.getValue() == PluginException.VALUE_ID_PREMIUM_ONLY) {
            linkStatus.addStatus(LinkStatus.TEMP_IGNORE);
            linkStatus.setValue(LinkStatus.TEMP_IGNORE_REASON_NO_SUITABLE_ACCOUNT_FOUND);
            return;
        } else if (linkStatus.getValue() == PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE) {
            downloadLogger.severe("Premium Account " + account.getUser() + ": Traffic Limit reached");
            account.setTempDisabled(true);
            account.getAccountInfo().setTrafficLeft(0);
        } else if (linkStatus.getValue() == PluginException.VALUE_ID_PREMIUM_DISABLE) {
            downloadLogger.severe("Premium Account " + account.getUser() + ": expired");
            account.setEnabled(false);
        } else {
            downloadLogger.severe("Premium Account " + account.getUser() + ": exception");
            account.setEnabled(false);
            if (exception != null) {
                /* show stacktrace where the exception happened */
                downloadLogger.log(exception);
            }
        }
        /* we reset linkStatus so link can be download again as free user */
        linkStatus.reset();
    }

    protected void onErrorLocalIO() {
        downloadLogger.severe("LOCALIO Error on: " + downloadLink.getFileOutput() + " -> disable link!");
        downloadLink.setEnabled(false);
    }

    protected void onErrorDownloadTemporarilyUnavailable() {
        if (linkStatus.getErrorMessage() == null) {
            linkStatus.setErrorMessage(_JDT._.controller_status_tempunavailable());
        }
        /*
         * Value<0 bedeutet das der link dauerhauft deaktiviert bleiben soll. value>0 gibt die zeit an die der link deaktiviert bleiben muss
         * in ms. value==0 macht default 30 mins Der DownloadWatchdoggibt den Link wieder frei ewnn es zeit ist.
         */
        if (linkStatus.getValue() > 0) {
            linkStatus.setWaitTime(linkStatus.getValue());
            downloadLogger.warning("Error occurred: Temporarily unavailable: Please wait " + linkStatus.getValue() + " ms for a retry");
        } else if (linkStatus.getValue() == 0) {
            linkStatus.setWaitTime(JsonConfig.create(GeneralSettings.class).getDownloadTempUnavailableRetryWaittime());
            downloadLogger.warning("Error occurred: Temporarily unavailable: Please wait " + JsonConfig.create(GeneralSettings.class).getDownloadTempUnavailableRetryWaittime() + " ms for a retry");
        } else {
            downloadLogger.warning("Error occurred: Temporarily unavailable: disable link!");
            linkStatus.resetWaitTime();
            downloadLink.setEnabled(false);
        }
        downloadLogger.warning("Error occurred: Temporarily unavailable: " + linkStatus.getErrorMessage());
        if (linkStatus.getValue() >= 0) {
            /* plugin can evaluate retrycount and act differently then */
            downloadLink.getLinkStatus().setRetryCount(downloadLink.getLinkStatus().getRetryCount() + 1);
        }
    }

    protected void onErrorHostTemporarilyUnavailable() {
        long milliSeconds = linkStatus.getValue();
        if (milliSeconds <= 0) {
            downloadLogger.severe(_JDT._.plugins_errors_pluginerror());
            milliSeconds = 60 * 60 * 1000l;
        }
        if (account != null) {
            /*
             * check blocked account(eg free user accounts with waittime)
             */
            downloadLogger.severe("Account: " + account.getUser() + " is blocked, temp. disabling it!");
            AccountController.getInstance().addAccountBlocked(account, milliSeconds);
        }
        downloadLogger.warning("Error occurred: Download from this host is currently not possible: Please wait " + milliSeconds + " ms for a retry");
        linkStatus.setWaitTime(milliSeconds);
        HTTPProxy proxyInfo = getCurrentProxy();
        if (proxyInfo != null && proxyInfo instanceof ProxyInfo) {
            /* set remaining waittime for host-temp unavailable */
            ((ProxyInfo) proxyInfo).setHostBlockedTimeout(downloadLink, milliSeconds);
        }

    }

    protected void onErrorIPWaittime() {
        long milliSeconds = linkStatus.getValue();
        if (milliSeconds <= 0) {
            downloadLogger.severe(_JDT._.plugins_errors_pluginerror());
            milliSeconds = 3600000l;
        }
        if (account != null) {
            /*
             * check blocked account(eg free user accounts with waittime)
             */
            downloadLogger.severe("Account: " + account.getUser() + " is blocked, temp. disabling it!");
            AccountController.getInstance().addAccountBlocked(account, milliSeconds);
            /*
             * we reset linkStatus so link can be download again as free user(not with this account)
             */
            linkStatus.reset();
            return;
        }
        linkStatus.setWaitTime(milliSeconds);
        linkStatus.setStatusText(null);
        HTTPProxy proxyInfo = getCurrentProxy();
        if (proxyInfo != null && proxyInfo instanceof ProxyInfo) {
            /* set remaining waittime for host-temp unavailable */
            ((ProxyInfo) proxyInfo).setHostIPBlockTimeout(downloadLink, milliSeconds);
        }
    }

    @Override
    public void run() {
        try {
            stateMachine.setStatus(RUNNING_STATE);
            linkStatus.setStatusText(null);
            linkStatus.setErrorMessage(null);
            linkStatus.resetWaitTime();
            /*
             * we are going to download this link, create new liveplugin instance here
             */
            /* we want a fresh ClassLoader for this download! */
            PluginClassLoaderChild cl;
            this.setContextClassLoader(cl = PluginClassLoader.getInstance().getChild());
            try {
                originalPlugin = downloadLink.getDefaultPlugin().getLazyP().newInstance(cl);
            } catch (UpdateRequiredClassNotFoundException e) {
                downloadLogger.log(e);
                downloadLink.getLinkStatus().setStatus(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (account != null && !account.getHoster().equalsIgnoreCase(downloadLink.getHost())) {
                /* another plugin to handle this download! ->multihoster */
                try {
                    LazyHostPlugin lazy = HostPluginController.getInstance().get(account.getHoster());
                    downloadLink.setLivePlugin(lazy.newInstance(cl));
                } catch (UpdateRequiredClassNotFoundException e) {
                    downloadLogger.log(e);
                    downloadLink.getLinkStatus().setStatus(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                downloadLink.setLivePlugin(originalPlugin);
            }
            livePlugin = downloadLink.getLivePlugin();
            if (livePlugin != null && originalPlugin != null) {

                livePlugin.setLogger(downloadLogger);

                /*
                 * handle is only called in download situation, that why we create a new browser instance here
                 */
                livePlugin.setBrowser(new Browser());
                if (originalPlugin != livePlugin) {
                    /* we have 2 different plugins -> multihoster */
                    originalPlugin.setBrowser(new Browser());

                    originalPlugin.setLogger(downloadLogger);
                    originalPlugin.setDownloadLink(downloadLink);
                }
                if (downloadLink.getDownloadURL() == null) {
                    downloadLink.getLinkStatus().setStatusText(_JDT._.controller_status_containererror());
                    downloadLink.getLinkStatus().setErrorMessage(_JDT._.controller_status_containererror());
                    downloadLink.setEnabled(false);
                    return;
                }
                /* check ob Datei existiert oder bereits geladen wird */
                synchronized (DUPELOCK) {
                    /*
                     * dieser sync block dient dazu das immer nur ein link gestartet wird und dann der dupe check durchgeführt werden kann
                     */
                    if (DownloadWatchDog.preDownloadCheckFailed(downloadLink)) {
                        if (downloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) onErrorFileExists();
                        return;
                    }
                    /*
                     * setinprogress innerhalb des sync damit keine 2 downloads gleichzeitig in progress übergehen können
                     */
                    linkStatus.setInProgress(true);
                }
                handlePlugin();
                if (isAborted() && !linkStatus.isFinished()) {
                    /* download aborted */
                    LogController.GL.info("\r\nDownload stopped- " + downloadLink.getName());
                    downloadLogger.clear();
                } else if (linkStatus.isFinished()) {
                    /* error free */
                    LogController.GL.finest("\r\nFinished- " + downloadLink.getLinkStatus());
                    LogController.GL.info("\r\nFinished- " + downloadLink.getName() + "->" + downloadLink.getFileOutput());
                    downloadLogger.clear();
                    FileCreationManager.getInstance().getEventSender().fireEvent(new FileCreationEvent(this, FileCreationEvent.Type.NEW_FILES, new File[] { new File(downloadLink.getFileOutput()) }));
                    if (CleanAfterDownloadAction.CLEANUP_IMMEDIATELY.equals(org.jdownloader.settings.staticreferences.CFG_GENERAL.CFG.getCleanupAfterDownloadAction())) {
                        LogController.GL.info("Remove Link " + downloadLink.getName() + " because Finished and CleanupImmediately!");
                        java.util.List<DownloadLink> remove = new ArrayList<DownloadLink>();
                        remove.add(downloadLink);
                        DownloadController.getInstance().removeChildren(remove);
                    } else if (CleanAfterDownloadAction.CLEANUP_AFTER_PACKAGE_HAS_FINISHED.equals(org.jdownloader.settings.staticreferences.CFG_GENERAL.CFG.getCleanupAfterDownloadAction())) {
                        FilePackage fp = downloadLink.getFilePackage();
                        FilePackageView fpv = new FilePackageView(fp);
                        if (fpv.isFinished() && fpv.getDisabledCount() == 0) {

                            LogController.GL.info("Remove Package " + fp.getName() + " because Finished and CleanupPackageFinished!");
                            DownloadController.getInstance().removePackage(fp);
                        } else if (fpv.isFinished()) {
                            LogController.GL.info("Did NOT remove Package " + fp.getName() + " because Finished and Disabled Links found!");
                        }
                    }
                }
            }
        } finally {
            downloadLogger.close();
            try {
                linkStatus.setInProgress(false);
                /* cleanup the DownloadInterface/Controller references */
                downloadLink.setDownloadLinkController(null);
                downloadLink.setDownloadInstance(null);
                downloadLink.setLivePlugin(null);
                if (originalPlugin != livePlugin) {
                    /* we have 2 different plugins */
                    originalPlugin.clean();
                    originalPlugin.setDownloadLink(null);
                }
                if (livePlugin != null) {
                    livePlugin.clean();
                    livePlugin.setDownloadLink(null);
                }
            } finally {
                linkStatus.setActive(false);
                stateMachine.setStatus(FINAL_STATE);
                linkStatus = null;
                exception = null;
                livePlugin = null;
                originalPlugin = null;
                this.setContextClassLoader(null);
            }
        }
    }

    @Override
    public void setLogger(Logger logger) {
        /* we dont allow external changes */
    }

    public StateMachine getStateMachine() {
        return stateMachine;
    }

}