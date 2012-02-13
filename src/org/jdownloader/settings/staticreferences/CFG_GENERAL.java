package org.jdownloader.settings.staticreferences;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.jdownloader.settings.GeneralSettings;

public class CFG_GENERAL {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(GeneralSettings.class);
    }

    // Static Mappings for interface org.jdownloader.settings.GeneralSettings
    public static final GeneralSettings                 CFG                                                   = JsonConfig.create(GeneralSettings.class);
    public static final StorageHandler<GeneralSettings> SH                                                    = (StorageHandler<GeneralSettings>) CFG.getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers,
    // access is faster, and we get an error on init if mappings are wrong.
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.httpconnecttimeout = 10000
    /**
     * Timeout for connecting to a httpserver
     **/
    public static final IntegerKeyHandler               HTTP_CONNECT_TIMEOUT                                  = SH.getKeyHandler("HttpConnectTimeout", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.linkcheckenabled = true
    public static final BooleanKeyHandler               LINKCHECK_ENABLED                                     = SH.getKeyHandler("LinkcheckEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.autoaddlinksafterlinkcheck =
    // false
    public static final BooleanKeyHandler               AUTOADD_LINKS_AFTER_LINKCHECK                         = SH.getKeyHandler("AutoaddLinksAfterLinkcheck", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.httpreadtimeout = 10000
    /**
     * Timeout for reading to a httpserver
     **/
    public static final IntegerKeyHandler               HTTP_READ_TIMEOUT                                     = SH.getKeyHandler("HttpReadTimeout", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.downloadcontrollerprefersreconnectenabled
    // = true
    /**
     * Do not start further downloads if others are waiting for a reconnect/new
     * ip
     **/
    public static final BooleanKeyHandler               DOWNLOAD_CONTROLLER_PREFERS_RECONNECT_ENABLED         = SH.getKeyHandler("DownloadControllerPrefersReconnectEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.flushbuffertimeout = 120000
    /**
     * flush download buffers after x ms
     **/
    public static final IntegerKeyHandler               FLUSH_BUFFER_TIMEOUT                                  = SH.getKeyHandler("FlushBufferTimeout", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.maxchunksperfile = 1
    /**
     * http://jdownloader.org/knowledge/wiki/glossary/chunkload
     **/
    public static final IntegerKeyHandler               MAX_CHUNKS_PER_FILE                                   = SH.getKeyHandler("MaxChunksPerFile", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.useavailableaccounts = true
    /**
     * Use available Accounts?
     **/
    public static final BooleanKeyHandler               USE_AVAILABLE_ACCOUNTS                                = SH.getKeyHandler("UseAvailableAccounts", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.autoopencontainerafterdownload =
    // true
    public static final BooleanKeyHandler               AUTO_OPEN_CONTAINER_AFTER_DOWNLOAD                    = SH.getKeyHandler("AutoOpenContainerAfterDownload", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.autodownloadstartafteraddingenabled
    // = true
    public static final BooleanKeyHandler               AUTO_DOWNLOAD_START_AFTER_ADDING_ENABLED              = SH.getKeyHandler("AutoDownloadStartAfterAddingEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.downloadspeedlimit = 0
    /**
     * Download Speed limit in bytes.
     **/
    public static final IntegerKeyHandler               DOWNLOAD_SPEED_LIMIT                                  = SH.getKeyHandler("DownloadSpeedLimit", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.autoreconnectenabled = true
    /**
     * AutoReconnect enabled?
     **/
    public static final BooleanKeyHandler               AUTO_RECONNECT_ENABLED                                = SH.getKeyHandler("AutoReconnectEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.autorestartdownloadsifexitwithrunningdownloads
    // = true
    /**
     * If JDownloader got closed with running downloads, Downloads will be
     * autostarted on next start.
     **/
    public static final BooleanKeyHandler               AUTO_RESTART_DOWNLOADS_IF_EXIT_WITH_RUNNING_DOWNLOADS = SH.getKeyHandler("AutoRestartDownloadsIfExitWithRunningDownloads", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.cleanupafterdownloadaction =
    // NEVER
    public static final EnumKeyHandler                  CLEANUP_AFTER_DOWNLOAD_ACTION                         = SH.getKeyHandler("CleanupAfterDownloadAction", EnumKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.interruptresumeabledownloadsenable
    // = true
    public static final BooleanKeyHandler               INTERRUPT_RESUMEABLE_DOWNLOADS_ENABLE                 = SH.getKeyHandler("InterruptResumeableDownloadsEnable", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.maxdownloadsperhostenabled =
    // false
    public static final BooleanKeyHandler               MAX_DOWNLOADS_PER_HOST_ENABLED                        = SH.getKeyHandler("MaxDownloadsPerHostEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.maxbuffersize = 500
    /**
     * max buffer size for write operations in kb
     **/
    public static final IntegerKeyHandler               MAX_BUFFER_SIZE                                       = SH.getKeyHandler("MaxBufferSize", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.maxsimultanedownloads = 3
    /**
     * How many downloads should Jdownloader download at once? Note that most
     * hosters allow only one download at a time in freemode
     **/
    public static final IntegerKeyHandler               MAX_SIMULTANE_DOWNLOADS                               = SH.getKeyHandler("MaxSimultaneDownloads", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.closedwithrunningdownloads =
    // false
    /**
     * Is true, if jdownloader got closed with running downloads.
     **/
    public static final BooleanKeyHandler               CLOSED_WITH_RUNNING_DOWNLOADS                         = SH.getKeyHandler("ClosedWithRunningDownloads", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.defaultdownloadfolder =
    // C:\Users\thomas\downloads
    public static final StringKeyHandler                DEFAULT_DOWNLOAD_FOLDER                               = SH.getKeyHandler("DefaultDownloadFolder", StringKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.useoriginallastmodified = false
    public static final BooleanKeyHandler               USE_ORIGINAL_LAST_MODIFIED                            = SH.getKeyHandler("UseOriginalLastModified", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.forcedfreespaceondisk = 512
    /**
     * Force Jdownloader to always keep a certain amount of MB Diskspace free
     **/
    public static final IntegerKeyHandler               FORCED_FREE_SPACE_ON_DISK                             = SH.getKeyHandler("ForcedFreeSpaceOnDisk", IntegerKeyHandler.class);
    // Keyhandler interface org.jdownloader.settings.GeneralSettings.pausespeed
    // = 10240
    /**
     * Pause Speed. in Pause Mode we limit speed to this value to keep
     * connections open, but use hardly bandwidth
     **/
    public static final IntegerKeyHandler               PAUSE_SPEED                                           = SH.getKeyHandler("PauseSpeed", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.flushbufferlevel = 80
    /**
     * flush download buffers when filled up to x percent (1-100)
     **/
    public static final IntegerKeyHandler               FLUSH_BUFFER_LEVEL                                    = SH.getKeyHandler("FlushBufferLevel", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.addnewlinksontop = false
    public static final BooleanKeyHandler               ADD_NEW_LINKS_ON_TOP                                  = SH.getKeyHandler("AddNewLinksOnTop", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.downloadspeedlimitenabled =
    // false
    public static final BooleanKeyHandler               DOWNLOAD_SPEED_LIMIT_ENABLED                          = SH.getKeyHandler("DownloadSpeedLimitEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.maxpluginretries = 3
    /**
     * How often a Plugin restarts a download if download failed
     **/
    public static final IntegerKeyHandler               MAX_PLUGIN_RETRIES                                    = SH.getKeyHandler("MaxPluginRetries", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.downloadfolderhistory = null
    public static final ObjectKeyHandler                DOWNLOAD_FOLDER_HISTORY                               = SH.getKeyHandler("DownloadFolderHistory", ObjectKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.autostartdownloadsonstartupenabled
    // = false
    public static final BooleanKeyHandler               AUTO_START_DOWNLOADS_ON_STARTUP_ENABLED               = SH.getKeyHandler("AutoStartDownloadsOnStartupEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.iffileexistsaction =
    // ASK_FOR_EACH_FILE
    public static final EnumKeyHandler                  IF_FILE_EXISTS_ACTION                                 = SH.getKeyHandler("IfFileExistsAction", EnumKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.maxsimultanedownloadsperhost = 1
    public static final IntegerKeyHandler               MAX_SIMULTANE_DOWNLOADS_PER_HOST                      = SH.getKeyHandler("MaxSimultaneDownloadsPerHost", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.cleanupfilenames = true
    public static final BooleanKeyHandler               CLEAN_UP_FILENAMES                                    = SH.getKeyHandler("CleanUpFilenames", BooleanKeyHandler.class);
    // Keyhandler interface org.jdownloader.settings.GeneralSettings.filterregex
    // = false
    public static final BooleanKeyHandler               FILTER_REGEX                                          = SH.getKeyHandler("FilterRegex", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.waittimeonconnectionloss =
    // 300000
    /**
     * Penaltytime before a retry if JDownloader lost connection
     **/
    public static final IntegerKeyHandler               WAITTIME_ON_CONNECTION_LOSS                           = SH.getKeyHandler("WaittimeOnConnectionLoss", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GeneralSettings.hashcheckenabled = true
    public static final BooleanKeyHandler               HASH_CHECK_ENABLED                                    = SH.getKeyHandler("HashCheckEnabled", BooleanKeyHandler.class);
}