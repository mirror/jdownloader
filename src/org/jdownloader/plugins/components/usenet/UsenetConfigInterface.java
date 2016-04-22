package org.jdownloader.plugins.components.usenet;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.PluginConfigInterface;

public interface UsenetConfigInterface extends PluginConfigInterface {
    @AboutConfig
    @DescriptionForConfigEntry("Usenet Server address NOTE: host port and ssl must be a valid combination")
    String getHost();

    void setHost(String server);

    @DescriptionForConfigEntry("Usenet Server port NOTE: host port and ssl must be a valid combination")
    @AboutConfig
    void setPort(int port);

    int getPort();

    @DescriptionForConfigEntry("Usenet SSL enabled NOTE: host port and ssl must be a valid combination")
    @AboutConfig
    void setSSLEnabled(boolean b);

    boolean isSSLEnabled();

}
