package org.jdownloader.settings;

import java.io.File;
import java.util.ArrayList;

import jd.utils.JDUtilities;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.appwork.storage.config.defaults.AbstractDefaultFactory;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;

public interface GeneralSettings extends ConfigInterface {
    // Static Mappings for interface org.jdownloader.settings.GeneralSettings
    public static final GeneralSettings                 CFG                                           = JsonConfig.create(GeneralSettings.class);
    public static final StorageHandler<GeneralSettings> SH                                            = (StorageHandler<GeneralSettings>) CFG.getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers,
    // access is faster, and we get an error on init if mappings are wrong.
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.httpconnecttimeout = 10000
    /**
     * Timeout for connecting to a httpserver
     **/
    public static final IntegerKeyHandler               HTTP_CONNECT_TIMEOUT                          = SH.getKeyHandler("HttpConnectTimeout", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.linkcheckenabled = true
    public static final BooleanKeyHandler               LINKCHECK_ENABLED                             = SH.getKeyHandler("LinkcheckEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.autoaddlinksafterlinkcheck =
    // false
    public static final BooleanKeyHandler               AUTOADD_LINKS_AFTER_LINKCHECK                 = SH.getKeyHandler("AutoaddLinksAfterLinkcheck", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.httpreadtimeout = 10000
    /**
     * Timeout for reading to a httpserver
     **/
    public static final IntegerKeyHandler               HTTP_READ_TIMEOUT                             = SH.getKeyHandler("HttpReadTimeout", IntegerKeyHandler.class);

    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.maxchunksperfile = 2
    /**
     * http://jdownloader.org/knowledge/wiki/glossary/chunkload
     **/
    public static final IntegerKeyHandler               MAX_CHUNKS_PER_FILE                           = SH.getKeyHandler("MaxChunksPerFile", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.flushbuffertimeout = 120000
    /**
     * flush download buffers after x ms
     **/
    public static final IntegerKeyHandler               FLUSH_BUFFER_TIMEOUT                          = SH.getKeyHandler("FlushBufferTimeout", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.downloadcontrollerprefersreconnectenabled
    // = true
    /**
     * Do not start further downloads if others are waiting for a reconnect/new
     * ip
     **/
    public static final BooleanKeyHandler               DOWNLOAD_CONTROLLER_PREFERS_RECONNECT_ENABLED = SH.getKeyHandler("DownloadControllerPrefersReconnectEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.useavailableaccounts = true
    /**
     * Use available Accounts?
     **/
    public static final BooleanKeyHandler               USE_AVAILABLE_ACCOUNTS                        = SH.getKeyHandler("UseAvailableAccounts", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.autoopencontainerafterdownload =
    // true
    public static final BooleanKeyHandler               AUTO_OPEN_CONTAINER_AFTER_DOWNLOAD            = SH.getKeyHandler("AutoOpenContainerAfterDownload", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.autodownloadstartafteraddingenabled
    // = true
    public static final BooleanKeyHandler               AUTO_DOWNLOAD_START_AFTER_ADDING_ENABLED      = SH.getKeyHandler("AutoDownloadStartAfterAddingEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.downloadspeedlimit = 0
    /**
     * Download Speed limit in bytes.
     **/
    public static final IntegerKeyHandler               DOWNLOAD_SPEED_LIMIT                          = SH.getKeyHandler("DownloadSpeedLimit", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.autoreconnectenabled = true
    /**
     * AutoReconnect enabled?
     **/
    public static final BooleanKeyHandler               AUTO_RECONNECT_ENABLED                        = SH.getKeyHandler("AutoReconnectEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.cleanupafterdownloadaction =
    // NEVER
    public static final EnumKeyHandler                  CLEANUP_AFTER_DOWNLOAD_ACTION                 = SH.getKeyHandler("CleanupAfterDownloadAction", EnumKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.interruptresumeabledownloadsenable
    // = true
    public static final BooleanKeyHandler               INTERRUPT_RESUMEABLE_DOWNLOADS_ENABLE         = SH.getKeyHandler("InterruptResumeableDownloadsEnable", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.maxdownloadsperhostenabled =
    // false
    public static final BooleanKeyHandler               MAX_DOWNLOADS_PER_HOST_ENABLED                = SH.getKeyHandler("MaxDownloadsPerHostEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.maxbuffersize = 500
    /**
     * max buffer size for write operations in kb
     **/
    public static final IntegerKeyHandler               MAX_BUFFER_SIZE                               = SH.getKeyHandler("MaxBufferSize", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.maxsimultanedownloads = 3
    /**
     * How many downloads should Jdownloader download at once? Note that most
     * hosters allow only one download at a time in freemode
     **/
    public static final IntegerKeyHandler               MAX_SIMULTANE_DOWNLOADS                       = SH.getKeyHandler("MaxSimultaneDownloads", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.defaultdownloadfolder =
    // C:\Users\Thomas\downloads
    public static final StringKeyHandler                DEFAULT_DOWNLOAD_FOLDER                       = SH.getKeyHandler("DefaultDownloadFolder", StringKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.useoriginallastmodified = false
    public static final BooleanKeyHandler               USE_ORIGINAL_LAST_MODIFIED                    = SH.getKeyHandler("UseOriginalLastModified", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.forcedfreespaceondisk = 512
    /**
     * Force Jdownloader to always keep a certain amount of MB Diskspace free
     **/
    public static final IntegerKeyHandler               FORCED_FREE_SPACE_ON_DISK                     = SH.getKeyHandler("ForcedFreeSpaceOnDisk", IntegerKeyHandler.class);
    // Keyhandler interface org.jdownloader.settings.GeneralSettings.pausespeed
    // = 10
    /**
     * Pause Speed. in Pause Mode we limit speed to this value to keep
     * connections open, but use hardly bandwidth
     **/
    public static final IntegerKeyHandler               PAUSE_SPEED                                   = SH.getKeyHandler("PauseSpeed", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.flushbufferlevel = 80
    /**
     * flush download buffers when filled up to x percent (1-100)
     **/
    public static final IntegerKeyHandler               FLUSH_BUFFER_LEVEL                            = SH.getKeyHandler("FlushBufferLevel", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.addnewlinksontop = false
    public static final BooleanKeyHandler               ADD_NEW_LINKS_ON_TOP                          = SH.getKeyHandler("AddNewLinksOnTop", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.maxpluginretries = 3
    /**
     * How often a Plugin restarts a download if download failed
     **/
    public static final IntegerKeyHandler               MAX_PLUGIN_RETRIES                            = SH.getKeyHandler("MaxPluginRetries", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.downloadspeedlimitenabled =
    // false
    public static final BooleanKeyHandler               DOWNLOAD_SPEED_LIMIT_ENABLED                  = SH.getKeyHandler("DownloadSpeedLimitEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.downloadfolderhistory = null
    public static final ObjectKeyHandler                DOWNLOAD_FOLDER_HISTORY                       = SH.getKeyHandler("DownloadFolderHistory", ObjectKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.autostartdownloadsonstartupenabled
    // = false
    public static final BooleanKeyHandler               AUTO_START_DOWNLOADS_ON_STARTUP_ENABLED       = SH.getKeyHandler("AutoStartDownloadsOnStartupEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.iffileexistsaction =
    // ASK_FOR_EACH_FILE
    public static final EnumKeyHandler                  IF_FILE_EXISTS_ACTION                         = SH.getKeyHandler("IfFileExistsAction", EnumKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.maxsimultanedownloadsperhost = 1
    public static final IntegerKeyHandler               MAX_SIMULTANE_DOWNLOADS_PER_HOST              = SH.getKeyHandler("MaxSimultaneDownloadsPerHost", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.cleanupfilenames = true
    public static final BooleanKeyHandler               CLEAN_UP_FILENAMES                            = SH.getKeyHandler("CleanUpFilenames", BooleanKeyHandler.class);
    // Keyhandler interface org.jdownloader.settings.GeneralSettings.filterregex
    // = false
    public static final BooleanKeyHandler               FILTER_REGEX                                  = SH.getKeyHandler("FilterRegex", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.waittimeonconnectionloss =
    // 300000
    /**
     * Penaltytime before a retry if JDownloader lost connection
     **/
    public static final IntegerKeyHandler               WAITTIME_ON_CONNECTION_LOSS                   = SH.getKeyHandler("WaittimeOnConnectionLoss", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.hashcheckenabled = true
    public static final BooleanKeyHandler               HASH_CHECK_ENABLED                            = SH.getKeyHandler("HashCheckEnabled", BooleanKeyHandler.class);

    class DefaultDownloadFolder extends AbstractDefaultFactory<String> {

        @Override
        public String getDefaultValue() {
            /* convert old value */
            String old = JDUtilities.getConfiguration().getStringProperty("DOWNLOAD_DIRECTORY", null);
            if (!StringUtils.isEmpty(old)) {
                File file = new File(old);
                if (file.exists() && file.isDirectory()) return old;
            }
            File home = new File(System.getProperty("user.home"));
            if (home.exists() && home.isDirectory()) {
                // new File(home, "downloads").mkdirs();
                return new File(home, "downloads").getAbsolutePath();

            } else {
                return Application.getResource("downloads").getAbsolutePath();

            }
        }

    }

    @AboutConfig
    @DefaultEnumValue("NEVER")
    CleanAfterDownloadAction getCleanupAfterDownloadAction();

    @DefaultFactory(DefaultDownloadFolder.class)
    String getDefaultDownloadFolder();

    ArrayList<String[]> getDownloadFolderHistory();

    @AboutConfig
    @Description("Download Speed limit in bytes.")
    @SpinnerValidator(min = 0, max = Integer.MAX_VALUE)
    int getDownloadSpeedLimit();

    @AboutConfig
    @Description("flush download buffers when filled up to x percent (1-100)")
    @DefaultIntValue(80)
    @SpinnerValidator(min = 1, max = 100)
    int getFlushBufferLevel();

    @AboutConfig
    @Description("flush download buffers after x ms")
    @DefaultIntValue(2 * 60 * 1000)
    int getFlushBufferTimeout();

    @AboutConfig
    @Description("Force Jdownloader to always keep a certain amount of MB Diskspace free")
    @DefaultIntValue(512)
    int getForcedFreeSpaceOnDisk();

    @AboutConfig
    @Description("Timeout for connecting to a httpserver")
    @SpinnerValidator(min = 0, max = 300000)
    @DefaultIntValue(10000)
    @RequiresRestart
    int getHttpConnectTimeout();

    @AboutConfig
    @Description("Timeout for reading to a httpserver")
    @SpinnerValidator(min = 0, max = 300000)
    @DefaultIntValue(10000)
    @RequiresRestart
    int getHttpReadTimeout();

    @AboutConfig
    @DefaultEnumValue("ASK_FOR_EACH_FILE")
    IfFileExistsAction getIfFileExistsAction();

    @AboutConfig
    @Description("max buffer size for write operations in kb")
    @SpinnerValidator(min = 100, max = 10240)
    @DefaultIntValue(500)
    int getMaxBufferSize();

    @AboutConfig
    @Description("http://jdownloader.org/knowledge/wiki/glossary/chunkload")
    @SpinnerValidator(min = 1, max = 20)
    @DefaultIntValue(1)
    int getMaxChunksPerFile();

    @AboutConfig
    @Description("How often a Plugin restarts a download if download failed")
    @DefaultIntValue(3)
    int getMaxPluginRetries();

    @AboutConfig
    @Description("How many downloads should Jdownloader download at once? Note that most hosters allow only one download at a time in freemode")
    @SpinnerValidator(min = 1, max = 20)
    @DefaultIntValue(3)
    int getMaxSimultaneDownloads();

    @AboutConfig
    @DefaultIntValue(1)
    @SpinnerValidator(min = 1, max = 100)
    int getMaxSimultaneDownloadsPerHost();

    @AboutConfig
    @Description("Pause Speed. in Pause Mode we limit speed to this value to keep connections open, but use hardly bandwidth")
    @DefaultIntValue(10)
    int getPauseSpeed();

    @AboutConfig
    @Description("Penaltytime before a retry if JDownloader lost connection")
    @DefaultIntValue(5 * 60 * 1000)
    int getWaittimeOnConnectionLoss();

    @AboutConfig
    boolean isAddNewLinksOnTop();

    @AboutConfig
    boolean isAutoaddLinksAfterLinkcheck();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isAutoDownloadStartAfterAddingEnabled();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isAutoOpenContainerAfterDownload();

    @AboutConfig
    @DefaultBooleanValue(true)
    @Description("AutoReconnect enabled?")
    boolean isAutoReconnectEnabled();

    boolean isAutoStartDownloadsOnStartupEnabled();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isCleanUpFilenames();

    @AboutConfig
    @Description("Do not start further downloads if others are waiting for a reconnect/new ip")
    @DefaultBooleanValue(true)
    boolean isDownloadControllerPrefersReconnectEnabled();

    @AboutConfig
    boolean isDownloadSpeedLimitEnabled();

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isFilterRegex();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isHashCheckEnabled();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isInterruptResumeableDownloadsEnable();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isLinkcheckEnabled();

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isMaxDownloadsPerHostEnabled();

    @AboutConfig
    @DefaultBooleanValue(true)
    @Description("Use available Accounts?")
    boolean isUseAvailableAccounts();

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isUseOriginalLastModified();

    void setAddNewLinksOnTop(boolean selected);

    void setAutoaddLinksAfterLinkcheck(boolean selected);

    void setAutoDownloadStartAfterAddingEnabled(boolean selected);

    void setAutoOpenContainerAfterDownload(boolean b);

    void setAutoReconnectEnabled(boolean b);

    void setAutoStartDownloadsOnStartupEnabled(boolean b);

    void setCleanupAfterDownloadAction(CleanAfterDownloadAction action);

    void setCleanUpFilenames(boolean b);

    @AboutConfig
    void setDefaultDownloadFolder(String ddl);

    void setDownloadControllerPrefersReconnectEnabled(boolean b);

    void setDownloadFolderHistory(ArrayList<String[]> history);

    void setDownloadSpeedLimit(int bytes);

    void setDownloadSpeedLimitEnabled(boolean b);

    void setFilterRegex(boolean b);

    void setFlushBufferLevel(int level);

    void setFlushBufferTimeout(int ms);

    void setForcedFreeSpaceOnDisk(int mb);

    void setHashCheckEnabled(boolean b);

    void setHttpConnectTimeout(int seconds);

    void setHttpReadTimeout(int seconds);

    void setIfFileExistsAction(IfFileExistsAction action);

    void setInterruptResumeableDownloadsEnable(boolean b);

    void setLinkcheckEnabled(boolean b);

    void setMaxBufferSize(int num);

    void setMaxChunksPerFile(int num);

    void setMaxDownloadsPerHostEnabled(boolean b);

    void setMaxPluginRetries(int nums);

    void setMaxSimultaneDownloads(int num);

    void setMaxSimultaneDownloadsPerHost(int num);

    void setPauseSpeed(int kb);

    void setUseAvailableAccounts(boolean b);

    void setUseOriginalLastModified(boolean b);

    void setWaittimeOnConnectionLoss(int milliseconds);

}
