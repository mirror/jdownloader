package org.jdownloader.statistics;

import java.awt.Dialog.ModalityType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import jd.SecondLevelLaunch;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.controlling.AccountUpOrDowngradeEvent;
import jd.controlling.downloadcontroller.AccountCache.CachedAccount;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult.RESULT;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogProperty;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
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
import org.appwork.utils.Exceptions;
import org.appwork.utils.Files;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSink;
import org.appwork.utils.logging2.LogSink.FLUSH;
import org.appwork.utils.logging2.LogSinkFileHandler;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.logging2.LogSourceProvider;
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
import org.jdownloader.jdserv.JDServUtils;
import org.jdownloader.jdserv.stats.StatsManagerConfigV2;
import org.jdownloader.logging.LogController;
import org.jdownloader.myjdownloader.client.json.AbstractJsonData;
import org.jdownloader.plugins.PluginTaskID;
import org.jdownloader.plugins.tasks.PluginSubTask;
import org.jdownloader.settings.AccountSettings;
import org.jdownloader.updatev2.UpdateController;

public class StatsManager implements GenericConfigEventListener<Object>, DownloadWatchdogListener, Runnable {
    public static final String        LID                          = "lid";
    public static final String        SLV                          = "slv";
    public static final String        SLID                         = "slid";
    public static final String        IMPORTED_TIMESTAMP           = "im";
    public static final String        FIRE_STATS_CALL2             = "fireStatsCall";
    public static final String        ADDED                        = "added";
    public static final String        ACCOUNT_PSEUDO_ID            = "uid";
    public static final String        CLICK_SOURCE                 = "cs";
    public static final String        ACCOUNT_ADDED_TIME           = "at";
    public static final String        ACCOUNTINSTANCE_CREATED_TIME = "it";
    public static final String        REGISTERED_TIME              = "rt";
    public static final String        CHECK_TIME                   = "ct";
    public static final String        EXPIRE_TIME                  = "et";
    public static final String        UPGRADE_TIME                 = "ut";
    public static final String        UPGRADE_PREMIUM              = "up";
    public static final String        UPGRADE_EXTENDED             = "ue";
    public static final String        UPGRADE_UNLIMITED            = "uu";
    private static final StatsManager INSTANCE                     = new StatsManager();
    public static final int           STACKTRACE_VERSION           = 2;

    /**
     * get the only existing instance of StatsManager. This is a singleton
     *
     * @return
     */
    public static StatsManager I() {
        return StatsManager.INSTANCE;
    }

    private final StatsManagerConfigV2                  config;
    private final LogSource                             logger;
    private final LinkedList<StatsLogInterface>         list          = new LinkedList<StatsLogInterface>();
    private final Thread                                thread;
    private final HashMap<String, AtomicInteger>        counterMap    = new HashMap<String, AtomicInteger>();
    private final long                                  sessionStart;
    private final File                                  reducerFile;
    private HashMap<String, Integer>                    reducerRandomMap;
    private final boolean                               isJared       = Application.isJared(null);
    private final int                                   minDelay      = 30;
    private final int                                   maxDelay      = 90;
    private final org.appwork.scheduler.DelayedRunnable delayedNotify = new DelayedRunnable(minDelay, maxDelay) {
        @Override
        public void delayedrun() {
            synchronized (list) {
                if (list.size() > 0) {
                    list.notifyAll();
                }
            }
        }
    };
    private final int                                   maxSendSize   = 20;
    private final int                                   maxListSize   = maxSendSize * 25;

    private boolean log(final StatsLogInterface dl) {
        if (isEnabled()) {
            synchronized (list) {
                if (list.size() == maxListSize) {
                    list.pollFirst();
                }
                list.add(dl);
                if (list.size() >= maxListSize / 2) {
                    // instant notification
                    list.notifyAll();
                } else {
                    // delayed notification
                    delayedNotify.resetAndStart();
                }
            }
            return true;
        }
        return false;
    }

    private final Number revision;

    /**
     * Create a new instance of StatsManager. This is a singleton class. Access the only existing instance by using {@link #link()}.
     */
     private StatsManager() {
         config = JsonConfig.create(StatsManagerConfigV2.class);
         if (config.isDebugEnabled()) {
             logger = LogController.getInstance().getLogger(StatsManager.class.getName());
         } else {
             logger = LogController.TRASH;
         }
         reducerFile = Application.getResource("cfg/reducer.json");
         if (reducerFile.exists()) {
             try {
                 reducerRandomMap = JSonStorage.restoreFromString(IO.readFileToString(reducerFile), TypeRef.HASHMAP_INTEGER);
             } catch (Throwable e) {
                 logger.log(e);
             }
         }
         if (reducerRandomMap == null) {
             reducerRandomMap = new HashMap<String, Integer>();
         }
         Number revision = null;
         try {
             final HashMap<String, Object> map = JSonStorage.restoreFromString(IO.readFileToString(Application.getResource("build.json")), TypeRef.HASHMAP);
             final Object object = map.get("JDownloaderRevision");
             if (object instanceof Number) {
                 revision = (Number) object;
             }
         } catch (Throwable e) {
             logger.log(e);
         }
         if (revision == null) {
             this.revision = -1;
         } else {
             this.revision = revision;
         }
         config._getStorageHandler().getKeyHandler("enabled").getEventSender().addListener(this);
         thread = new Thread(this);
         thread.setDaemon(true);
         thread.setName("StatsSender");
         thread.start();
         sessionStart = System.currentTimeMillis();
         trackR();
         SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {
             @Override
             public void run() {
                 DownloadWatchDog.getInstance().getEventSender().addListener(StatsManager.this);
                 AccountController.getInstance().getEventSender().addListener(new AccountControllerListener() {
                     @Override
                     public synchronized void onAccountControllerEvent(AccountControllerEvent event) {
                         final Account account = event.getAccount();
                         if (account != null) {
                             final String accountHoster = account.getHoster();
                             final String addedProperty = ADDED;
                             boolean isFireStatsCall = false;
                             try {
                                 final String fireStatsCallProperty = FIRE_STATS_CALL2;
                                 isFireStatsCall = event.getAccount().getBooleanProperty(fireStatsCallProperty);
                                 if (event.getType() == AccountControllerEvent.Types.ADDED || (event.getType() == AccountControllerEvent.Types.ACCOUNT_CHECKED && isFireStatsCall)) {
                                     if (account.getLongProperty(addedProperty, 0) <= 0) {
                                         account.setProperty(addedProperty, System.currentTimeMillis());
                                     }
                                     if (account.getError() != null || account.getLastValidTimestamp() <= 0) {
                                         // account is not checked yet or currently in error state. send stats later
                                         account.setProperty(fireStatsCallProperty, true);
                                     } else {
                                         try {
                                             final HashMap<String, String> infos = new HashMap<String, String>();
                                             final File file = Application.getResource("cfg/clicked/" + CrossSystem.alleviatePathParts(accountHoster) + ".json");
                                             final AccountInfo accountInfo = account.getAccountInfo();
                                             final String user = account.getUser();
                                             if (StringUtils.isNotEmpty(user)) {
                                                 infos.put(ACCOUNT_PSEUDO_ID, pseudoID(user));
                                             }
                                             final long importedTimeStamp = account.getLongProperty(IMPORTED_TIMESTAMP, -1);
                                             if (importedTimeStamp > 0) {
                                                 infos.put(IMPORTED_TIMESTAMP, Long.toString(importedTimeStamp));
                                                 infos.put(SLID, Long.toString(account.getLongProperty(SLID, -1)));
                                                 infos.put(SLV, Long.toString(account.getLongProperty(SLV, -1)));
                                             }
                                             infos.put(LID, Long.toString(JsonConfig.create(AccountSettings.class).getListID()));
                                             infos.put(REGISTERED_TIME, Long.toString(account.getRegisterTimeStamp()));
                                             infos.put(ACCOUNTINSTANCE_CREATED_TIME, Long.toString(account.getId().getID()));
                                             infos.put(ACCOUNT_ADDED_TIME, Long.toString(account.getLongProperty(addedProperty, System.currentTimeMillis())));
                                             if (file.exists()) {
                                                 try {
                                                     final ArrayList<ClickedAffLinkStorable> list = JSonStorage.restoreFromString(IO.readFileToString(file), new TypeRef<ArrayList<ClickedAffLinkStorable>>() {
                                                     });
                                                     if (list != null && list.size() > 0) {
                                                         infos.put(CLICK_SOURCE, JSonStorage.serializeToJson(list));
                                                     }
                                                 } catch (Throwable e) {
                                                     StatsManager.I().track("premium/affTrackError/" + accountHoster + "/" + e.getMessage());
                                                     logger.log(e);
                                                     file.delete();
                                                 }
                                             }
                                             final String id;
                                             if (accountInfo != null) {
                                                 final long validUntilTimeStamp = accountInfo.getValidUntil();
                                                 final long expireInMs = validUntilTimeStamp - System.currentTimeMillis();
                                                 if (validUntilTimeStamp > 0) {
                                                     infos.put(EXPIRE_TIME, Long.toString(expireInMs));
                                                     if (expireInMs > 0) {
                                                         id = "premium/added/" + accountHoster + "/" + account.getType() + "/until";
                                                     } else {
                                                         id = "premium/added/" + accountHoster + "/" + account.getType() + "/expired";
                                                     }
                                                 } else {
                                                     infos.put(EXPIRE_TIME, Long.toString(-1));
                                                     id = "premium/added/" + accountHoster + "/" + account.getType() + "/unlimited";
                                                 }
                                             } else {
                                                 id = "premium/added/" + accountHoster + "/" + account.getType() + "/unknown";
                                             }
                                             StatsManager.I().track(id, infos);
                                         } finally {
                                             account.removeProperty(fireStatsCallProperty);
                                         }
                                     }
                                     return;
                                 }
                             } catch (Throwable e) {
                                 StatsManager.I().track("premium/affTrackError/" + accountHoster + "/" + e.getMessage());
                                 logger.log(e);
                             }
                             try {
                                 if (event.getType() == AccountControllerEvent.Types.ACCOUNT_UP_OR_DOWNGRADE && !isFireStatsCall) {
                                     final AccountUpOrDowngradeEvent accountEvent = (AccountUpOrDowngradeEvent) event;
                                     if (accountEvent.isPremiumUpgraded() || accountEvent.isPremiumLimitedRenewal() || accountEvent.isPremiumUnlimitedRenewal()) {
                                         final HashMap<String, String> infos = new HashMap<String, String>();
                                         final String user = account.getUser();
                                         if (StringUtils.isNotEmpty(user)) {
                                             infos.put(ACCOUNT_PSEUDO_ID, pseudoID(user));
                                         }
                                         final long importedTimeStamp = account.getLongProperty(IMPORTED_TIMESTAMP, -1);
                                         if (importedTimeStamp > 0) {
                                             infos.put(IMPORTED_TIMESTAMP, Long.toString(importedTimeStamp));
                                             infos.put(SLID, Long.toString(account.getLongProperty(SLID, -1)));
                                             infos.put(SLV, Long.toString(account.getLongProperty(SLV, -1)));
                                         }
                                         infos.put(LID, Long.toString(JsonConfig.create(AccountSettings.class).getListID()));
                                         infos.put(UPGRADE_PREMIUM, accountEvent.isPremiumUpgraded() ? "1" : "0");
                                         infos.put(UPGRADE_EXTENDED, accountEvent.isPremiumLimitedRenewal() ? "1" : "0");
                                         infos.put(UPGRADE_UNLIMITED, accountEvent.isPremiumUnlimitedRenewal() ? "1" : "0");
                                         infos.put(CHECK_TIME, Long.toString(System.currentTimeMillis()));
                                         infos.put(REGISTERED_TIME, Long.toString(account.getRegisterTimeStamp()));
                                         infos.put(ACCOUNTINSTANCE_CREATED_TIME, Long.toString(account.getId().getID()));
                                         infos.put(ACCOUNT_ADDED_TIME, Long.toString(account.getLongProperty(addedProperty, System.currentTimeMillis())));
                                         final long currentValidUntilTimeStamp = accountEvent.getExpireTimeStamp();
                                         if (currentValidUntilTimeStamp > 0) {
                                             final long expireInMs = currentValidUntilTimeStamp - System.currentTimeMillis();
                                             infos.put(EXPIRE_TIME, Long.toString(expireInMs));
                                             infos.put(UPGRADE_TIME, Long.toString(accountEvent.getPremiumRenewalDuration()));
                                         } else {
                                             infos.put(EXPIRE_TIME, Long.toString(-1));
                                         }
                                         final File file = Application.getResource("cfg/clicked/" + CrossSystem.alleviatePathParts(accountHoster) + ".json");
                                         if (file.exists()) {
                                             try {
                                                 final ArrayList<ClickedAffLinkStorable> list = JSonStorage.restoreFromString(IO.readFileToString(file), new TypeRef<ArrayList<ClickedAffLinkStorable>>() {
                                                 });
                                                 if (list != null && list.size() > 0) {
                                                     infos.put(CLICK_SOURCE, JSonStorage.serializeToJson(list));
                                                 } else {
                                                     StatsManager.I().track("premium/upgradeTrackError/" + accountHoster + "/empty");
                                                 }
                                             } catch (Throwable e) {
                                                 StatsManager.I().track("premium/upgradeTrackError/" + accountHoster + "/" + e.getMessage());
                                                 logger.log(e);
                                                 file.delete();
                                             }
                                         }
                                         StatsManager.I().track("premium/upgrade/" + accountHoster, infos);
                                     }
                                 }
                             } catch (Throwable e) {
                                 StatsManager.I().track("premium/upgradeTrackError/" + accountHoster + "/" + e.getMessage());
                                 logger.log(e);
                             }
                         }
                     }

                     final private String pseudoID(String user) {
                         // this should result in ids that are not absolutly unique, but unique enough for stats reasons.
                         // it would be almost impossible to lookup and get back to the user
                         final String modifiedHash = Hash.getSHA256(user + "vyHeUnLbJI2AEHe2w7b4Mb9H7txwypmATmejYEnnmBhVJOgH47xT5daSJgo4LWXe3M2ejxssWXxv8W3q");
                         return modifiedHash.substring(0, ((2 * modifiedHash.length()) / 3));
                     }
                 });
             }
         });
         ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
             @Override
             public long getMaxDuration() {
                 return 5000;
             }

             @Override
             public void onShutdown(ShutdownRequest shutdownRequest) {
                 delayedNotify.delayedrun();
                 try {
                     boolean waitFlag = true;
                     while (waitFlag) {
                         synchronized (list) {
                             if (list.size() == 0) {
                                 waitFlag = false;
                             }
                         }
                         Thread.sleep(250);
                     }
                 } catch (InterruptedException e) {
                 }
             }
         });
     }

     private void trackR() {
         final long started = (int) System.currentTimeMillis();
         final HashMap<String, String> cvar = new HashMap<String, String>();
         cvar.put("j", Long.toString(Application.getJavaVersion()));
         cvar.put(REGISTERED_TIME, "0");
         if (!track(1000, null, "ping", cvar, CollectionName.PING)) {
             return;
         }
         new Thread("Pinger") {
             {
                 setDaemon(true);
             }

             public void run() {
                 while (true) {
                     try {
                         Thread.sleep(10 * 60 * 1000l);
                     } catch (InterruptedException e) {
                         return;
                     }
                     cvar.put(REGISTERED_TIME, (System.currentTimeMillis() - started) + "");
                     if (!track(1000, null, "ping", cvar, CollectionName.PING)) {
                         return;
                     }
                 }
             };
         }.start();
     }

     private boolean checkReducer(String path, int reducer) {
         synchronized (reducerRandomMap) {
             path += "_" + reducer;
             Integer randomValue = reducerRandomMap.get(path);
             if (randomValue == null) {
                 Random random = new Random(System.currentTimeMillis());
                 randomValue = random.nextInt(reducer);
                 reducerRandomMap.put(path, randomValue.intValue());
                 try {
                     IO.secureWrite(reducerFile, JSonStorage.serializeToJson(reducerRandomMap).getBytes("UTF-8"));
                 } catch (Throwable e) {
                     logger.log(e);
                 }
             }
             return randomValue != null && randomValue == 0;
         }
     }

     public long getSessionStart() {
         return sessionStart;
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
         final String dev = System.getProperty("statsmanager");
         if (dev != null && dev.equalsIgnoreCase("true")) {
             return true;
         }
         if (!isJared) {
             return false;
         }
         return config.isEnabled();
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

     private Pattern[] buildLogPattern(ErrorDetails errorDetails) {
         if (errorDetails != null && errorDetails._getLogName() != null) {
             return new Pattern[] { Pattern.compile("^" + Pattern.quote(errorDetails._getLogName()) + ".*") };
         }
         return null;
     }

     private synchronized void createAndUploadLog(final PostAction action, final boolean silent, final Pattern... includeOnly) {
         /*
          * this is our current logfolder, flush it before we can upload it
          */
         final LogFolder lf = new LogFolder(LogController.getInstance().getLogFolder(), LogController.getInstance().getInitTime());
         final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy HH.mm.ss", Locale.GERMANY);
         // return .format(date);
         lf.setNeedsFlush(true);
         final File zip = Application.getTempResource("logs/logPackage_" + System.currentTimeMillis() + ".zip");
         zip.delete();
         zip.getParentFile().mkdirs();
         ZipIOWriter writer = null;
         final String name = lf.getFolder().getName() + "-" + df.format(new Date(lf.getCreated())) + " to " + df.format(new Date(lf.getLastModified()));
         final File tmpZipFolder = Application.getTempResource("logs/" + name);
         final AtomicInteger fileCounter = new AtomicInteger(0);
         try {
             try {
                 LogSourceProvider.flushAllSinks(FLUSH.FORCE);
                 writer = new ZipIOWriter(zip) {
                     @Override
                     public void addFile(final File addFile, final boolean compress, final String fullPath) throws FileNotFoundException, ZipIOException, IOException {
                         if (addFile.getName().endsWith(".lck") || (addFile.isFile() && addFile.length() == 0)) {
                             return;
                         }
                         if (Thread.currentThread().isInterrupted()) {
                             throw new IOException("interrupted");
                         }
                         boolean addFileFlag = true;
                         if (includeOnly != null && includeOnly.length > 0) {
                             addFileFlag = false;
                             for (final Pattern include : includeOnly) {
                                 if (include.matcher(addFile.getName()).matches()) {
                                     addFileFlag = true;
                                     break;
                                 }
                             }
                         }
                         if (addFileFlag) {
                             super.addFile(addFile, compress, fullPath);
                             fileCounter.incrementAndGet();
                         }
                     }
                 };
                 if (tmpZipFolder.exists()) {
                     Files.deleteRecursiv(tmpZipFolder);
                 }
                 IO.copyFolderRecursive(lf.getFolder(), tmpZipFolder, true);
                 writer.addDirectory(tmpZipFolder, true, null);
             } finally {
                 try {
                     writer.close();
                 } catch (final Throwable e) {
                 }
             }
             if (Thread.currentThread().isInterrupted()) {
                 throw new IOException("interrupted");
             }
             if (zip.length() > 1024 * 1024 * 10) {
                 throw new Exception("Too large Filesize: " + zip.length());
             }
             if (fileCounter.get() == 0) {
                 throw new Exception("Empty LogZip!");
             }
             final String id = JDServUtils.upload(IO.readFile(zip), "ErrorID: " + action.getData(), null);
             sendLogDetails(new LogDetails(id, action.getData(), action.getCls(), action.getHost(), action.getType()));
             if (!silent) {
                 UIOManager.I().showMessageDialog(_GUI.T.StatsManager_createAndUploadLog_thanks_(action.getData()));
             }
         } catch (Exception e) {
             logger.log(e);
         } finally {
             zip.delete();
             try {
                 if (tmpZipFolder.exists()) {
                     Files.deleteRecursiv(tmpZipFolder);
                 }
             } catch (final Exception e) {
                 logger.log(e);
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

     private final ConcurrentHashMap<String, ErrorDetails> errors        = new ConcurrentHashMap<String, ErrorDetails>(10, 0.9f, 1);
     private final HashSet<String>                         requestedLogs = new HashSet<String>();

     @Override
     public void onDownloadControllerStopped(SingleDownloadController downloadController, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
         try {
             // HashResult hashResult = downloadController.getHashResult();
             // long startedAt = downloadController.getStartTimestamp();
             final DownloadLink link = downloadController.getDownloadLink();
             final DownloadLogEntry dl = new DownloadLogEntry();
             Throwable th = null;
             if (result.getResult() != null) {
                 switch (result.getResult()) {
                 case ACCOUNT_ERROR:
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
                 case CONNECTION_TEMP_UNAVAILABLE:
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
                             if (((PluginException) th).getValue() == LinkStatus.VALUE_NETWORK_IO_ERROR) {
                                 dl.setResult(DownloadResult.CONNECTION_ISSUES);
                             } else if (((PluginException) th).getValue() == LinkStatus.VALUE_LOCAL_IO_ERROR) {
                                 return;
                             }
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
                             String error = th.getMessage();
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
                 final PluginSubTask task = tasks.get(i);
                 if (task.getId() != null) {
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
                         default:
                             break;
                         }
                     }
                     if (task.getId() == PluginTaskID.DOWNLOAD) {
                         downloadTask = task;
                         break;
                     }
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
             case CONNECTION_TEMP_UNAVAILABLE:
             case FAILED:
             case FAILED_EXISTS:
             case FAILED_INCOMPLETE:
             case FATAL_ERROR:
             case FILE_UNAVAILABLE:
             case FINISHED_EXISTS:
             case HOSTER_UNAVAILABLE:
             case IP_BLOCKED:
             case OFFLINE_TRUSTED:
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
             } //
             // dl.set
             dl.setBuildTime(readBuildTime());
             dl.setResume(downloadController.isResumed());
             dl.setCanceled(aborted);
             dl.setHost(Candidate.replace(link.getHost()));
             dl.setCandidate(Candidate.create(account));
             dl.setCaptchaRuntime(captcha);
             dl.setRevU(UpdateController.getInstance().getInstalledRevisionJDU());
             dl.setRev(UpdateController.getInstance().getInstalledRevisionJD());
             dl.setFilesize(Math.max(0, link.getView().getBytesTotal()));
             dl.setPluginRuntime(pluginRuntime);
             dl.setProxy(usedProxy != null && !usedProxy.isDirect() && !usedProxy.isNone());
             dl.setSpeed(speed);
             dl.setWaittime(waittime);
             dl.setOs(CrossSystem.getOSFamily().name());
             dl.setUtcOffset(TimeZone.getDefault().getOffset(System.currentTimeMillis()));
             final String errorID = result.getErrorID();
             String stacktrace = errorID;
             if (stacktrace != null) {
                 stacktrace = "IDV" + STACKTRACE_VERSION + "\r\n" + cleanErrorID(stacktrace);
             }
             dl.setErrorID(errorID == null ? null : Hash.getMD5(stacktrace));
             dl.setTimestamp(System.currentTimeMillis());
             dl.setSessionStart(sessionStart);
             downloadController.getLogger().info("ErrorID: " + dl.getErrorID());
             // this linkid is only unique for you. it is not globaly unique, thus it cannot be mapped to the actual url or anything like
             // this.
             dl.setLinkID(link.getUniqueID().getID());
             String id = dl.getCandidate().getRevision() + "_" + dl.getErrorID() + "_" + dl.getCandidate().getClazz() + "_" + dl.getCandidate().getPlugin() + "_" + dl.getCandidate().getType();
             AtomicInteger errorCounter = counterMap.get(id);
             if (errorCounter == null) {
                 counterMap.put(id, errorCounter = new AtomicInteger());
             }
             dl.setCounter(errorCounter.incrementAndGet());
             if (dl.getErrorID() != null) {
                 ErrorDetails error = errors.get(dl.getErrorID());
                 if (error == null) {
                     final ErrorDetails error2 = errors.putIfAbsent(dl.getErrorID(), error = new ErrorDetails(dl.getErrorID()));
                     final LogInterface downloadLog = downloadController.getLogger();
                     if (downloadLog instanceof LogSource) {
                         final LogSink logSink = ((LogSource) downloadLog).getLogSink();
                         if (logSink != null) {
                             final LogSinkFileHandler fileHandler = logSink.getFileHandler();
                             if (fileHandler != null) {
                                 error._setLogName(fileHandler.getFile().getName());
                             }
                         }
                     }
                     error.setStacktrace(stacktrace);
                     error.setBuildTime(dl.getBuildTime());
                     if (error2 != null) {
                         error = error2;
                     }
                 }
             }
             logger.info("Tracker Package: \r\n" + JSonStorage.serializeToJson(dl));
             if (dl.getErrorID() != null) {
                 logger.info("Error Details: \r\n" + JSonStorage.serializeToJson(errors.get(dl.getErrorID())));
             }
             if (result.getLastPluginHost() != null && !StringUtils.equals(dl.getCandidate().getPlugin(), result.getLastPluginHost())) {
                 // the error did not happen in the plugin
                 logger.info("Do not track. " + result.getLastPluginHost() + "!=" + dl.getCandidate().getPlugin());
                 // return;
             }
             dl.setScaler(2d);
             // DownloadInterface instance = link.getDownloadLinkController().getDownloadInstance();
             log(dl);
         } catch (Throwable e) {
             logger.log(e);
         }
     }

     public static String cleanErrorID(String errorID) {
         if (errorID == null) {
             return null;
         }
         if (errorID.contains("java.lang.NumberFormatException")) {
             errorID = Pattern.compile("java.lang.NumberFormatException: For input string: \".*?\"\\s*[\r\n]{1,}", Pattern.DOTALL).matcher(errorID).replaceAll("java.lang.NumberFormatException: For input string: \"@See Log\"\r\n");
         }
         return errorID;
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

         public PostAction(ActionID id, String data, String cls, String host, String type) {
             this.id = id;
             this.data = data;
             this.cls = cls;
             this.host = host;
             this.type = type;
         }

         private String   data = null;
         private ActionID id   = null;
         private String   host;

         public String getHost() {
             return host;
         }

         public void setHost(String host) {
             this.host = host;
         }

         public String getType() {
             return type;
         }

         public void setType(String type) {
             this.type = type;
         }

         private String cls;

         public String getCls() {
             return this.cls;
         }

         public void setCls(String cls) {
             this.cls = cls;
         }

         private String type;

         public String getData() {
             return this.data;
         }

         public void setData(String data) {
             this.data = data;
         }

         public ActionID getId() {
             return this.id;
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
             final ArrayList<AbstractLogEntry> sendRequests = new ArrayList<AbstractLogEntry>();
             final ArrayList<LogEntryWrapper> logRequests = new ArrayList<LogEntryWrapper>();
             final ArrayList<AbstractTrackEntry> trackRequests = new ArrayList<AbstractTrackEntry>();
             try {
                 while (true) {
                     synchronized (list) {
                         if (list.size() == 0) {
                             list.wait(10 * 60 * 1000);
                         }
                         if (list.size() > 0) {
                             int nextSendSize = 0;
                             while (nextSendSize < maxSendSize) {
                                 final StatsLogInterface next = list.poll();
                                 if (next != null) {
                                     nextSendSize++;
                                     if (next instanceof AbstractLogEntry) {
                                         final AbstractLogEntry logEntry = (AbstractLogEntry) next;
                                         if (Math.random() < logEntry.getScaler()) {
                                             sendRequests.add(logEntry);
                                             logRequests.add(new LogEntryWrapper(logEntry, LogEntryWrapper.VERSION));
                                         }
                                     } else if (next instanceof AbstractTrackEntry) {
                                         trackRequests.add((AbstractTrackEntry) next);
                                     }
                                 } else {
                                     break;
                                 }
                             }
                             break;
                         }
                     }
                 }
                 int maxLoopRetries = 2;
                 retry: while (maxLoopRetries-- > 0) {
                     try {
                         if (trackRequests.size() > 0) {
                             final Browser br = createBrowser();
                             final Iterator<AbstractTrackEntry> it = trackRequests.iterator();
                             while (it.hasNext()) {
                                 final AbstractTrackEntry next = it.next();
                                 try {
                                     next.send(br);
                                     it.remove();
                                 } catch (Throwable e) {
                                     logger.log(e);
                                 }
                             }
                         }
                         if (logRequests.size() > 0) {
                             if (!isEnabled()) {
                                 return;
                             } else {
                                 final Browser br = createBrowser();
                                 final String responseString = br.postPageRaw(getBase() + "stats/push", Encoding.urlEncode(JSonStorage.serializeToJson(new TimeWrapper(logRequests))));
                                 if (br.getRequest().getHttpConnection().getResponseCode() == 504) {
                                     throw new IOException("504 " + br.getRequest().getHttpConnection().getResponseMessage());
                                 }
                                 if (br.getRequest().getHttpConnection().getResponseCode() == 502) {
                                     throw new IOException("502 " + br.getRequest().getHttpConnection().getResponseMessage());
                                 }
                                 // br.postPageRaw("http://localhost:8888/stats/push", JSonStorage.serializeToJson(sendTo));
                                 final Response response = JSonStorage.restoreFromString(responseString, new TypeRef<RIDWrapper<Response>>() {
                                 }).getData();
                                 if (response != null) {
                                     logRequests.clear();
                                 }
                                 switch (response.getCode()) {
                                 case OK:
                                     final PostAction[] actions = response.getActions();
                                     if (actions != null) {
                                         for (final PostAction action : actions) {
                                             if (action != null) {
                                                 switch (action.getId()) {
                                                 case REQUEST_MESSAGE:
                                                     requestMessage(action);
                                                     break;
                                                 case REQUEST_ERROR_DETAILS:
                                                     final ErrorDetails error = errors.get(action.getData());
                                                     if (error != null) {
                                                         sendErrorDetails(error);
                                                     }
                                                     break;
                                                 case REQUEST_LOG:
                                                     if (requestedLogs.add(action.getData())) {
                                                         boolean found = false;
                                                         if (action.getData() != null) {
                                                             for (AbstractLogEntry s : sendRequests) {
                                                                 if (s instanceof DownloadLogEntry) {
                                                                     if (StringUtils.equals(((DownloadLogEntry) s).getErrorID(), action.getData())) {
                                                                         final DownloadLink downloadLink = DownloadController.getInstance().getLinkByID(((DownloadLogEntry) s).getLinkID());
                                                                         if (downloadLink != null) {
                                                                             found = true;
                                                                             new Thread("Log Requestor") {
                                                                                 {
                                                                                     setDaemon(true);
                                                                                 }

                                                                                 @Override
                                                                                 public void run() {
                                                                                     final ErrorDetails errorDetails = errors.get(action.getData());
                                                                                     if (config.isAlwaysAllowLogUploads()) {
                                                                                         createAndUploadLog(action, true, buildLogPattern(errorDetails));
                                                                                     } else {
                                                                                         final UploadSessionLogDialogInterface d = UIOManager.I().show(UploadSessionLogDialogInterface.class, new UploadSessionLogDialog(action.getData(), downloadLink));
                                                                                         config.setAlwaysAllowLogUploads(d.isDontShowAgainSelected());
                                                                                         if (d.getCloseReason() == CloseReason.OK) {
                                                                                             UIOManager.I().show(ProgressInterface.class, new ProgressDialog(new ProgressGetter() {
                                                                                                 @Override
                                                                                                 public void run() throws Exception {
                                                                                                     createAndUploadLog(action, false, buildLogPattern(errorDetails));
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
                                                                                             }, 0, _GUI.T.StatsManager_run_upload_error_title(), _GUI.T.StatsManager_run_upload_error_message(), new AbstractIcon(IconKey.ICON_UPLOAD, 32)) {
                                                                                                 public java.awt.Dialog.ModalityType getModalityType() {
                                                                                                     return ModalityType.MODELESS;
                                                                                                 };
                                                                                             });
                                                                                         }
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
                                                                 {
                                                                     setDaemon(true);
                                                                 }

                                                                 @Override
                                                                 public void run() {
                                                                     final ErrorDetails errorDetails = errors.get(action.getData());
                                                                     if (config.isAlwaysAllowLogUploads()) {
                                                                         createAndUploadLog(action, true, buildLogPattern(errorDetails));
                                                                     } else {
                                                                         final UploadGeneralSessionLogDialogInterface d = UIOManager.I().show(UploadGeneralSessionLogDialogInterface.class, new UploadGeneralSessionLogDialog());
                                                                         if (d.getCloseReason() == CloseReason.OK) {
                                                                             UIOManager.I().show(ProgressInterface.class, new ProgressDialog(new ProgressGetter() {
                                                                                 @Override
                                                                                 public void run() throws Exception {
                                                                                     createAndUploadLog(action, false, buildLogPattern(errorDetails));
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
                                                                             }, 0, _GUI.T.StatsManager_run_upload_error_title(), _GUI.T.StatsManager_run_upload_error_message(), new AbstractIcon(IconKey.ICON_UPLOAD, 32)) {
                                                                                 public java.awt.Dialog.ModalityType getModalityType() {
                                                                                     return ModalityType.MODELESS;
                                                                                 };
                                                                             });
                                                                         }
                                                                     }
                                                                 }
                                                             }.start();
                                                         }
                                                     }
                                                     break;
                                                 }
                                             }
                                         }
                                     }
                                     if (trackRequests.size() == 0) {
                                         break retry;
                                     }
                                     break;
                                 case FAILED:
                                     if (trackRequests.size() == 0) {
                                         break retry;
                                     }
                                     break;
                                 case KILL:
                                     return;
                                 }
                             }
                         } else {
                             if (trackRequests.size() == 0) {
                                 break retry;
                             }
                         }
                     } catch (Throwable e) {
                         logger.log(e);
                         logger.info("Wait and retry");
                         Thread.sleep(5 * 60 * 1000l);
                     }
                 }
             } catch (Exception e) {
                 logger.log(e);
             }
         }
     }

     private void requestMessage(final PostAction action) {
         new Thread("Log Requestor") {
             @Override
             public void run() {
                 InputDialogInterface d = UIOManager.I().show(InputDialogInterface.class, new InputDialog(Dialog.STYLE_LARGE, _GUI.T.StatsManager_run_requestMessage_title(), _GUI.T.StatsManager_run_requestMessage_message(), null, null, _GUI.T.lit_send(), null));
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
         final Browser br = new Browser();
         final int[] codes = new int[999];
         for (int i = 0; i < codes.length; i++) {
             codes[i] = i;
         }
         br.setAllowedResponseCodes(codes);
         return br;
     }

     private void sendLogDetails(LogDetails log) throws StorageException, IOException {
         final Browser br = createBrowser();
         br.postPageRaw(getBase() + "stats/sendLog", Encoding.urlEncode(JSonStorage.serializeToJson(log)));
     }

     private void sendErrorDetails(ErrorDetails error) throws StorageException, IOException {
         final Browser br = createBrowser();
         br.postPageRaw(getBase() + "stats/sendError", Encoding.urlEncode(JSonStorage.serializeToJson(error)));
     }

     private String getBase() {
         if (!Application.isJared(null) && false) {
             return "http://localhost:8888/";
         }
         if (!Application.isJared(null) && false) {
             return "http://192.168.2.250:81/thomas/fcgi/";
         }
         return "http://stats.appwork.org/jcgi/";
     }

     @Override
     public void onDownloadWatchDogPropertyChange(DownloadWatchDogProperty propertyChange) {
     }

     public static long readBuildTime() {
         try {
             final HashMap<String, Object> map = JSonStorage.restoreFromString(IO.readFileToString(Application.getResource("build.json")), TypeRef.HASHMAP);
             return readBuildTime(map);
         } catch (Throwable e) {
             return 0;
         }
     }

     public static long readBuildTime(HashMap<String, Object> map) {
         try {
             final Object ret = map.get("buildTimestamp");
             if (ret instanceof Number) {
                 return ((Number) ret).longValue();
             }
             return Long.parseLong(ret + "");
         } catch (Throwable e) {
             return 0;
         }
     }

     protected void sendMessage(String text, PostAction action) throws StorageException, IOException {
         final Browser br = createBrowser();
         br.postPageRaw(getBase() + "stats/sendMessage", Encoding.urlEncode(JSonStorage.serializeToJson(new MessageData(text, action.getData()))));
     }

     public static enum CollectionName {
         BASIC,
         RECAPTCHA,
         SECURITY,
         PING,
         PLUGINS,
         CAPTCHA,
         PJS
     }

     public boolean track(final int reducer, String reducerID, final String id, final Map<String, String> infos, final CollectionName col) {
         final String reducerKey;
         if (reducer > 1) {
             if (StringUtils.isEmpty(reducerID)) {
                 reducerKey = id + "_in" + reducer;
             } else {
                 reducerKey = reducerID + "_in" + reducer;
             }
             synchronized (reducerRandomMap) {
                 final Integer randomValue = reducerRandomMap.get(reducerKey);
                 if (randomValue != null) {
                     if (randomValue.intValue() != 0) {
                         return false;
                     }
                 }
             }
         } else {
             reducerKey = null;
         }
         return log(new AbstractTrackEntry() {
             @Override
             public void send(Browser br) throws IOException {
                 String path = id;
                 if (reducer > 1) {
                     path = id + "_in" + reducer;
                     synchronized (reducerRandomMap) {
                         Integer randomValue = reducerRandomMap.get(reducerKey);
                         if (randomValue == null) {
                             final Random random = new Random(System.currentTimeMillis());
                             randomValue = random.nextInt(reducer);
                             reducerRandomMap.put(reducerKey, randomValue.intValue());
                             try {
                                 IO.secureWrite(reducerFile, JSonStorage.serializeToJson(reducerRandomMap).getBytes("UTF-8"));
                             } catch (Throwable e) {
                                 logger.log(e);
                             }
                         }
                         if (randomValue.intValue() != 0) {
                             return;
                         }
                     }
                 }
                 final HashMap<String, String> cvar = new HashMap<String, String>();
                 try {
                     cvar.put("_id", "000000");
                 } catch (Throwable e1) {
                     e1.printStackTrace();
                 }
                 cvar.put("source", "jd2");
                 cvar.put("os", CrossSystem.getOS().name());
                 cvar.put("rev", revision.toString());
                 cvar.put("c", System.getProperty("user.country"));
                 cvar.put("l", System.getProperty("user.language"));
                 if (infos != null) {
                     cvar.putAll(infos);
                 }
                 final Browser browser = new Browser();
                 try {
                     URLConnectionAdapter con = null;
                     if (col == null || col == CollectionName.BASIC) {
                         con = browser.openGetConnection("http://stats.appwork.org/jcgi/event/track?" + Encoding.urlEncode(path) + "&" + Encoding.urlEncode(JSonStorage.serializeToJson(cvar)));
                     } else {
                         con = browser.openGetConnection("http://stats.appwork.org/jcgi/event/track?" + Encoding.urlEncode(path) + "&" + Encoding.urlEncode(JSonStorage.serializeToJson(cvar)) + "&null&" + col.name());
                     }
                     con.disconnect();
                 } finally {
                     try {
                         browser.getHttpConnection().disconnect();
                     } catch (final Throwable e) {
                     }
                 }
             }
         });
     }

     /**
      * use the reducer if you want to limit the tracker. 1000 means that only one out of 1000 calls will be accepted
      *
      * @param reducer
      * @param path
      */
     public void track(final int reducer, String path2) {
         track(reducer, null, path2, null, CollectionName.BASIC);
     }

     public void track(final String path) {
         track(1, null, path, null, CollectionName.BASIC);
     }

     public void track(final String path, CollectionName col) {
         track(1, null, path, null, col);
     }

     public void track(String path, HashMap<String, String> infos, CollectionName col) {
         track(1, null, path, infos, col);
     }

     public void track(final String path, final Map<String, String> infos) {
         track(1, null, path, infos, CollectionName.BASIC);
     }

     public void logDownloadException(DownloadLink link, PluginForHost plugin, Throwable e) {
         try {
             DownloadLinkCandidateResult result = new DownloadLinkCandidateResult(RESULT.PLUGIN_DEFECT, e, plugin.getHost());
             DownloadLogEntry dl = new DownloadLogEntry();
             SingleDownloadController downloadController = link.getDownloadLinkController();
             if (downloadController == null) {
                 // probably accidently called from a linkchecker stack
                 return;
             }
             dl.setResult(DownloadResult.PLUGIN_DEFECT);
             HTTPProxy usedProxy = downloadController.getUsedProxy();
             boolean aborted = downloadController.isAborting();
             // long duration = link.getView().getDownloadTime();
             // long sizeChange = Math.max(0, link.getView().getBytesLoaded() - downloadController.getSizeBefore());
             dl.setBuildTime(StatsManager.readBuildTime());
             dl.setRevU(UpdateController.getInstance().getInstalledRevisionJDU());
             dl.setRev(UpdateController.getInstance().getInstalledRevisionJD());
             dl.setResume(downloadController.isResumed());
             dl.setCanceled(aborted);
             dl.setHost(Candidate.replace(link.getHost()));
             dl.setCandidate(Candidate.create(new CachedAccount(plugin.getHost(), null, plugin)));
             dl.setCaptchaRuntime(0);
             dl.setFilesize(Math.max(0, link.getView().getBytesTotal()));
             dl.setPluginRuntime(1000);
             dl.setProxy(usedProxy != null && !usedProxy.isDirect() && !usedProxy.isNone());
             dl.setSpeed(1000);
             dl.setWaittime(1000);
             dl.setOs(CrossSystem.getOSFamily().name());
             dl.setUtcOffset(TimeZone.getDefault().getOffset(System.currentTimeMillis()));
             final String errorID = result.getErrorID();
             String stacktrace = errorID;
             if (stacktrace != null) {
                 stacktrace = "IDV" + STACKTRACE_VERSION + "\r\n" + cleanErrorID(stacktrace);
             }
             dl.setErrorID(errorID == null ? null : Hash.getMD5(stacktrace));
             dl.setTimestamp(System.currentTimeMillis());
             dl.setSessionStart(StatsManager.I().getSessionStart());
             // this linkid is only unique for you. it is not globaly unique, thus it cannot be mapped to the actual url or anything like
             // this.
             dl.setLinkID(link.getUniqueID().getID());
             String id = dl.getCandidate().getRevision() + "_" + dl.getErrorID() + "_" + dl.getCandidate().getClazz() + "_" + dl.getCandidate().getPlugin() + "_" + dl.getCandidate().getType();
             AtomicInteger errorCounter = counterMap.get(id);
             if (errorCounter == null) {
                 counterMap.put(id, errorCounter = new AtomicInteger());
             }
             //
             dl.setCounter(errorCounter.incrementAndGet());
             if (dl.getErrorID() != null) {
                 ErrorDetails error = errors.get(dl.getErrorID());
                 if (error == null) {
                     ErrorDetails error2 = errors.putIfAbsent(dl.getErrorID(), error = new ErrorDetails(dl.getErrorID()));
                     final LogInterface downloadLog = plugin.getLogger();
                     if (downloadLog instanceof LogSource) {
                         final LogSink logSink = ((LogSource) downloadLog).getLogSink();
                         if (logSink != null) {
                             final LogSinkFileHandler fileHandler = logSink.getFileHandler();
                             if (fileHandler != null) {
                                 error._setLogName(fileHandler.getFile().getName());
                             }
                         }
                     }
                     error.setStacktrace(stacktrace);
                     error.setBuildTime(dl.getBuildTime());
                     if (error2 != null) {
                         error = error2;
                     }
                 }
             }
             logger.info("Tracker Package: \r\n" + JSonStorage.serializeToJson(dl));
             if (dl.getErrorID() != null) {
                 logger.info("Error Details: \r\n" + JSonStorage.serializeToJson(errors.get(dl.getErrorID())));
             }
             if (result.getLastPluginHost() != null && !StringUtils.equals(dl.getCandidate().getPlugin(), result.getLastPluginHost())) {
                 // the error did not happen in the plugin
                 logger.info("Do not track. " + result.getLastPluginHost() + "!=" + dl.getCandidate().getPlugin());
                 // return;
             }
             //
             log(dl);
         } catch (Throwable e1) {
             logger.log(e1);
         }
     }

     public void openAfflink(final PluginForHost plugin, final String customRefURL, final String source) {
         final String domain;
         if (plugin != null) {
             domain = plugin.getHost();
         } else {
             if (StringUtils.contains(customRefURL, "RedirectInterface/ul")) {
                 domain = "uploaded.to";
             } else {
                 final String host = Browser.getHost(customRefURL, false);
                 if (host != null && (host.equalsIgnoreCase("uploaded.to") || host.equalsIgnoreCase("ul.to") || host.equalsIgnoreCase("ul.net") || host.equalsIgnoreCase("uploaded.net"))) {
                     domain = "uploaded.to";
                 } else {
                     domain = host;
                 }
             }
         }
         String refURL = customRefURL;
         if (StringUtils.isEmpty(refURL) && plugin != null) {
             String buyPremium = plugin.getBuyPremiumUrl();
             if (StringUtils.isEmpty(buyPremium)) {
                 buyPremium = "http://" + plugin.getHost();
             }
             refURL = AccountController.createFullBuyPremiumUrl(buyPremium, source);
         }
         openAfflink(domain, refURL, source);
     }

     public void openAfflink(final String domain, final String refURL, final String source) {
         try {
             synchronized (this) {
                 if (refURL.startsWith("https://www.oboom.com/ref/C0ACB0?ref_token=")) {
                     StatsManager.I().track("buypremium/" + source + "/https://www.oboom.com/ref/C0ACB0?ref_token=...");
                 } else if (refURL.startsWith("http://update3.jdownloader.org/jdserv/RedirectInterface/ul")) {
                     StatsManager.I().track("buypremium/" + source + "/http://update3.jdownloader.org/jdserv/RedirectInterface/ul...");
                 } else {
                     StatsManager.I().track("buypremium/" + source + "/" + domain);
                 }
                 // do mappings here.
                 final File file = Application.getResource("cfg/clicked/" + CrossSystem.alleviatePathParts(domain) + ".json");
                 file.getParentFile().mkdirs();
                 ArrayList<ClickedAffLinkStorable> list = null;
                 if (file.exists()) {
                     try {
                         list = JSonStorage.restoreFromString(IO.readFileToString(file), new TypeRef<ArrayList<ClickedAffLinkStorable>>() {
                         });
                         // TODO CLeanup
                     } catch (Throwable e) {
                         logger.log(e);
                     }
                 }
                 if (list == null) {
                     list = new ArrayList<ClickedAffLinkStorable>();
                 }
                 // there is no reason to keep older clicks right now.
                 list.add(new ClickedAffLinkStorable(refURL, source));
                 try {
                     IO.secureWrite(file, JSonStorage.serializeToJson(list).getBytes("UTF-8"));
                 } catch (Throwable e) {
                     logger.log(e);
                     file.delete();
                 }
             }
         } finally {
             if (refURL != null) {
                 CrossSystem.openURLOrShowMessage(refURL);
             }
         }
     }

     public void trackException(int reducer, String reducerID, Throwable e, String subkey, CollectionName pjs) {
         HashMap<String, String> infos = new HashMap<String, String>();
         infos.put("stack", Exceptions.getStackTrace(e));
         StackTraceElement[] stack = e.getStackTrace();
         StackTraceElement src = stack[0];
         for (StackTraceElement el : stack) {
             if (src == stack[0] && el.getLineNumber() > 0 && StringUtils.isNotEmpty(el.getFileName())) {
                 src = el;
                 break;
             }
         }
         StatsManager.I().track(0, null, subkey + "exception/" + src.getFileName() + ":" + src.getLineNumber() + "/" + e.getMessage(), infos, pjs);
     }
}
