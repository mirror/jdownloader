package jd;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.jdownloader.settings.AboutConfig;

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
}
