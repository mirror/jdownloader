package org.jdownloader.settings;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.appwork.storage.config.annotations.Description;
import org.jdownloader.settings.annotations.AboutConfig;
import org.jdownloader.settings.annotations.RangeValidatorMarker;

@DefaultFactory(GeneralSettingsDefaults.class)
public interface GeneralSettings extends ConfigInterface {
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
    @RangeValidatorMarker(range = { 0, 100 })
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

    @AboutConfig
    @Description("How many downloads should Jdownloader download at once? Note that most hosters allow only one download at a time in freemode")
    @RangeValidatorMarker(range = { 1, 50 })
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
    @AboutConfig
    @Description("Download Speed limit in kilobytes.")
    int getDownloadSpeedLimit();

    void setDownloadSpeedLimit(int kb);

    @AboutConfig
    @Description("Pause Speed. in Pause Mode we limit speed to this value to keep connections open, but use hardly bandwidth")
    int getPauseSpeed();

    void setPauseSpeed(int kb);

    @AboutConfig
    @Description("http://jdownloader.org/knowledge/wiki/glossary/chunkload")
    int getMaxChunksPerFile();

    void setMaxChunksPerFile(int num);

    @AboutConfig
    @Description("Max Buffersize for downloading in kb")
    int getMaxBufferSize();

    void setMaxBufferSize(int num);

    @AboutConfig
    @Description("Timeout for connecting to a httpserver")
    int getHttpConnectTimeout();

    void setHttpConnectTimeout(int seconds);

    @AboutConfig
    @Description("Timeout for reading to a httpserver")
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
}
