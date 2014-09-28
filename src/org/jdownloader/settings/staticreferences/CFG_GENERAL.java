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
    public static final GeneralSettings                 CFG                                                              = JsonConfig.create(GeneralSettings.class);
    public static final StorageHandler<GeneralSettings> SH                                                               = (StorageHandler<GeneralSettings>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    /**
     * Mirrordetection works caseinsensitive on filename
     **/
    public static final BooleanKeyHandler               FORCE_MIRROR_DETECTION_CASE_INSENSITIVE                          = SH.getKeyHandler("ForceMirrorDetectionCaseInsensitive", BooleanKeyHandler.class);

    public static final BooleanKeyHandler               LINKCHECK_ENABLED                                                = SH.getKeyHandler("LinkcheckEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler               AUTOADD_LINKS_AFTER_LINKCHECK                                    = SH.getKeyHandler("AutoaddLinksAfterLinkcheck", BooleanKeyHandler.class);

    /**
     * CommandLine to open a link in a browser. Use %s as wildcard for the url
     **/
    public static final StringListHandler               BROWSER_COMMAND_LINE                                             = SH.getKeyHandler("BrowserCommandLine", StringListHandler.class);

    /**
     * Timeout for network problems
     **/
    public static final IntegerKeyHandler               NETWORK_ISSUES_TIMEOUT                                           = SH.getKeyHandler("NetworkIssuesTimeout", IntegerKeyHandler.class);

    /**
     * Should JDownloader check free available diskspace before download starts?
     **/
    public static final BooleanKeyHandler               FREE_SPACE_CHECK_ENABLED                                         = SH.getKeyHandler("FreeSpaceCheckEnabled", BooleanKeyHandler.class);

    /**
     * flush download buffers after x ms
     **/
    public static final IntegerKeyHandler               FLUSH_BUFFER_TIMEOUT                                             = SH.getKeyHandler("FlushBufferTimeout", IntegerKeyHandler.class);

    /**
     * Use available Accounts?
     **/
    public static final BooleanKeyHandler               USE_AVAILABLE_ACCOUNTS                                           = SH.getKeyHandler("UseAvailableAccounts", BooleanKeyHandler.class);

    /**
     * How many downloads more than getMaxSimultaneDownloads should JDownloader download at once when forced?
     **/
    public static final IntegerKeyHandler               MAX_FORCED_DOWNLOADS                                             = SH.getKeyHandler("MaxForcedDownloads", IntegerKeyHandler.class);

    /**
     * Waittime in ms if a Download Temp Unavailable Failed
     **/
    public static final LongKeyHandler                  DOWNLOAD_TEMP_UNAVAILABLE_RETRY_WAITTIME                         = SH.getKeyHandler("DownloadTempUnavailableRetryWaittime", LongKeyHandler.class);

    /**
     * If >0, JD will start additional downloads when total speed is below this value
     **/
    public static final IntegerKeyHandler               AUTO_MAX_DOWNLOADS_SPEED_LIMIT                                   = SH.getKeyHandler("AutoMaxDownloadsSpeedLimit", IntegerKeyHandler.class);

    /**
     * Set a list of crawlerplugin names to ignore
     **/
    public static final StringListHandler               CRAWLER_CRAWLER_PLUGIN_BLACKLIST                                 = SH.getKeyHandler("CrawlerCrawlerPluginBlacklist", StringListHandler.class);

    /**
     * Create subfolders after adding links? When should we create the final Downloaddirectory?
     **/
    public static final EnumKeyHandler                  CREATE_FOLDER_TRIGGER                                            = SH.getKeyHandler("CreateFolderTrigger", EnumKeyHandler.class);

    /**
     * Keep max X old lists on disk (DownloadList,Linkgrabber)
     **/
    public static final IntegerKeyHandler               KEEP_XOLD_LISTS                                                  = SH.getKeyHandler("KeepXOldLists", IntegerKeyHandler.class);

    /**
     * max buffer size for write operations in kb
     **/
    public static final IntegerKeyHandler               MAX_BUFFER_SIZE                                                  = SH.getKeyHandler("MaxBufferSize", IntegerKeyHandler.class);

    /**
     * How many downloads should Jdownloader download at once? Note that most hosters allow only one download at a time in freemode
     **/
    public static final IntegerKeyHandler               MAX_SIMULTANE_DOWNLOADS                                          = SH.getKeyHandler("MaxSimultaneDownloads", IntegerKeyHandler.class);

    public static final BooleanKeyHandler               MAX_DOWNLOADS_PER_HOST_ENABLED                                   = SH.getKeyHandler("MaxDownloadsPerHostEnabled", BooleanKeyHandler.class);

    /**
     * Is true, if jdownloader got closed with running downloads.
     **/
    public static final BooleanKeyHandler               CLOSED_WITH_RUNNING_DOWNLOADS                                    = SH.getKeyHandler("ClosedWithRunningDownloads", BooleanKeyHandler.class);

    public static final BooleanKeyHandler               USE_ORIGINAL_LAST_MODIFIED                                       = SH.getKeyHandler("UseOriginalLastModified", BooleanKeyHandler.class);

    /**
     * Force Jdownloader to always keep a certain amount of MB Diskspace free
     **/
    public static final IntegerKeyHandler               FORCED_FREE_SPACE_ON_DISK                                        = SH.getKeyHandler("ForcedFreeSpaceOnDisk", IntegerKeyHandler.class);

    /**
     * Filesize must be x equal to be a mirror. 10000 = 100%
     **/
    public static final IntegerKeyHandler               MIRROR_DETECTION_FILE_SIZE_EQUALITY                              = SH.getKeyHandler("MirrorDetectionFileSizeEquality", IntegerKeyHandler.class);

    /**
     * Pause Speed. in Pause Mode we limit speed to this value to keep connections open, but use hardly bandwidth
     **/
    public static final IntegerKeyHandler               PAUSE_SPEED                                                      = SH.getKeyHandler("PauseSpeed", IntegerKeyHandler.class);

    /**
     * http://jdownloader.org/knowledge/wiki/glossary/chunkload
     **/
    public static final IntegerKeyHandler               MAX_CHUNKS_PER_FILE                                              = SH.getKeyHandler("MaxChunksPerFile", IntegerKeyHandler.class);

    /**
     * Waittime in ms if a Download Host Unavailable Failed
     **/
    public static final LongKeyHandler                  DOWNLOAD_HOST_UNAVAILABLE_RETRY_WAITTIME                         = SH.getKeyHandler("DownloadHostUnavailableRetryWaittime", LongKeyHandler.class);

    /**
     * [ms] Define how long an account should stay disabled if a "temporarily disabled event" occures (Like Download Quota reached)
     **/
    public static final LongKeyHandler                  ACCOUNT_TEMPORARILY_DISABLED_DEFAULT_TIMEOUT                     = SH.getKeyHandler("AccountTemporarilyDisabledDefaultTimeout", LongKeyHandler.class);

    /**
     * Choose which type urls will be used for copy&paste, tables and settings.
     **/
    public static final ObjectKeyHandler                URL_ORDER                                                        = SH.getKeyHandler("UrlOrder", ObjectKeyHandler.class);

    public static final ObjectKeyHandler                DOWNLOAD_FOLDER_HISTORY                                          = SH.getKeyHandler("DownloadFolderHistory", ObjectKeyHandler.class);

    public static final EnumKeyHandler                  IF_FILE_EXISTS_ACTION                                            = SH.getKeyHandler("IfFileExistsAction", EnumKeyHandler.class);

    /**
     * How often a Plugin restarts a download if download failed
     **/
    public static final IntegerKeyHandler               MAX_PLUGIN_RETRIES                                               = SH.getKeyHandler("MaxPluginRetries", IntegerKeyHandler.class);

    /**
     * Load balance free downloads over all possible connections
     **/
    public static final BooleanKeyHandler               FREE_DOWNLOAD_LOAD_BALANCING_ENABLED                             = SH.getKeyHandler("FreeDownloadLoadBalancingEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler               DOWNLOAD_SPEED_LIMIT_ENABLED                                     = SH.getKeyHandler("DownloadSpeedLimitEnabled", BooleanKeyHandler.class);

    /**
     * @see AutoStartCountdownSeconds
     **/
    public static final BooleanKeyHandler               SHOW_COUNTDOWNON_AUTO_START_DOWNLOADS                            = SH.getKeyHandler("ShowCountdownonAutoStartDownloads", BooleanKeyHandler.class);

    /**
     * Remember Speed Limiter enabled/disabled setting after restart. Note: not associated with pause mode.
     **/
    public static final BooleanKeyHandler               DOWNLOAD_SPEED_LIMIT_REMEMBERED_ENABLED                          = SH.getKeyHandler("DownloadSpeedLimitRememberedEnabled", BooleanKeyHandler.class);

    public static final StringKeyHandler                DEFAULT_DOWNLOAD_FOLDER                                          = SH.getKeyHandler("DefaultDownloadFolder", StringKeyHandler.class);

    /**
     * If enabled, filename will be cleaned up of superfluous . and _ characters, and replaced with spaces. Please note plugins can override
     * this setting.
     **/
    public static final BooleanKeyHandler               CLEAN_UP_FILENAMES                                               = SH.getKeyHandler("CleanUpFilenames", BooleanKeyHandler.class);

    public static final IntegerKeyHandler               MAX_SIMULTANE_DOWNLOADS_PER_HOST                                 = SH.getKeyHandler("MaxSimultaneDownloadsPerHost", IntegerKeyHandler.class);

    public static final BooleanKeyHandler               COPY_SINGLE_REAL_URL                                             = SH.getKeyHandler("CopySingleRealURL", BooleanKeyHandler.class);

    /**
     * If disabled, JDownloader will only grab links that have an dedicated HostPlugin (no basic Http Links)
     **/
    public static final BooleanKeyHandler               DIRECT_HTTPCRAWLER_ENABLED                                       = SH.getKeyHandler("DirectHTTPCrawlerEnabled", BooleanKeyHandler.class);

    /**
     * Allow cleanup of existing files
     **/
    public static final BooleanKeyHandler               CLEANUP_FILE_EXISTS                                              = SH.getKeyHandler("CleanupFileExists", BooleanKeyHandler.class);

    public static final EnumKeyHandler                  MIRROR_DETECTION_DECISION                                        = SH.getKeyHandler("MirrorDetectionDecision", EnumKeyHandler.class);

    public static final EnumKeyHandler                  CLEANUP_AFTER_DOWNLOAD_ACTION                                    = SH.getKeyHandler("CleanupAfterDownloadAction", EnumKeyHandler.class);

    public static final EnumKeyHandler                  AUTO_START_DOWNLOAD_OPTION                                       = SH.getKeyHandler("AutoStartDownloadOption", EnumKeyHandler.class);

    public static final BooleanKeyHandler               DELETE_EMPTY_SUB_FOLDERS_AFTER_DELETING_DOWNLOADED_FILES_ENABLED = SH.getKeyHandler("DeleteEmptySubFoldersAfterDeletingDownloadedFilesEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler               FILTER_REGEX                                                     = SH.getKeyHandler("FilterRegex", BooleanKeyHandler.class);

    /**
     * Correct paths relative to JDownloader root
     **/
    public static final BooleanKeyHandler               CONVERT_RELATIVE_PATHS_JDROOT                                    = SH.getKeyHandler("ConvertRelativePathsJDRoot", BooleanKeyHandler.class);

    public static final BooleanKeyHandler               HASH_CHECK_ENABLED                                               = SH.getKeyHandler("HashCheckEnabled", BooleanKeyHandler.class);

    /**
     * If Enabled, JDownloader will save the linkgrabber list when you exit jd, and restore it on next startup
     **/
    public static final BooleanKeyHandler               SAVE_LINKGRABBER_LIST_ENABLED                                    = SH.getKeyHandler("SaveLinkgrabberListEnabled", BooleanKeyHandler.class);

    /**
     * if this value is >0, the subfolder option only will be used if the package contains more than subfolderThreshold value links
     **/
    public static final IntegerKeyHandler               SUBFOLDER_THRESHOLD                                              = SH.getKeyHandler("SubfolderThreshold", IntegerKeyHandler.class);

    public static final EnumKeyHandler                  ON_SKIP_DUE_TO_ALREADY_EXISTS_ACTION                             = SH.getKeyHandler("OnSkipDueToAlreadyExistsAction", EnumKeyHandler.class);

    public static final LongKeyHandler                  PROXY_HOST_BAN_TIMEOUT                                           = SH.getKeyHandler("ProxyHostBanTimeout", LongKeyHandler.class);

    public static final BooleanKeyHandler               AUTO_SORT_CHILDREN_ENABLED                                       = SH.getKeyHandler("AutoSortChildrenEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler               AUTO_OPEN_CONTAINER_AFTER_DOWNLOAD                               = SH.getKeyHandler("AutoOpenContainerAfterDownload", BooleanKeyHandler.class);

    /**
     * Download Speed limit in bytes.
     **/
    public static final IntegerKeyHandler               DOWNLOAD_SPEED_LIMIT                                             = SH.getKeyHandler("DownloadSpeedLimit", IntegerKeyHandler.class);

    /**
     * What Action should be performed after adding a container (DLC RSDF,METALINK,CCF,...)
     **/
    public static final EnumKeyHandler                  DELETE_CONTAINER_FILES_AFTER_ADDING_THEM_ACTION                  = SH.getKeyHandler("DeleteContainerFilesAfterAddingThemAction", EnumKeyHandler.class);

    /**
     * Set a list of hostplugin names to ignore
     **/
    public static final StringListHandler               CRAWLER_HOST_PLUGIN_BLACKLIST                                    = SH.getKeyHandler("CrawlerHostPluginBlacklist", StringListHandler.class);

    public static final BooleanKeyHandler               SAMBA_PREFETCH_ENABLED                                           = SH.getKeyHandler("SambaPrefetchEnabled", BooleanKeyHandler.class);

    /**
     * Waittime in ms if a Download HashCheck Failed
     **/
    public static final LongKeyHandler                  DOWNLOAD_HASH_CHECK_FAILED_RETRY_WAITTIME                        = SH.getKeyHandler("DownloadHashCheckFailedRetryWaittime", LongKeyHandler.class);

    /**
     * Setup Rules by Domain. Let us know if you use this feature and require a nicer User Interface
     **/
    public static final ObjectKeyHandler                DOMAIN_RULES                                                     = SH.getKeyHandler("DomainRules", ObjectKeyHandler.class);

    /**
     * AutoStart Downloads will show a Countdown Dialog after Startup. Set the countdown time to 0 to remove this dialog. @see
     * showCountdownonAutoStartDownloads
     **/
    public static final IntegerKeyHandler               AUTO_START_COUNTDOWN_SECONDS                                     = SH.getKeyHandler("AutoStartCountdownSeconds", IntegerKeyHandler.class);

    /**
     * If you experience tiny(betweeen 0 and 2 seconds) 'lags' when while working with JDownloader, try to disable this feature.
     **/
    public static final BooleanKeyHandler               WINDOWS_JNAIDLE_DETECTOR_ENABLED                                 = SH.getKeyHandler("WindowsJNAIdleDetectorEnabled", BooleanKeyHandler.class);

    /**
     * Mirrordetection enforces verified filesizes!
     **/
    public static final BooleanKeyHandler               FORCE_MIRROR_DETECTION_FILE_SIZE_CHECK                           = SH.getKeyHandler("ForceMirrorDetectionFileSizeCheck", BooleanKeyHandler.class);

    public static final BooleanKeyHandler               SILENT_RESTART                                                   = SH.getKeyHandler("SilentRestart", BooleanKeyHandler.class);

    /**
     * Waittime in ms if a Download had unknown IOException
     **/
    public static final LongKeyHandler                  DOWNLOAD_UNKNOWN_IOEXCEPTION_WAITTIME                            = SH.getKeyHandler("DownloadUnknownIOExceptionWaittime", LongKeyHandler.class);

    /**
     * Disable this option if you do not want to see the filename in a captchadialog
     **/
    public static final BooleanKeyHandler               SHOW_FILE_NAME_IN_CAPTCHA_DIALOG_ENABLED                         = SH.getKeyHandler("ShowFileNameInCaptchaDialogEnabled", BooleanKeyHandler.class);

    /**
     * Penaltytime before a retry if JDownloader lost connection
     **/
    public static final IntegerKeyHandler               WAITTIME_ON_CONNECTION_LOSS                                      = SH.getKeyHandler("WaittimeOnConnectionLoss", IntegerKeyHandler.class);
}