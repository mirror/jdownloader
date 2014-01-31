package org.jdownloader.extensions.folderwatchV2;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultLongValue;
import org.appwork.storage.config.annotations.DefaultStringArrayValue;

public interface FolderWatchConfig extends ExtensionConfigInterface {
    @AboutConfig
    @DefaultStringArrayValue({ "folderwatch" })
    String[] getFolders();

    void setFolders(String[] folders);

    @AboutConfig
    @DefaultLongValue(10 * 1000)
    long getCheckInterval();

    void setCheckInterval(long ms);
}
