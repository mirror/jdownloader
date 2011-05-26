package org.jdownloader.settings;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultFactory;

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

}
