package jd;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;

public interface DownloadSettings extends ConfigInterface {

    void setDefaultDownloadFolder(String ddl);

    String getDefaultDownloadFolder();

    @DefaultBooleanValue(false)
    boolean isAutoStartDownloadsOnStartupEnabled();

    void setAutoStartDownloadsOnStartupEnabled(boolean b);
}
