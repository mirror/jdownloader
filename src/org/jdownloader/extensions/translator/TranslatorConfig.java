package org.jdownloader.extensions.translator;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.Description;
import org.jdownloader.settings.annotations.AboutConfig;

/**
 * This interface defines all settings of the translation Extension. Extend it
 * if you need additional entries. Make sure that you have a valid getter AND
 * setter
 * 
 * @author thomas
 * 
 */
public interface TranslatorConfig extends ExtensionConfigInterface {

    @AboutConfig
    @Description("Username for the SVN Access")
    public String getSVNUser();

    public void setSVNUser(String user);

    @AboutConfig
    @Description("Password for the SVN Access")
    public String getSVNPassword();

    public void setSVNPassword(String password);
}
