package org.jdownloader.settings;

import java.io.File;
import java.util.ArrayList;

import jd.utils.JDUtilities;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.appwork.storage.config.defaults.AbstractDefaultFactory;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;

public interface GeneralSettings extends ConfigInterface {

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
    @DefaultIntValue(10240)
    @SpinnerValidator(min = 0, max = Integer.MAX_VALUE)
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

    @Description("Is true, if jdownloader got closed with running downloads.")
    @DefaultBooleanValue(false)
    void setClosedWithRunningDownloads(boolean b);

    boolean isClosedWithRunningDownloads();

    @Description("If JDownloader got closed with running downloads, Downloads will be autostarted on next start. ")
    @DefaultBooleanValue(true)
    @AboutConfig
    void setAutoRestartDownloadsIfExitWithRunningDownloads(boolean b);

    boolean isAutoRestartDownloadsIfExitWithRunningDownloads();
}
