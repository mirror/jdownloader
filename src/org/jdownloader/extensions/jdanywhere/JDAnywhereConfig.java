package org.jdownloader.extensions.jdanywhere;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.DescriptionForConfigEntry;

public interface JDAnywhereConfig extends ExtensionConfigInterface {

    @DescriptionForConfigEntry("Username")
    String getUsername();

    void setUsername(String username);

    @DescriptionForConfigEntry("Password")
    String getPassword();

    void setPassword(String password);

    @DescriptionForConfigEntry("Port")
    int getPort();

    void setPort(int port);

}
