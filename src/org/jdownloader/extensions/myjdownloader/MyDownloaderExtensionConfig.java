package org.jdownloader.extensions.myjdownloader;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;

public interface MyDownloaderExtensionConfig extends ExtensionConfigInterface {

    @DefaultStringValue("api.jdownloader.org")
    @AboutConfig
    public String getAPIServerURL();

    public void setAPIServerURL(String url);

    //

    @DefaultIntValue(80)
    @AboutConfig
    public int getAPIServerPort();

    public void setAPIServerPort(int port);

    public String getUsername();

    public String getPassword();

    public void setUsername(String s);

    public void setPassword(String s);

}
