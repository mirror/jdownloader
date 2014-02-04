package org.jdownloader.statistics;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import jd.controlling.downloadcontroller.AccountCache.CachedAccount;
import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.plugins.download.raf.HashResult;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.jdserv.stats.StatsManagerConfig;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.PluginTaskID;
import org.jdownloader.plugins.tasks.PluginSubTask;

public class StatsManager implements GenericConfigEventListener<Object>, DownloadWatchdogListener, Runnable {
    private static final StatsManager INSTANCE = new StatsManager();

    private static final boolean      DISABLED = false;

    /**
     * get the only existing instance of StatsManager. This is a singleton
     * 
     * @return
     */
    public static StatsManager I() {
        return StatsManager.INSTANCE;
    }

    private StatsManagerConfig             config;

    private long                           startTime;
    private LogSource                      logger;
    private ArrayList<AbstractLogEntry>    list;
    private Thread                         thread;

    private HashMap<String, AtomicInteger> counterMap;

    private void log(DownloadLogEntry dl) {
        if (isEnabled()) {
            synchronized (list) {
                list.add(dl);
                list.notifyAll();
            }
        }

    }

    /**
     * Create a new instance of StatsManager. This is a singleton class. Access the only existing instance by using {@link #link()}.
     */
    private StatsManager() {
        list = new ArrayList<AbstractLogEntry>();
        counterMap = new HashMap<String, AtomicInteger>();
        config = JsonConfig.create(StatsManagerConfig.class);
        logger = LogController.getInstance().getLogger(StatsManager.class.getName());

        DownloadWatchDog.getInstance().getEventSender().addListener(this);
        config._getStorageHandler().getKeyHandler("enabled").getEventSender().addListener(this);
        thread = new Thread(this);
        thread.setName("StatsSender");
        thread.start();
    }

    /**
     * this setter does not set the config flag. Can be used to disable the logger for THIS session.
     * 
     * @param b
     */
    public void setEnabled(boolean b) {
        config.setEnabled(b);
    }

    public boolean isEnabled() {

        return config.isEnabled() && !DISABLED;
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {

    }

    @Override
    public void onDownloadWatchdogDataUpdate() {
    }

    @Override
    public void onDownloadWatchdogStateIsIdle() {
    }

    @Override
    public void onDownloadWatchdogStateIsPause() {
    }

    @Override
    public void onDownloadWatchdogStateIsRunning() {
    }

    @Override
    public void onDownloadWatchdogStateIsStopped() {
    }

    @Override
    public void onDownloadWatchdogStateIsStopping() {
    }

    @Override
    public void onDownloadControllerStart(SingleDownloadController downloadController, DownloadLinkCandidate candidate) {
    }

    @Override
    public void onDownloadControllerStopped(SingleDownloadController downloadController, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
        try {
            DownloadLogEntry dl = new DownloadLogEntry();

            HashResult hashResult = downloadController.getHashResult();
            long startedAt = downloadController.getStartTimestamp();
            DownloadLink link = downloadController.getDownloadLink();
            long downloadTime = link.getView().getDownloadTime();
            List<PluginSubTask> tasks = downloadController.getTasks();
            PluginSubTask plugintask = tasks.get(0);
            PluginSubTask downloadTask = null;
            long userIO = 0l;
            long captcha = 0l;
            long waittime = 0l;
            for (int i = 1; i < tasks.size(); i++) {
                PluginSubTask task = tasks.get(i);
                if (downloadTask == null) {
                    switch (task.getId()) {
                    case CAPTCHA:
                        captcha += task.getRuntime();
                        break;
                    case USERIO:
                        userIO += task.getRuntime();
                        break;
                    case WAIT:
                        waittime += task.getRuntime();
                        break;

                    }
                }
                if (task.getId() == PluginTaskID.DOWNLOAD) {
                    downloadTask = task;
                    break;
                }
            }
            if (downloadTask == null) {
                // download stopped or failed, before the downloadtask
            }
            long pluginRuntime = downloadTask != null ? (downloadTask.getStartTime() - plugintask.getStartTime()) : plugintask.getRuntime();

            HTTPProxy usedProxy = downloadController.getUsedProxy();
            CachedAccount account = candidate.getCachedAccount();
            boolean aborted = downloadController.isAborting();
            // long duration = link.getView().getDownloadTime();

            long sizeChange = Math.max(0, link.getView().getBytesLoaded() - downloadController.getSizeBefore());
            long duration = downloadTask != null ? downloadTask.getRuntime() : 0;
            long speed = duration <= 0 ? 0 : (sizeChange * 1000) / duration;

            pluginRuntime -= userIO;
            pluginRuntime -= captcha;

            switch (result.getResult()) {
            case ACCOUNT_INVALID:
            case ACCOUNT_REQUIRED:
            case ACCOUNT_UNAVAILABLE:
            case CAPTCHA:
            case CONDITIONAL_SKIPPED:
            case CONNECTION_ISSUES:
            case CONNECTION_UNAVAILABLE:
            case FAILED:
            case FAILED_EXISTS:
            case FAILED_INCOMPLETE:
            case FATAL_ERROR:
            case FILE_UNAVAILABLE:

            case FINISHED_EXISTS:
            case HOSTER_UNAVAILABLE:
            case IP_BLOCKED:
            case OFFLINE_TRUSTED:
            case OFFLINE_UNTRUSTED:
            case PLUGIN_DEFECT:
            case PROXY_UNAVAILABLE:
            case RETRY:
            case SKIPPED:
            case STOPPED:

                break;
            case FINISHED:

                if (downloadTask != null) {
                    // we did at least download somthing
                }

            }
            //

            // dl.set
            long[] chunks = link.getView().getChunksProgress();
            if (chunks != null) {
                dl.setChunks(chunks.length);
            }

            dl.setResume(downloadController.isResumed());
            dl.setCanceled(aborted);
            dl.setHost(account.getHost());
            dl.setAccount(account.getAccount() == null ? null : account.getPlugin().getHost());
            dl.setCaptchaRuntime(captcha);
            dl.setFilesize(Math.max(0, link.getView().getBytesTotal()));
            dl.setPluginRuntime(pluginRuntime);
            dl.setProxy(usedProxy != null && !usedProxy.isDirect() && !usedProxy.isNone());
            dl.setResult(result.getResult());
            dl.setSpeed(speed);
            dl.setWaittime(waittime);
            dl.setRevision(candidate.getCachedAccount().getPlugin().getVersion());
            dl.setOs(CrossSystem.getOSFamily().name());
            dl.setUtcOffset(TimeZone.getDefault().getOffset(System.currentTimeMillis()));
            dl.setErrorID(result.getErrorID());
            dl.setTimestamp(System.currentTimeMillis());
            // this linkid is only unique for you. it is not globaly unique, thus it cannot be mapped to the actual url or anything like
            // this.
            dl.setLinkID(link.getUniqueID().getID());
            String id = dl.getErrorID() + "_" + dl.getHost() + "_" + dl.getAccount();
            AtomicInteger errorCounter = counterMap.get(id);
            if (errorCounter == null) {
                counterMap.put(id, errorCounter = new AtomicInteger());
            }
            dl.setCounter(errorCounter.incrementAndGet());
            ;
            // DownloadInterface instance = link.getDownloadLinkController().getDownloadInstance();
            log(dl);
        } catch (Throwable e) {
            logger.log(e);
        }

    }

    public static enum Response {
        OK,
        WAIT_5,
        KILL,
        WAIT_30;
    }

    @Override
    public void run() {
        while (true) {
            ArrayList<LogEntryWrapper> sendTo = new ArrayList<LogEntryWrapper>();
            ArrayList<AbstractLogEntry> sendRequest = new ArrayList<AbstractLogEntry>();
            Browser br = new Browser();
            try {
                while (list.size() == 0) {
                    synchronized (list) {
                        if (list.size() == 0) {
                            list.wait(10 * 60 * 1000);

                        }
                    }
                }
                retry: while (true) {
                    try {
                        synchronized (list) {
                            sendRequest.addAll(list);
                            for (AbstractLogEntry e : list) {
                                sendTo.add(new LogEntryWrapper(e, LogEntryWrapper.VERSION));
                            }
                            list.clear();
                        }
                        if (sendTo.size() > 0) {
                            logger.info("Try to send: \r\n" + JSonStorage.serializeToJson(sendRequest));
                            if (Application.isJared(null)) {
                                br.postPageRaw("http://stats.appwork.org/jcgi/plugins/push", JSonStorage.serializeToJson(sendTo));
                            } else {
                                br.postPageRaw("http://nas:81/thomas/fcgi/plugins/push", JSonStorage.serializeToJson(sendTo));
                            }

                            // br.postPageRaw("http://localhost:8888/plugins/push", JSonStorage.serializeToJson(sendTo));

                            Response response = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), new TypeRef<Response>() {
                            });
                            if (response != null) {
                                switch (response) {
                                case OK:

                                    break retry;
                                case WAIT_5:
                                    Thread.sleep(5 * 60 * 1000l);
                                case WAIT_30:
                                    Thread.sleep(30 * 60 * 1000l);
                                    break;
                                case KILL:
                                    return;

                                }
                            }
                        } else {
                            break retry;
                        }
                        System.out.println(1);
                    } catch (ConnectException e) {
                        logger.log(e);
                        logger.info("Wait and retry");
                        Thread.sleep(5 * 60 * 1000l);
                        // not sent. push back
                        synchronized (list) {
                            list.addAll(sendRequest);
                        }
                    } catch (JSonMapperException e) {
                        logger.log(e);
                        logger.info("Wait and retry");
                        Thread.sleep(5 * 60 * 1000l);
                        // not sent. push back
                        synchronized (list) {
                            list.addAll(sendRequest);
                        }
                    }
                }
            } catch (Exception e) {
                // failed. push back
                logger.log(e);
                // synchronized (list) {
                // list.addAll(sendRequest);
                // }
            }
        }
    }
}
