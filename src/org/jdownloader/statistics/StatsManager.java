package org.jdownloader.statistics;

import java.io.File;
import java.io.FileInputStream;
import java.net.ConnectException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.AccountCache.CachedAccount;
import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.plugins.download.raf.HashResult;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.Application;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.jdserv.stats.StatsManagerConfig;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.PluginTaskID;
import org.jdownloader.plugins.tasks.PluginSubTask;

public class StatsManager implements GenericConfigEventListener<Object>, DownloadWatchdogListener, Runnable {
    private static final StatsManager INSTANCE = new StatsManager();

    /**
     * get the only existing instance of StatsManager. This is a singleton
     * 
     * @return
     */
    public static StatsManager I() {
        return StatsManager.INSTANCE;
    }

    private StatsManagerConfig          config;

    private long                        startTime;
    private LogSource                   logger;
    private ArrayList<AbstractLogEntry> list;
    private Thread                      thread;

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

        return config.isEnabled() && !Application.isJared(StatsManager.class);
    }

    public static String getRandomStringHash(final String arg) {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(arg.getBytes("UTF-8"));
            md.update((byte) (Math.random() * 3));
            final byte[] digest = md.digest();
            return HexFormatter.byteArrayToHex(digest);
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getFingerprint(final File arg) {
        if (arg == null || !arg.exists() || arg.isDirectory()) { return null; }
        FileInputStream fis = null;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            // if (true) { throw new IOException("Any IOEXCeption"); }
            final byte[] b = new byte[32767];

            fis = new FileInputStream(arg);
            int n = 0;
            long total = 0;
            while ((n = fis.read(b)) >= 0 && total < 2 * 1024 * 1024) {
                if (n > 0) {
                    md.update(b, 0, n);
                }
                total += n;
            }
            // add random number. We cannot reference back from a pseudo fingerprint to a certain file this way.
            //
            md.update((byte) (Math.random() * 3));
        } catch (final Throwable e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                fis.close();
            } catch (final Throwable e) {
            }
        }
        final byte[] digest = md.digest();
        return HexFormatter.byteArrayToHex(digest);
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
            dl.setRandom(getRandomStringHash(link.getLinkID()));
            dl.setResume(downloadController.isResumed());
            dl.setCanceled(aborted);
            dl.setHost(account.getHost());
            dl.setAccount(account.getPlugin().getHost());
            dl.setCaptchaRuntime(captcha);
            dl.setFilesize(link.getView().getBytesTotal());
            dl.setPluginRuntime(pluginRuntime);
            dl.setProxy(usedProxy != null && !usedProxy.isDirect() && !usedProxy.isNone());
            dl.setResult(result.getResult());
            dl.setSpeed(speed);
            dl.setWaittime(waittime);
            System.out.println(tasks);

            // DownloadInterface instance = link.getDownloadLinkController().getDownloadInstance();
            log(dl);
        } catch (Throwable e) {

        }

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
                while (true) {
                    try {
                        synchronized (list) {
                            sendRequest.addAll(list);
                            for (AbstractLogEntry e : list) {
                                sendTo.add(new LogEntryWrapper(e));
                            }
                            list.clear();
                        }
                        if (sendTo.size() > 0) {
                            logger.info("Try to send: \r\n" + JSonStorage.serializeToJson(sendRequest));
                            br.postPageRaw("http://localhost:8888/plugins/push", JSonStorage.serializeToJson(sendTo));
                            break;
                        }
                        System.out.println(1);
                    } catch (ConnectException e) {
                        logger.log(e);
                        logger.info("Wait and retry");
                        Thread.sleep(15000);
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
