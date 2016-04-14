package jd.plugins.hoster;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.PluginConfigInterface;

public interface DropboxConfig extends PluginConfigInterface {
    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry("If enabled, the Linkgrabber will offer a zip archive to download folders")
    boolean isZipFolderDownloadEnabled();

    void setZipFolderDownloadEnabled(boolean b);

}
