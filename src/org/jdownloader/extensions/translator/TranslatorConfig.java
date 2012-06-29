package org.jdownloader.extensions.translator;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.CryptedStorage;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.Description;

/**
 * This interface defines all settings of the translation Extension. Extend it if you need additional entries. Make sure that you have a
 * valid getter AND setter
 * 
 * @author thomas
 * 
 */
@CryptedStorage(key = { 0x00, 0x02, 0x11, 0x01, 0x01, 0x54, 0x02, 0x01, 0x01, 0x01, 0x12, 0x01, 0x01, 0x01, 0x22, 0x01 })
public interface TranslatorConfig extends ExtensionConfigInterface {

    @Description("Username for the SVN Access")
    public String getSVNUser();

    public void setSVNUser(String user);

    @Description("Password for the SVN Access")
    public String getSVNPassword();

    public void setSVNPassword(String password);

    @DefaultBooleanValue(false)
    public boolean isRememberLoginsEnabled();

    public void setRememberLoginsEnabled(boolean b);

    public void setLastLoaded(String id);

    public String getLastLoaded();

    @DefaultBooleanValue(true)
    @AboutConfig
    public boolean isQuickEditBarVisible();

    public void setQuickEditBarVisible(boolean b);

    @DefaultIntValue(200)
    @AboutConfig
    public int getQuickEditHeight();

    public void setQuickEditHeight(int i);
}
