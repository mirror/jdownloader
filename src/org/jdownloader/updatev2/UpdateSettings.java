package org.jdownloader.updatev2;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultLongValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;

public interface UpdateSettings extends ConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("If enabled, JDownloader will update all plugins silently in the background without restarting")
    boolean isInstallUpdatesSilentlyIfPossibleEnabled();

    void setInstallUpdatesSilentlyIfPossibleEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("If enabled, JD will install Updates silently during the next JDStart")
    boolean isAskMyBeforeInstallingAnUpdateEnabled();

    void setAskMyBeforeInstallingAnUpdateEnabled(boolean b);

    // @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Jar diffs speed up the update process because it reduces the update package size a lot. Do NOT disable this without a very good reason")
    boolean isJarDiffEnabled();

    void setJarDiffEnabled(boolean b);

    @DefaultLongValue(10 * 60 * 1000)
    @AboutConfig
    @DescriptionForConfigEntry("[MS] How often shall JD do a silent Updatecheck.")
    long getUpdateInterval();

    void setUpdateInterval(long intervalMS);

    @AboutConfig
    @DefaultBooleanValue(true)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("If true, JDownloader will check for updates in an interval (see: Update Interval)")
    boolean isAutoUpdateCheckEnabled();

    void setAutoUpdateCheckEnabled(boolean enabled);

    @DefaultBooleanValue(true)
    @RequiresRestart("A JDownloader Restart is Required")
    @AboutConfig
    @DescriptionForConfigEntry("If enabled, the Updater Gui will always be on top of all other windows")
    boolean isUpdateGuiAlwaysOnTop();

    void setUpdateGuiAlwaysOnTop(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry("If enabled, JDownloader will ask before starting to download Updates.")
    boolean isDoAskBeforeDownloadingAnUpdate();

    void setDoAskBeforeDownloadingAnUpdate(boolean b);

    //
    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry("If enabled, JDownloader will close the Updatedialog if silent updates were installed, and there are no further updates. Else, you have to click to close")
    boolean isAutohideGuiIfSilentUpdatesWereInstalledEnabled();

    void setAutohideGuiIfSilentUpdatesWereInstalledEnabled(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry("If enabled, JDownloader will close the Updatedialog if there are no updates. Else, you have to click to close")
    boolean isAutohideGuiIfThereAreNoUpdatesEnabled();

    void setAutohideGuiIfThereAreNoUpdatesEnabled(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isProxyDialogOnNoConnectionEnabled();

    void setProxyDialogOnNoConnectionEnabled(boolean b);

    @DefaultLongValue(0l)
    void setLastSuccessfulConnection(long currentTimeMillis);

    long getLastSuccessfulConnection();

    String getSTID();

    void setSTID(String stid);

    long getLastST();

    void setLastST(long currentTimeMillis);

    @DefaultBooleanValue(true)
    @AboutConfig
    @DescriptionForConfigEntry("Try to install updates when you exit JDownloader")
    boolean isInstallUpdatesOnExitEnabled();

    void setInstallUpdatesOnExitEnabled(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry("Show the tray panel when installing updates on exit")
    boolean isInstallUpdatesOnExitPanelVisibleDev();

    void setInstallUpdatesOnExitPanelVisibleDev(boolean b);

}
