package jd.plugins.hoster;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;

public interface DropboxConfig extends ConfigInterface {
    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry("If enabled, the Linkgrabber will offer a zip archive to download folders")
    boolean isZipFolderDownloadEnabled();

    void setZipFolderDownloadEnabled(boolean b);

}
