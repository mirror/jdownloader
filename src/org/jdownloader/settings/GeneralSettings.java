package org.jdownloader.settings;

import java.io.File;
import java.util.ArrayList;

import jd.utils.JDUtilities;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.AbstractCustomValueGetter;
import org.appwork.storage.config.annotations.CustomValueGetter;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultLongValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.appwork.storage.config.defaults.AbstractDefaultFactory;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;

public interface GeneralSettings extends ConfigInterface {
    class DefaultBrowserCommand extends AbstractDefaultFactory<String[]> {

        @Override
        public String[] getDefaultValue() {
            return CrossSystem.isWindows() ? new String[] { "rundll32.exe", "url.dll,FileProtocolHandler", "%s" } : null;
        }

    }

    class DefaultDownloadFolder extends AbstractDefaultFactory<String> {

        @Override
        public String getDefaultValue() {
            /* convert old value */
            String old = JDUtilities.getConfiguration().getStringProperty("DOWNLOAD_DIRECTORY", null);
            if (!StringUtils.isEmpty(old)) {
                File file = new File(old);
                if (file.exists() && file.isDirectory()) return old;
            }
            String home = System.getProperty("user.home");
            if (home != null && new File(home).exists() && new File(home).isDirectory()) {
                // new File(home, "downloads").mkdirs();
                return new File(home, "downloads").getAbsolutePath();
            } else {
                return Application.getResource("downloads").getAbsolutePath();
            }
        }
    }

    class CustomDownloadFolderGetter extends AbstractCustomValueGetter<String> {
        String defaultFolder = null;

        @Override
        public String getValue(String value) {
            if (StringUtils.isEmpty(value)) {
                if (defaultFolder != null) return defaultFolder;
                defaultFolder = new DefaultDownloadFolder().getDefaultValue();
                return defaultFolder;
            }
            return value;
        }
    };

    @DefaultIntValue(10)
    @AboutConfig
    @SpinnerValidator(min = 0, max = 120)
    @DescriptionForConfigEntry("AutoStart Downloads will show a Countdown Dialog after Startup. Set the countdown time to 0 to remove this dialog. @see showCountdownonAutoStartDownloads")
    int getAutoStartCountdownSeconds();

    AutoDownloadStartOption getAutoStartDownloadOption();

    String[] getBrowserCommandLine();

    @AboutConfig
    @DefaultEnumValue("NEVER")
    CleanAfterDownloadAction getCleanupAfterDownloadAction();

    @AboutConfig
    @DefaultFactory(DefaultDownloadFolder.class)
    @CustomValueGetter(CustomDownloadFolderGetter.class)
    String getDefaultDownloadFolder();

    ArrayList<String[]> getDownloadFolderHistory();

    @AboutConfig
    @DefaultLongValue(5 * 60 * 1000l)
    @DescriptionForConfigEntry("Waittime in ms if a Download HashCheck Failed")
    long getDownloadHashCheckFailedRetryWaittime();

    @AboutConfig
    @DescriptionForConfigEntry("Download Speed limit in bytes.")
    @DefaultIntValue(50 * 1024)
    @SpinnerValidator(min = 0, max = Integer.MAX_VALUE)
    int getDownloadSpeedLimit();

    @AboutConfig
    @DefaultLongValue(30 * 60 * 1000l)
    @DescriptionForConfigEntry("Waittime in ms if a Download Temp Unavailable Failed")
    long getDownloadTempUnavailableRetryWaittime();

    @AboutConfig
    @DefaultLongValue(10 * 60 * 1000l)
    @DescriptionForConfigEntry("Waittime in ms if a Download had unknown IOException")
    long getDownloadUnknownIOExceptionWaittime();

    @AboutConfig
    @DescriptionForConfigEntry("flush download buffers when filled up to x percent (1-100)")
    @DefaultIntValue(80)
    @SpinnerValidator(min = 1, max = 100)
    int getFlushBufferLevel();

    @AboutConfig
    @DescriptionForConfigEntry("flush download buffers after x ms")
    @DefaultIntValue(2 * 60 * 1000)
    int getFlushBufferTimeout();

    @AboutConfig
    @DescriptionForConfigEntry("Force Jdownloader to always keep a certain amount of MB Diskspace free")
    @DefaultIntValue(512)
    @SpinnerValidator(min = 0, max = Integer.MAX_VALUE)
    int getForcedFreeSpaceOnDisk();

    @AboutConfig
    @DefaultEnumValue("ASK_FOR_EACH_FILE")
    IfFileExistsAction getIfFileExistsAction();

    @AboutConfig
    @DescriptionForConfigEntry("max buffer size for write operations in kb")
    @SpinnerValidator(min = 100, max = 50240)
    @DefaultIntValue(500)
    int getMaxBufferSize();

    @AboutConfig
    @DescriptionForConfigEntry("http://jdownloader.org/knowledge/wiki/glossary/chunkload")
    @SpinnerValidator(min = 1, max = 20)
    @DefaultIntValue(1)
    int getMaxChunksPerFile();

    @AboutConfig
    @DescriptionForConfigEntry("How often a Plugin restarts a download if download failed")
    @DefaultIntValue(3)
    @RequiresRestart
    int getMaxPluginRetries();

    @AboutConfig
    @DescriptionForConfigEntry("How many downloads should Jdownloader download at once? Note that most hosters allow only one download at a time in freemode")
    @SpinnerValidator(min = 1, max = 20)
    @DefaultIntValue(3)
    int getMaxSimultaneDownloads();

    @AboutConfig
    @DefaultIntValue(1)
    @SpinnerValidator(min = 1, max = 100)
    int getMaxSimultaneDownloadsPerHost();

    @AboutConfig
    @DescriptionForConfigEntry("Timeout for network problems")
    @SpinnerValidator(min = 0, max = 1000000)
    @DefaultIntValue(15000)
    @RequiresRestart
    int getNetworkIssuesTimeout();

    @AboutConfig
    @DescriptionForConfigEntry("Pause Speed. in Pause Mode we limit speed to this value to keep connections open, but use hardly bandwidth")
    @DefaultIntValue(10240)
    @SpinnerValidator(min = 0, max = Integer.MAX_VALUE)
    int getPauseSpeed();

    @AboutConfig
    @DescriptionForConfigEntry("Penaltytime before a retry if JDownloader lost connection")
    @DefaultIntValue(5 * 60 * 1000)
    int getWaittimeOnConnectionLoss();

    @AboutConfig
    @DescriptionForConfigEntry("Keep max X old lists on disk (DownloadList,Linkgrabber)")
    @DefaultIntValue(5)
    @SpinnerValidator(min = 0, max = 20)
    int getKeepXOldLists();

    void setKeepXOldLists(int x);

    @AboutConfig
    boolean isAddNewLinksOnTop();

    @AboutConfig
    boolean isAutoaddLinksAfterLinkcheck();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isAutoOpenContainerAfterDownload();

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("AutoReconnect enabled?")
    boolean isAutoReconnectEnabled();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isCleanUpFilenames();

    boolean isClosedWithRunningDownloads();

    boolean isConvertRelativePathesJDRoot();

    @AboutConfig
    @DescriptionForConfigEntry("Do not start further downloads if others are waiting for a reconnect/new ip")
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
    @DescriptionForConfigEntry("If disabled, No Reconnects will be done while Resumable Downloads (Premium Downloads) are running")
    boolean isReconnectAllowedToInterruptResumableDownloads();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isLinkcheckEnabled();

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isMaxDownloadsPerHostEnabled();

    boolean isShowCountdownonAutoStartDownloads();

    boolean isSilentRestart();

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Use available Accounts?")
    boolean isUseAvailableAccounts();

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isUseOriginalLastModified();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isAutoSortChildrenEnabled();

    void setAutoSortChildrenEnabled(boolean b);

    void setAddNewLinksOnTop(boolean selected);

    void setAutoaddLinksAfterLinkcheck(boolean selected);

    void setAutoOpenContainerAfterDownload(boolean b);

    void setAutoReconnectEnabled(boolean b);

    void setAutoStartCountdownSeconds(int seconds);

    @AboutConfig
    @DefaultEnumValue("ONLY_IF_EXIT_WITH_RUNNING_DOWNLOADS")
    void setAutoStartDownloadOption(AutoDownloadStartOption option);

    @DefaultFactory(DefaultBrowserCommand.class)
    @AboutConfig
    @DescriptionForConfigEntry("CommandLine to open a link in a browser. Use %s as wildcard for the url")
    void setBrowserCommandLine(String[] b);

    void setCleanupAfterDownloadAction(CleanAfterDownloadAction action);

    void setCleanUpFilenames(boolean b);

    @DescriptionForConfigEntry("Is true, if jdownloader got closed with running downloads.")
    @DefaultBooleanValue(false)
    void setClosedWithRunningDownloads(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    @RequiresRestart
    @DescriptionForConfigEntry("Correct pathes relative to JDownloader root")
    void setConvertRelativePathesJDRoot(boolean b);

    void setDefaultDownloadFolder(String ddl);

    void setDownloadControllerPrefersReconnectEnabled(boolean b);

    void setDownloadFolderHistory(ArrayList<String[]> history);

    void setDownloadHashCheckFailedRetryWaittime(long ms);

    void setDownloadSpeedLimit(int bytes);

    void setDownloadSpeedLimitEnabled(boolean b);

    void setDownloadTempUnavailableRetryWaittime(long ms);

    void setDownloadUnknownIOExceptionWaittime(long ms);

    void setFilterRegex(boolean b);

    void setFlushBufferLevel(int level);

    void setFlushBufferTimeout(int ms);

    void setForcedFreeSpaceOnDisk(int mb);

    void setHashCheckEnabled(boolean b);

    void setIfFileExistsAction(IfFileExistsAction action);

    void setReconnectAllowedToInterruptResumableDownloads(boolean b);

    void setLinkcheckEnabled(boolean b);

    void setMaxBufferSize(int num);

    void setMaxChunksPerFile(int num);

    void setMaxDownloadsPerHostEnabled(boolean b);

    void setMaxPluginRetries(int nums);

    void setMaxSimultaneDownloads(int num);

    void setMaxSimultaneDownloadsPerHost(int num);

    void setNetworkIssuesTimeout(int timeout);

    void setPauseSpeed(int kb);

    @DefaultBooleanValue(true)
    @AboutConfig
    @DescriptionForConfigEntry("@see AutoStartCountdownSeconds")
    void setShowCountdownonAutoStartDownloads(boolean b);

    @DefaultBooleanValue(false)
    void setSilentRestart(boolean b);

    void setUseAvailableAccounts(boolean b);

    void setUseOriginalLastModified(boolean b);

    void setWaittimeOnConnectionLoss(int milliseconds);

    @AboutConfig
    @DescriptionForConfigEntry("Should JDownloader check free available diskspace before download starts?")
    @DefaultBooleanValue(true)
    boolean isFreeSpaceCheckEnabled();

    void setFreeSpaceCheckEnabled(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("if this value is >0, the subfolder option only will be used if the package contains more than subfolderThreshold value links")
    @DefaultIntValue(0)
    int getSubfolderThreshold();

    void setSubfolderThreshold(int i);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Disable this option if you do not want to see the filename in a captchadialog")
    boolean isShowFileNameInCaptchaDialogEnabled();

    void setShowFileNameInCaptchaDialogEnabled(boolean b);

    //
    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("If Enabled, JDownloader will save the linkgrabber list when you exit jd, and restore it on next startup")
    boolean isSaveLinkgrabberListEnabled();

    void setSaveLinkgrabberListEnabled(boolean b);

    public static enum DeleteContainerAction {
        ASK_FOR_DELETE,
        DELETE,
        DONT_DELETE
    }

    @AboutConfig
    @DefaultEnumValue("ASK_FOR_DELETE")
    @DescriptionForConfigEntry("What Action should be performed after adding a container (DLC RSDF,METALINK,CCF,...)")
    DeleteContainerAction getDeleteContainerFilesAfterAddingThemAction();

    void setDeleteContainerFilesAfterAddingThemAction(DeleteContainerAction action);

    @DefaultIntValue(15)
    @AboutConfig
    int getMaxPremiumIcons();

    void setMaxPremiumIcons(int icons);

}
