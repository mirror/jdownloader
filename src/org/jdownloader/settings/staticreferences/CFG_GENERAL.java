package org.jdownloader.settings.staticreferences;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.LongKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.storage.config.handler.StringListHandler;
import org.jdownloader.settings.GeneralSettings;

public class CFG_GENERAL {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(GeneralSettings.class);
    }

    // Static Mappings for interface org.jdownloader.settings.GeneralSettings
    public static final GeneralSettings                 CFG                                             = JsonConfig.create(GeneralSettings.class);
    public static final StorageHandler<GeneralSettings> SH                                              = (StorageHandler<GeneralSettings>) CFG.getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // true
    public static final BooleanKeyHandler               LINKCHECK_ENABLED                               = SH.getKeyHandler("LinkcheckEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler               AUTOADD_LINKS_AFTER_LINKCHECK                   = SH.getKeyHandler("AutoaddLinksAfterLinkcheck", BooleanKeyHandler.class);
    // [Ljava.lang.String;@2950fcc7
    /**
     * CommandLine to open a link in a browser. Use %s as wildcard for the url
     **/
    public static final StringListHandler               BROWSER_COMMAND_LINE                            = SH.getKeyHandler("BrowserCommandLine", StringListHandler.class);
    // 15000
    /**
     * Timeout for network problems
     **/
    public static final IntegerKeyHandler               NETWORK_ISSUES_TIMEOUT                          = SH.getKeyHandler("NetworkIssuesTimeout", IntegerKeyHandler.class);
    // true
    /**
     * Should JDownloader check free available diskspace before download starts?
     **/
    public static final BooleanKeyHandler               FREE_SPACE_CHECK_ENABLED                        = SH.getKeyHandler("FreeSpaceCheckEnabled", BooleanKeyHandler.class);
    // 2
    /**
     * http://jdownloader.org/knowledge/wiki/glossary/chunkload
     **/
    public static final IntegerKeyHandler               MAX_CHUNKS_PER_FILE                             = SH.getKeyHandler("MaxChunksPerFile", IntegerKeyHandler.class);
    // true
    public static final BooleanKeyHandler               AUTO_SORT_CHILDREN_ENABLED                      = SH.getKeyHandler("AutoSortChildrenEnabled", BooleanKeyHandler.class);
    // 120000
    /**
     * flush download buffers after x ms
     **/
    public static final IntegerKeyHandler               FLUSH_BUFFER_TIMEOUT                            = SH.getKeyHandler("FlushBufferTimeout", IntegerKeyHandler.class);
    // true
    /**
     * Use available Accounts?
     **/
    public static final BooleanKeyHandler               USE_AVAILABLE_ACCOUNTS                          = SH.getKeyHandler("UseAvailableAccounts", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler               AUTO_OPEN_CONTAINER_AFTER_DOWNLOAD              = SH.getKeyHandler("AutoOpenContainerAfterDownload", BooleanKeyHandler.class);
    // 1800000
    /**
     * Waittime in ms if a Download Temp Unavailable Failed
     **/
    public static final LongKeyHandler                  DOWNLOAD_TEMP_UNAVAILABLE_RETRY_WAITTIME        = SH.getKeyHandler("DownloadTempUnavailableRetryWaittime", LongKeyHandler.class);
    // 0
    /**
     * Download Speed limit in bytes.
     **/
    public static final IntegerKeyHandler               DOWNLOAD_SPEED_LIMIT                            = SH.getKeyHandler("DownloadSpeedLimit", IntegerKeyHandler.class);
    // 15
    public static final IntegerKeyHandler               MAX_PREMIUM_ICONS                               = SH.getKeyHandler("MaxPremiumIcons", IntegerKeyHandler.class);
    // ASK_FOR_DELETE
    /**
     * What Action should be performed after adding a container (DLC RSDF,METALINK,CCF,...)
     **/
    public static final EnumKeyHandler                  DELETE_CONTAINER_FILES_AFTER_ADDING_THEM_ACTION = SH.getKeyHandler("DeleteContainerFilesAfterAddingThemAction", EnumKeyHandler.class);
    // NEVER
    public static final EnumKeyHandler                  CLEANUP_AFTER_DOWNLOAD_ACTION                   = SH.getKeyHandler("CleanupAfterDownloadAction", EnumKeyHandler.class);
    // 300000
    /**
     * Waittime in ms if a Download HashCheck Failed
     **/
    public static final LongKeyHandler                  DOWNLOAD_HASH_CHECK_FAILED_RETRY_WAITTIME       = SH.getKeyHandler("DownloadHashCheckFailedRetryWaittime", LongKeyHandler.class);
    // 10
    /**
     * AutoStart Downloads will show a Countdown Dialog after Startup. Set the countdown time to 0 to remove this dialog. @see
     * showCountdownonAutoStartDownloads
     **/
    public static final IntegerKeyHandler               AUTO_START_COUNTDOWN_SECONDS                    = SH.getKeyHandler("AutoStartCountdownSeconds", IntegerKeyHandler.class);
    // true
    /**
     * Correct pathes relative to JDownloader root
     **/
    public static final BooleanKeyHandler               CONVERT_RELATIVE_PATHES_JDROOT                  = SH.getKeyHandler("ConvertRelativePathesJDRoot", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler               MAX_DOWNLOADS_PER_HOST_ENABLED                  = SH.getKeyHandler("MaxDownloadsPerHostEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler               SILENT_RESTART                                  = SH.getKeyHandler("SilentRestart", BooleanKeyHandler.class);
    // 5
    /**
     * Keep max X old lists on disk (DownloadList,Linkgrabber)
     **/
    public static final IntegerKeyHandler               KEEP_XOLD_LISTS                                 = SH.getKeyHandler("KeepXOldLists", IntegerKeyHandler.class);
    // true
    /**
     * @see AutoStartCountdownSeconds
     **/
    public static final BooleanKeyHandler               SHOW_COUNTDOWNON_AUTO_START_DOWNLOADS           = SH.getKeyHandler("ShowCountdownonAutoStartDownloads", BooleanKeyHandler.class);
    // 500
    /**
     * max buffer size for write operations in kb
     **/
    public static final IntegerKeyHandler               MAX_BUFFER_SIZE                                 = SH.getKeyHandler("MaxBufferSize", IntegerKeyHandler.class);
    // 600000
    /**
     * Waittime in ms if a Download had unknown IOException
     **/
    public static final LongKeyHandler                  DOWNLOAD_UNKNOWN_IOEXCEPTION_WAITTIME           = SH.getKeyHandler("DownloadUnknownIOExceptionWaittime", LongKeyHandler.class);
    // 3
    /**
     * How many downloads should Jdownloader download at once? Note that most hosters allow only one download at a time in freemode
     **/
    public static final IntegerKeyHandler               MAX_SIMULTANE_DOWNLOADS                         = SH.getKeyHandler("MaxSimultaneDownloads", IntegerKeyHandler.class);
    // false
    /**
     * Is true, if jdownloader got closed with running downloads.
     **/
    public static final BooleanKeyHandler               CLOSED_WITH_RUNNING_DOWNLOADS                   = SH.getKeyHandler("ClosedWithRunningDownloads", BooleanKeyHandler.class);
    // C:\Users\Thomas\downloads
    public static final StringKeyHandler                DEFAULT_DOWNLOAD_FOLDER                         = SH.getKeyHandler("DefaultDownloadFolder", StringKeyHandler.class);
    // false
    public static final BooleanKeyHandler               USE_ORIGINAL_LAST_MODIFIED                      = SH.getKeyHandler("UseOriginalLastModified", BooleanKeyHandler.class);
    // 512
    /**
     * Force Jdownloader to always keep a certain amount of MB Diskspace free
     **/
    public static final IntegerKeyHandler               FORCED_FREE_SPACE_ON_DISK                       = SH.getKeyHandler("ForcedFreeSpaceOnDisk", IntegerKeyHandler.class);
    // 10
    /**
     * Pause Speed. in Pause Mode we limit speed to this value to keep connections open, but use hardly bandwidth
     **/
    public static final IntegerKeyHandler               PAUSE_SPEED                                     = SH.getKeyHandler("PauseSpeed", IntegerKeyHandler.class);
    // 80
    /**
     * flush download buffers when filled up to x percent (1-100)
     **/
    public static final IntegerKeyHandler               FLUSH_BUFFER_LEVEL                              = SH.getKeyHandler("FlushBufferLevel", IntegerKeyHandler.class);
    // false
    public static final BooleanKeyHandler               ADD_NEW_LINKS_ON_TOP                            = SH.getKeyHandler("AddNewLinksOnTop", BooleanKeyHandler.class);
    // 3
    /**
     * How often a Plugin restarts a download if download failed
     **/
    public static final IntegerKeyHandler               MAX_PLUGIN_RETRIES                              = SH.getKeyHandler("MaxPluginRetries", IntegerKeyHandler.class);
    // ONLY_IF_EXIT_WITH_RUNNING_DOWNLOADS
    public static final EnumKeyHandler                  AUTO_START_DOWNLOAD_OPTION                      = SH.getKeyHandler("AutoStartDownloadOption", EnumKeyHandler.class);
    // false
    public static final BooleanKeyHandler               DOWNLOAD_SPEED_LIMIT_ENABLED                    = SH.getKeyHandler("DownloadSpeedLimitEnabled", BooleanKeyHandler.class);
    // null
    public static final ObjectKeyHandler                DOWNLOAD_FOLDER_HISTORY                         = SH.getKeyHandler("DownloadFolderHistory", ObjectKeyHandler.class);
    // ASK_FOR_EACH_FILE
    public static final EnumKeyHandler                  IF_FILE_EXISTS_ACTION                           = SH.getKeyHandler("IfFileExistsAction", EnumKeyHandler.class);
    // true
    /**
     * Disable this option if you do not want to see the filename in a captchadialog
     **/
    public static final BooleanKeyHandler               SHOW_FILE_NAME_IN_CAPTCHA_DIALOG_ENABLED        = SH.getKeyHandler("ShowFileNameInCaptchaDialogEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler               COPY_SINGLE_REAL_URL                            = SH.getKeyHandler("CopySingleRealURL", BooleanKeyHandler.class);
    // 1
    public static final IntegerKeyHandler               MAX_SIMULTANE_DOWNLOADS_PER_HOST                = SH.getKeyHandler("MaxSimultaneDownloadsPerHost", IntegerKeyHandler.class);
    // true
    public static final BooleanKeyHandler               CLEAN_UP_FILENAMES                              = SH.getKeyHandler("CleanUpFilenames", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler               FILTER_REGEX                                    = SH.getKeyHandler("FilterRegex", BooleanKeyHandler.class);
    // 300000
    /**
     * Penaltytime before a retry if JDownloader lost connection
     **/
    public static final IntegerKeyHandler               WAITTIME_ON_CONNECTION_LOSS                     = SH.getKeyHandler("WaittimeOnConnectionLoss", IntegerKeyHandler.class);
    // true
    public static final BooleanKeyHandler               HASH_CHECK_ENABLED                              = SH.getKeyHandler("HashCheckEnabled", BooleanKeyHandler.class);
    // 0
    /**
     * if this value is >0, the subfolder option only will be used if the package contains more than subfolderThreshold value links
     **/
    public static final IntegerKeyHandler               SUBFOLDER_THRESHOLD                             = SH.getKeyHandler("SubfolderThreshold", IntegerKeyHandler.class);
    // true
    /**
     * If Enabled, JDownloader will save the linkgrabber list when you exit jd, and restore it on next startup
     **/
    public static final BooleanKeyHandler               SAVE_LINKGRABBER_LIST_ENABLED                   = SH.getKeyHandler("SaveLinkgrabberListEnabled", BooleanKeyHandler.class);
}