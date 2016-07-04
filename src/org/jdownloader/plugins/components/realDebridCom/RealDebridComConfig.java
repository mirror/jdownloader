package org.jdownloader.plugins.components.realDebridCom;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.CustomStorageName;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.PluginConfigInterface;

@CustomStorageName("RealDebridCom")
public interface RealDebridComConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(false)
    void setIgnoreServerSideChunksNum(boolean b);

    boolean isIgnoreServerSideChunksNum();

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isUseSSLForDownload();

    void setUseSSLForDownload();

}