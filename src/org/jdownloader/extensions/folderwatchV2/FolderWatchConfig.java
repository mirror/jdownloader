package org.jdownloader.extensions.folderwatchV2;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultLongValue;
import org.appwork.storage.config.annotations.DefaultStringArrayValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;

public interface FolderWatchConfig extends ExtensionConfigInterface {
    @AboutConfig
    @DefaultStringArrayValue({ "folderwatch" })
    String[] getFolders();

    void setFolders(String[] folders);

    @AboutConfig
    @DefaultLongValue(10 * 1000)
    @DescriptionForConfigEntry("check interval in milliseconds (1 second = 1000)")
    long getCheckInterval();

    void setCheckInterval(long ms);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isDebugEnabled();

    void setDebugEnabled(boolean b);
}
