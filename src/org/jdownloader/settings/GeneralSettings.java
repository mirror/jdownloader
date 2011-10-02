package org.jdownloader.settings;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.KeyHandler;
import org.appwork.storage.config.StorageHandler;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.RangeValidator;

@DefaultFactory(GeneralSettingsDefaults.class)
public interface GeneralSettings extends ConfigInterface {

    public static final GeneralSettings                 CFG = JsonConfig.create(GeneralSettings.class);
    @SuppressWarnings("unchecked")
    public static final StorageHandler<GeneralSettings> SH  = (StorageHandler<GeneralSettings>) CFG.getStorageHandler();

    @AboutConfig
    void setDefaultDownloadFolder(String ddl);

    String getDefaultDownloadFolder();

    boolean isAutoStartDownloadsOnStartupEnabled();

    void setAutoStartDownloadsOnStartupEnabled(boolean b);

    @AboutConfig
    boolean isCreatePackageNameSubFolderEnabled();

    void setCreatePackageNameSubFolderEnabled(boolean b);

    @AboutConfig
    boolean isAddNewLinksOnTop();

    void setAddNewLinksOnTop(boolean selected);

    @AboutConfig
    boolean isAutoDownloadStartAfterAddingEnabled();

    void setAutoDownloadStartAfterAddingEnabled(boolean selected);

    @AboutConfig
    boolean isAutoaddLinksAfterLinkcheck();

    void setAutoaddLinksAfterLinkcheck(boolean selected);

    ArrayList<String[]> getDownloadFolderHistory();

    void setDownloadFolderHistory(ArrayList<String[]> history);

    @AboutConfig
    boolean isHashCheckEnabled();

    void setHashCheckEnabled(boolean b);

    @AboutConfig
    boolean isAutoOpenContainerAfterDownload();

    void setAutoOpenContainerAfterDownload(boolean b);

    @AboutConfig
    CleanAfterDownloadAction getCleanupAfterDownloadAction();

    void setCleanupAfterDownloadAction(CleanAfterDownloadAction action);

    @AboutConfig
    IfFileExistsAction getIfFileExistsAction();

    void setIfFileExistsAction(IfFileExistsAction action);

    @AboutConfig
    @RangeValidator(range = { 0, 100 })
    int getMaxSimultaneDownloadsPerHost();

    void setMaxSimultaneDownloadsPerHost(int num);

    @AboutConfig
    boolean isLinkcheckEnabled();

    void setLinkcheckEnabled(boolean b);

    @AboutConfig
    boolean isCleanUpFilenames();

    void setCleanUpFilenames(boolean b);

    @AboutConfig
    boolean isClickNLoadEnabled();

    void setClickNLoadEnabled(boolean b);

    @AboutConfig
    @Description("Force Jdownloader to always keep a certain amount of MB Diskspace free")
    int getForcedFreeSpaceOnDisk();

    void setForcedFreeSpaceOnDisk(int mb);

    // JSonWrapper.get("DOWNLOAD").getBooleanProperty("PARAM_DOWNLOAD_AUTORESUME_ON_RECONNECT",
    // true);
    @AboutConfig
    boolean isInterruptResumeableDownloadsEnable();

    void setInterruptResumeableDownloadsEnable(boolean b);

    // JSonWrapper.get("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN,
    // 2);
    public static final KeyHandler MAX_SIMULTANE_DOWNLOADS = CFG.getStorageHandler().getKeyHandler("MaxSimultaneDownloads");

    @AboutConfig
    @Description("How many downloads should Jdownloader download at once? Note that most hosters allow only one download at a time in freemode")
    @RangeValidator(range = { 1, 50 })
    int getMaxSimultaneDownloads();

    void setMaxSimultaneDownloads(int num);

    // JSonWrapper.get("DOWNLOAD").getBooleanProperty("PARAM_DOWNLOAD_PREFER_RECONNECT",
    // true)
    @AboutConfig
    @Description("Do not start further downloads if others are waiting for a reconnect/new ip")
    boolean isDownloadControllerPrefersReconnectEnabled();

    void setDownloadControllerPrefersReconnectEnabled(boolean b);

    // JSonWrapper.get("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED,
    // 0)
    public static final KeyHandler DOWNLOAD_SPEED_LIMIT = CFG.getStorageHandler().getKeyHandler("DownloadSpeedLimit");

    @AboutConfig
    @Description("Download Speed limit in bytes.")
    int getDownloadSpeedLimit();

    void setDownloadSpeedLimit(int bytes);

    @AboutConfig
    @Description("Pause Speed. in Pause Mode we limit speed to this value to keep connections open, but use hardly bandwidth")
    int getPauseSpeed();

    void setPauseSpeed(int kb);

    public static final KeyHandler MAX_CHUNKS_PER_FILE = CFG.getStorageHandler().getKeyHandler("MaxChunksPerFile");

    @AboutConfig
    @Description("http://jdownloader.org/knowledge/wiki/glossary/chunkload")
    int getMaxChunksPerFile();

    void setMaxChunksPerFile(int num);

    @AboutConfig
    @Description("max buffer size for each download connection in kb")
    @RangeValidator(range = { 100, 10240 })
    int getMaxBufferSize();

    void setMaxBufferSize(int num);

    @AboutConfig
    @Description("flush download buffers when filled up to x percent (1-100)")
    @RangeValidator(range = { 1, 100 })
    int getFlushBufferLevel();

    void setFlushBufferLevel(int level);

    @AboutConfig
    @Description("flush download buffers after x ms")
    int getFlushBufferTimeout();

    void setFlushBufferTimeout(int ms);

    @AboutConfig
    @Description("Timeout for connecting to a httpserver")
    @RangeValidator(range = { 0, 300000 })
    int getHttpConnectTimeout();

    void setHttpConnectTimeout(int seconds);

    @AboutConfig
    @Description("Timeout for reading to a httpserver")
    @RangeValidator(range = { 0, 300000 })
    int getHttpReadTimeout();

    void setHttpReadTimeout(int seconds);

    @AboutConfig
    @Description("How often a Plugin restarts a download if download failed")
    int getMaxPluginRetries();

    void setMaxPluginRetries(int nums);

    @AboutConfig
    @Description("Penaltytime before a retry if JDownloader lost connection")
    long getWaittimeOnConnectionLoss();

    void setWaittimeOnConnectionLoss(long milliseconds);

    public static final KeyHandler DOWNLOAD_SPEED_LIMIT_ENABLED = CFG.getStorageHandler().getKeyHandler("DownloadSpeedLimitEnabled");

    @AboutConfig
    boolean isDownloadSpeedLimitEnabled();

    void setDownloadSpeedLimitEnabled(boolean b);
}
