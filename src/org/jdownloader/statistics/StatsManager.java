package org.jdownloader.statistics;

import java.awt.Dialog.ModalityType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jd.controlling.downloadcontroller.AccountCache;
import jd.controlling.downloadcontroller.AccountCache.CachedAccount;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadSession;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogProperty;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.gui.swing.jdgui.DirectFeedback;
import jd.gui.swing.jdgui.DownloadFeedBack;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.storage.StorageException;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.uio.CloseReason;
import org.appwork.uio.InputDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.logging2.sendlogs.LogFolder;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.InputDialog;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.appwork.utils.swing.dialog.ProgressInterface;
import org.appwork.utils.zip.ZipIOException;
import org.appwork.utils.zip.ZipIOWriter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.jdserv.JD_SERV_CONSTANTS;
import org.jdownloader.jdserv.UploadInterface;
import org.jdownloader.jdserv.stats.StatsManagerConfigV2;
import org.jdownloader.logging.LogController;
import org.jdownloader.myjdownloader.client.json.AbstractJsonData;
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

    private StatsManagerConfigV2           config;

    private long                           startTime;
    private LogSource                      logger;
    private ArrayList<AbstractLogEntry>    list;
    private Thread                         thread;

    private HashMap<String, AtomicInteger> counterMap;

    private long                           sessionStart;

    private void log(AbstractLogEntry dl) {
        if (isEnabled()) {
            synchronized (list) {
                if (list.size() > 20) list.clear();
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
        config = JsonConfig.create(StatsManagerConfigV2.class);
        logger = LogController.getInstance().getLogger(StatsManager.class.getName());

        DownloadWatchDog.getInstance().getEventSender().addListener(this);
        config._getStorageHandler().getKeyHandler("enabled").getEventSender().addListener(this);
        thread = new Thread(this);
        thread.setName("StatsSender");
        thread.start();
        sessionStart = System.currentTimeMillis();
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
        return config.isEnabled() && Application.isJared(StatsManager.class);

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

    private void createAndUploadLog(PostAction action) {

        final File[] logs = Application.getResource("logs").listFiles();

        LogFolder latestLog = null;
        LogFolder currentLog = null;

        if (logs != null) {
            for (final File f : logs) {
                final String timestampString = new Regex(f.getName(), "(\\d+)_\\d\\d\\.\\d\\d").getMatch(0);
                if (timestampString != null) {
                    final long timestamp = Long.parseLong(timestampString);
                    LogFolder lf;
                    lf = new LogFolder(f, timestamp);
                    if (LogController.getInstance().getInitTime() == timestamp) {
                        /*
                         * this is our current logfolder, flush it before we can upload it
                         */

                        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy HH.mm.ss", Locale.GERMANY);
                        // return .format(date);
                        lf.setNeedsFlush(true);
                        currentLog = lf;
                        final File zip = Application.getTempResource("logs/logPackage_" + System.currentTimeMillis() + ".zip");
                        zip.delete();
                        zip.getParentFile().mkdirs();
                        ZipIOWriter writer = null;

                        final String name = lf.getFolder().getName() + "-" + df.format(new Date(lf.getCreated())) + " to " + df.format(new Date(lf.getLastModified()));
                        final File folder = Application.getTempResource("logs/" + name);
                        try {
                            try {
                                LogController.getInstance().flushSinks(true, false);
                                writer = new ZipIOWriter(zip) {
                                    @Override
                                    public void addFile(final File addFile, final boolean compress, final String fullPath) throws FileNotFoundException, ZipIOException, IOException {
                                        if (addFile.getName().endsWith(".lck") || addFile.isFile() && addFile.length() == 0) { return; }
                                        if (Thread.currentThread().isInterrupted()) { throw new WTFException("INterrupted"); }
                                        super.addFile(addFile, compress, fullPath);
                                    }
                                };

                                if (folder.exists()) {
                                    Files.deleteRecursiv(folder);
                                }
                                IO.copyFolderRecursive(lf.getFolder(), folder, true);
                                writer.addDirectory(folder, true, null);

                            } finally {
                                try {
                                    writer.close();
                                } catch (final Throwable e) {
                                }
                            }

                            if (Thread.currentThread().isInterrupted()) throw new WTFException("INterrupted");
                            String id = JD_SERV_CONSTANTS.CLIENT.create(UploadInterface.class).upload(IO.readFile(zip), "ErrorID: " + action.getData(), null);

                            zip.delete();
                            if (zip.length() > 1024 * 1024 * 10) throw new Exception("Filesize: " + zip.length());
                            sendLogDetails(new LogDetails(id, action.getData()));
                            UIOManager.I().showMessageDialog(_GUI._.StatsManager_createAndUploadLog_thanks_(action.getData()));

                        } catch (Exception e) {
                            logger.log(e);

                        }
                        return;
                    }

                }
            }
        }

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

    private ConcurrentHashMap<String, ErrorDetails> errors                = new ConcurrentHashMap<String, ErrorDetails>(10, 0.9f, 1);
    private HashSet<String>                         requestedErrorDetails = new HashSet<String>();

    @Override
    public void onDownloadControllerStopped(SingleDownloadController downloadController, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
        try {
            // HashResult hashResult = downloadController.getHashResult();
            // long startedAt = downloadController.getStartTimestamp();
            DownloadLink link = downloadController.getDownloadLink();
            DownloadLogEntry dl = new DownloadLogEntry();
            Throwable th = null;
            if (result.getResult() != null) {
                switch (result.getResult()) {
                case ACCOUNT_INVALID:
                    dl.setResult(DownloadResult.ACCOUNT_INVALID);
                    break;
                case ACCOUNT_REQUIRED:
                    dl.setResult(DownloadResult.ACCOUNT_REQUIRED);
                    break;
                case ACCOUNT_UNAVAILABLE:
                    dl.setResult(DownloadResult.ACCOUNT_UNAVAILABLE);
                    break;
                case CAPTCHA:
                    dl.setResult(DownloadResult.CAPTCHA);
                    break;
                case CONDITIONAL_SKIPPED:
                    dl.setResult(DownloadResult.CONDITIONAL_SKIPPED);
                    break;
                case CONNECTION_ISSUES:
                    dl.setResult(DownloadResult.CONNECTION_ISSUES);

                    break;
                case CONNECTION_UNAVAILABLE:
                    dl.setResult(DownloadResult.CONNECTION_UNAVAILABLE);
                    break;
                case FAILED:
                    dl.setResult(DownloadResult.FAILED);
                    break;

                case FAILED_INCOMPLETE:
                    dl.setResult(DownloadResult.FAILED_INCOMPLETE);
                    th = result.getThrowable();
                    if (th != null) {

                        if (th instanceof PluginException) {

                            // String error = ((PluginException) th).getErrorMessage();
                            if (((PluginException) th).getValue() == LinkStatus.VALUE_TIMEOUT_REACHED) {
                                dl.setResult(DownloadResult.CONNECTION_ISSUES);
                            } else if (((PluginException) th).getValue() == LinkStatus.VALUE_LOCAL_IO_ERROR) { return; }

                        }

                    }
                    break;
                case FATAL_ERROR:
                    dl.setResult(DownloadResult.FATAL_ERROR);
                    break;
                case FILE_UNAVAILABLE:
                    dl.setResult(DownloadResult.FILE_UNAVAILABLE);
                    break;
                case FINISHED:
                    dl.setResult(DownloadResult.FINISHED);
                    break;
                case FINISHED_EXISTS:
                    dl.setResult(DownloadResult.FINISHED_EXISTS);
                    break;
                case HOSTER_UNAVAILABLE:
                    dl.setResult(DownloadResult.HOSTER_UNAVAILABLE);
                    th = result.getThrowable();
                    if (th != null) {
                        if (th instanceof PluginException) {
                            System.out.println(1);
                            String error = ((PluginException) th).getErrorMessage();
                            if (error != null && (error.contains("Reconnection") || error.contains("Waiting till new downloads can be started"))) {
                                dl.setResult(DownloadResult.IP_BLOCKED);
                            }

                        }
                    }
                    break;
                case IP_BLOCKED:
                    dl.setResult(DownloadResult.IP_BLOCKED);
                    break;
                case OFFLINE_TRUSTED:
                    dl.setResult(DownloadResult.OFFLINE_TRUSTED);
                    break;
                case OFFLINE_UNTRUSTED:
                    dl.setResult(DownloadResult.OFFLINE_UNTRUSTED);
                    break;
                case PLUGIN_DEFECT:
                    dl.setResult(DownloadResult.PLUGIN_DEFECT);
                    break;
                case PROXY_UNAVAILABLE:
                    dl.setResult(DownloadResult.PROXY_UNAVAILABLE);
                    break;
                case SKIPPED:
                    dl.setResult(DownloadResult.SKIPPED);
                    break;

                case FAILED_EXISTS:
                case RETRY:
                case STOPPED:
                    // no reason to log them
                    return;
                default:
                    dl.setResult(DownloadResult.UNKNOWN);
                }
            } else {
                dl.setResult(DownloadResult.UNKNOWN);
            }
            // long downloadTime = link.getView().getDownloadTime();
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
            HashMap<String, Object> map = JSonStorage.restoreFromString(IO.readFileToString(Application.getResource("build.json")), new TypeRef<HashMap<String, Object>>() {
            });
            try {
                dl.setBuildTime(Long.parseLong(map.get("buildTimestamp") + ""));
            } catch (Exception e) {
            }
            dl.setResume(downloadController.isResumed());
            dl.setCanceled(aborted);
            dl.setHost(link.getHost());
            dl.setCandidate(Candidate.create(account));
            dl.setCaptchaRuntime(captcha);
            dl.setFilesize(Math.max(0, link.getView().getBytesTotal()));
            dl.setPluginRuntime(pluginRuntime);
            dl.setProxy(usedProxy != null && !usedProxy.isDirect() && !usedProxy.isNone());

            dl.setSpeed(speed);
            dl.setWaittime(waittime);

            dl.setOs(CrossSystem.getOSFamily().name());
            dl.setUtcOffset(TimeZone.getDefault().getOffset(System.currentTimeMillis()));

            String errorID = result.getErrorID();
            if (errorID != null && !errorID.contains(dl.getCandidate().getPlugin())) {
                errorID = dl.getCandidate().getPlugin() + "-" + dl.getCandidate().getType() + "-" + errorID;
            }
            dl.setErrorID(result.getErrorID() == null ? null : Hash.getMD5(errorID));
            dl.setTimestamp(System.currentTimeMillis());
            dl.setSessionStart(sessionStart);
            // this linkid is only unique for you. it is not globaly unique, thus it cannot be mapped to the actual url or anything like
            // this.
            dl.setLinkID(link.getUniqueID().getID());
            String id = dl.getCandidate().getRevision() + "_" + dl.getErrorID() + "_" + dl.getCandidate().getPlugin() + "_" + dl.getCandidate().getType();
            AtomicInteger errorCounter = counterMap.get(id);
            if (errorCounter == null) {
                counterMap.put(id, errorCounter = new AtomicInteger());
            }

            //
            dl.setCounter(errorCounter.incrementAndGet());
            ;

            if (dl.getErrorID() != null) {
                ErrorDetails error = errors.get(dl.getErrorID());
                if (error == null) {
                    ErrorDetails error2 = errors.putIfAbsent(dl.getErrorID(), error = new ErrorDetails(dl.getErrorID(), result));
                    if (error2 != null) {
                        error = error2;
                    }
                }
            }
            // DownloadInterface instance = link.getDownloadLinkController().getDownloadInstance();

            log(dl);
        } catch (Throwable e) {
            logger.log(e);
        }

    }

    public static enum ActionID {

        REQUEST_LOG,
        REQUEST_ERROR_DETAILS,
        REQUEST_MESSAGE;
    }

    public static enum PushResponseCode {
        OK,
        FAILED,
        KILL;
    }

    public static class PostAction extends AbstractJsonData implements Storable {
        public PostAction(/* storable */) {

        }

        public PostAction(ActionID id, String data) {
            this.id = id;
            this.data = data;
        }

        private String   data = null;
        private ActionID id   = null;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public ActionID getId() {
            return id;
        }

        public void setId(ActionID id) {
            this.id = id;
        }

    }

    public static class Response extends AbstractJsonData implements Storable {
        public Response(PushResponseCode code) {
            this.code = code;
        }

        public PushResponseCode getCode() {
            return code;
        }

        public void setCode(PushResponseCode code) {
            this.code = code;
        }

        public Response(/* storable */) {

        }

        private PostAction[] actions = null;

        public PostAction[] getActions() {
            return actions;
        }

        public void setActions(PostAction[] actions) {
            this.actions = actions;
        }

        private PushResponseCode code = PushResponseCode.OK;

    }

    @Override
    public void run() {
        while (true) {
            ArrayList<LogEntryWrapper> sendTo = new ArrayList<LogEntryWrapper>();
            ArrayList<AbstractLogEntry> sendRequest = new ArrayList<AbstractLogEntry>();
            Browser br = createBrowser();
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
                            Thread.sleep(1 * 60 * 1000l);
                            logger.info("Try to send: \r\n" + JSonStorage.serializeToJson(sendRequest));
                            if (!config.isEnabled()) return;
                            br.postPageRaw(getBase() + "stats/push", JSonStorage.serializeToJson(new TimeWrapper(sendTo)));

                            // br.postPageRaw("http://localhost:8888/stats/push", JSonStorage.serializeToJson(sendTo));

                            Response response = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), new TypeRef<RIDWrapper<Response>>() {
                            }).getData();
                            switch (response.getCode()) {
                            case OK:
                                PostAction[] actions = response.getActions();
                                if (actions != null) {
                                    for (final PostAction action : actions) {
                                        if (action != null) {
                                            switch (action.getId()) {
                                            case REQUEST_MESSAGE:
                                                requestMessage(action);
                                                break;
                                            case REQUEST_ERROR_DETAILS:
                                                ErrorDetails error = errors.get(action.getData());
                                                if (error != null) {
                                                    sendErrorDetails(error);
                                                } else {
                                                    requestedErrorDetails.add(action.getData());
                                                }

                                                break;

                                            case REQUEST_LOG:
                                                boolean found = false;
                                                if (action.getData() != null) {
                                                    for (AbstractLogEntry s : sendRequest) {
                                                        if (s instanceof DownloadLogEntry) {
                                                            if (StringUtils.equals(((DownloadLogEntry) s).getErrorID(), action.getData())) {
                                                                final DownloadLink downloadLink = DownloadController.getInstance().getLinkByID(((DownloadLogEntry) s).getLinkID());
                                                                if (downloadLink != null) {
                                                                    found = true;
                                                                    new Thread("Log Requestor") {
                                                                        @Override
                                                                        public void run() {
                                                                            UploadSessionLogDialogInterface d = UIOManager.I().show(UploadSessionLogDialogInterface.class, new UploadSessionLogDialog(action.getData(), downloadLink));
                                                                            if (d.getCloseReason() == CloseReason.OK) {
                                                                                UIOManager.I().show(ProgressInterface.class, new ProgressDialog(new ProgressGetter() {

                                                                                    @Override
                                                                                    public void run() throws Exception {
                                                                                        createAndUploadLog(action);
                                                                                    }

                                                                                    @Override
                                                                                    public String getString() {
                                                                                        return null;
                                                                                    }

                                                                                    @Override
                                                                                    public int getProgress() {
                                                                                        return -1;
                                                                                    }

                                                                                    @Override
                                                                                    public String getLabelString() {
                                                                                        return null;
                                                                                    }
                                                                                }, 0, _GUI._.StatsManager_run_upload_error_title(), _GUI._.StatsManager_run_upload_error_message(), new AbstractIcon(IconKey.ICON_UPLOAD, 32)) {
                                                                                    public java.awt.Dialog.ModalityType getModalityType() {
                                                                                        return ModalityType.MODELESS;
                                                                                    };
                                                                                });
                                                                            }
                                                                        }
                                                                    }.start();
                                                                }
                                                            }
                                                        }

                                                    }
                                                }
                                                if (!found) {
                                                    new Thread("Log Requestor") {
                                                        @Override
                                                        public void run() {
                                                            UploadGeneralSessionLogDialogInterface d = UIOManager.I().show(UploadGeneralSessionLogDialogInterface.class, new UploadGeneralSessionLogDialog());
                                                            if (d.getCloseReason() == CloseReason.OK) {
                                                                UIOManager.I().show(ProgressInterface.class, new ProgressDialog(new ProgressGetter() {

                                                                    @Override
                                                                    public void run() throws Exception {
                                                                        createAndUploadLog(action);
                                                                    }

                                                                    @Override
                                                                    public String getString() {
                                                                        return null;
                                                                    }

                                                                    @Override
                                                                    public int getProgress() {
                                                                        return -1;
                                                                    }

                                                                    @Override
                                                                    public String getLabelString() {
                                                                        return null;
                                                                    }
                                                                }, 0, _GUI._.StatsManager_run_upload_error_title(), _GUI._.StatsManager_run_upload_error_message(), new AbstractIcon(IconKey.ICON_UPLOAD, 32)) {
                                                                    public java.awt.Dialog.ModalityType getModalityType() {
                                                                        return ModalityType.MODELESS;
                                                                    };
                                                                });
                                                            }
                                                        }
                                                    }.start();
                                                    // non-error related log request
                                                }
                                                // if (StringUtils.equals(getErrorID(), action.getData())) {
                                                // StatsManager.I().sendLogs(getErrorID(),);
                                                // }

                                                break;

                                            }

                                        }
                                    }
                                }
                                break retry;
                            case FAILED:
                                break retry;
                            case KILL:
                                return;
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
                        // synchronized (list) {
                        // list.addAll(sendRequest);
                        // }
                    } catch (JSonMapperException e) {
                        logger.log(e);
                        logger.info("Wait and retry");
                        Thread.sleep(5 * 60 * 1000l);
                        // not sent. push back
                        // synchronized (list) {
                        // list.addAll(sendRequest);
                        // }
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

    private void requestMessage(final PostAction action) {
        new Thread("Log Requestor") {
            @Override
            public void run() {
                InputDialogInterface d = UIOManager.I().show(InputDialogInterface.class, new InputDialog(Dialog.STYLE_LARGE, _GUI._.StatsManager_run_requestMessage_title(), _GUI._.StatsManager_run_requestMessage_message(), null, null, _GUI._.lit_send(), null));
                if (d.getCloseReason() == CloseReason.OK) {
                    try {
                        sendMessage(d.getText(), action);
                    } catch (Exception e) {
                        logger.log(e);
                    }
                }
            }
        }.start();

    }

    public Browser createBrowser() {
        Browser br = new Browser();
        final int[] codes = new int[999];
        for (int i = 0; i < codes.length; i++) {
            codes[i] = i;
        }
        br.setAllowedResponseCodes(codes);
        return br;
    }

    private void sendLogDetails(LogDetails log) throws StorageException, IOException {
        Browser br = createBrowser();
        br.postPageRaw(getBase() + "stats/sendLog", JSonStorage.serializeToJson(log));

    }

    private void sendErrorDetails(ErrorDetails error) throws StorageException, IOException {
        Browser br = createBrowser();
        br.postPageRaw(getBase() + "stats/sendError", JSonStorage.serializeToJson(error));

    }

    private String getBase() {
        if (!Application.isJared(null) && false) return "http://localhost:8888/";
        if (!Application.isJared(null) && false) return "http://192.168.2.250:81/thomas/fcgi/";
        return "http://stats.appwork.org/jcgi/";
    }

    @Override
    public void onDownloadWatchDogPropertyChange(DownloadWatchDogProperty propertyChange) {
    }

    public void feedback(DirectFeedback feedback) {

        if (feedback instanceof DownloadFeedBack) {
            DownloadLink link = ((DownloadFeedBack) feedback).getDownloadLink();

            ArrayList<Candidate> possibleAccounts = new ArrayList<Candidate>();
            AccountCache accountCache = new DownloadSession().getAccountCache(link);
            HashSet<String> dupe = new HashSet<String>();
            for (CachedAccount s : accountCache) {
                Account acc = s.getAccount();
                if (acc != null && !acc.isEnabled()) continue;
                Candidate candidate = Candidate.create(s);
                if (dupe.add(candidate.toID())) {
                    possibleAccounts.add(candidate);
                }
            }

            DownloadFeedbackLogEntry dl = new DownloadFeedbackLogEntry();
            dl.setHost(link.getHost());
            dl.setCandidates(possibleAccounts);
            dl.setFilesize(Math.max(0, link.getView().getBytesTotal()));
            try {
                HashMap<String, Object> map = JSonStorage.restoreFromString(IO.readFileToString(Application.getResource("build.json")), new TypeRef<HashMap<String, Object>>() {
                });

                dl.setBuildTime(Long.parseLong(map.get("buildTimestamp") + ""));
            } catch (Exception e) {
            }

            dl.setOs(CrossSystem.getOSFamily().name());
            dl.setUtcOffset(TimeZone.getDefault().getOffset(System.currentTimeMillis()));
            dl.setTimestamp(System.currentTimeMillis());

            dl.setSessionStart(sessionStart);
            // this linkid is only unique for you. it is not globaly unique, thus it cannot be mapped to the actual url or anything like
            // this.
            dl.setLinkID(link.getUniqueID().getID());
            String id = dl.getLinkID() + "";
            AtomicInteger errorCounter = counterMap.get(id);
            if (errorCounter == null) {
                counterMap.put(id, errorCounter = new AtomicInteger());
            }
            dl.setCounter(errorCounter.incrementAndGet());
            sendFeedback(dl);

            UIOManager.I().showMessageDialog(_GUI._.VoteFinderWindow_runInEDT_thankyou_2());
        }

    }

    private void sendFeedback(AbstractFeedbackLogEntry dl) {
        Browser br = createBrowser();
        ArrayList<LogEntryWrapper> sendTo = new ArrayList<LogEntryWrapper>();
        sendTo.add(new LogEntryWrapper(dl, LogEntryWrapper.VERSION));
        try {
            String feedbackjson = JSonStorage.serializeToJson(new TimeWrapper(sendTo));

            br.postPageRaw(getBase() + "stats/push", feedbackjson);

            // br.postPageRaw("http://localhost:8888/stats/push", JSonStorage.serializeToJson(sendTo));

            Response response = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), new TypeRef<RIDWrapper<Response>>() {
            }).getData();
            switch (response.getCode()) {
            case FAILED:
                break;
            case KILL:
                break;

            case OK:

                PostAction[] actions = response.getActions();
                if (actions != null) {
                    for (final PostAction action : actions) {
                        try {
                            if (action != null) {
                                switch (action.getId()) {
                                case REQUEST_MESSAGE:
                                    requestMessage(action);
                                    break;
                                case REQUEST_ERROR_DETAILS:
                                    break;

                                case REQUEST_LOG:

                                    new Thread("Log Requestor") {
                                        @Override
                                        public void run() {
                                            UploadGeneralSessionLogDialogInterface d = UIOManager.I().show(UploadGeneralSessionLogDialogInterface.class, new UploadGeneralSessionLogDialog());
                                            if (d.getCloseReason() == CloseReason.OK) {
                                                UIOManager.I().show(ProgressInterface.class, new ProgressDialog(new ProgressGetter() {

                                                    @Override
                                                    public void run() throws Exception {
                                                        createAndUploadLog(action);
                                                    }

                                                    @Override
                                                    public String getString() {
                                                        return null;
                                                    }

                                                    @Override
                                                    public int getProgress() {
                                                        return -1;
                                                    }

                                                    @Override
                                                    public String getLabelString() {
                                                        return null;
                                                    }
                                                }, 0, _GUI._.StatsManager_run_upload_error_title(), _GUI._.StatsManager_run_upload_error_message(), new AbstractIcon(IconKey.ICON_UPLOAD, 32)) {
                                                    public java.awt.Dialog.ModalityType getModalityType() {
                                                        return ModalityType.MODELESS;
                                                    };
                                                });
                                            }
                                        }
                                    }.start();
                                    // non-error related log request
                                }
                                // if (StringUtils.equals(getErrorID(), action.getData())) {
                                // StatsManager.I().sendLogs(getErrorID(),);
                                // }

                            }
                        } catch (Exception e) {
                            logger.log(e);

                        }
                    }
                }

            }

        } catch (StorageException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void sendMessage(String text, PostAction action) throws StorageException, IOException {

        Browser br = createBrowser();
        br.postPageRaw(getBase() + "stats/sendMessage", JSonStorage.serializeToJson(new MessageData(text, action.getData())));

    }
}
