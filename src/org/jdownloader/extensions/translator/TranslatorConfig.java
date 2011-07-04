package org.jdownloader.extensions.translator;

import jd.plugins.ExtensionConfigInterface;

import org.jdownloader.settings.AboutConfig;

public interface TranslatorConfig extends ExtensionConfigInterface {

    @AboutConfig
    public String getSVNUser();

    public void setSVNUser(String user);

    @AboutConfig
    public String getSVNPassword();

    public void setSVNPassword(String password);
}
