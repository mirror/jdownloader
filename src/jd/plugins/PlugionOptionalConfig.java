package jd.plugins;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;

public interface PlugionOptionalConfig extends ConfigInterface {
    /**
     * The enabled field is used to store whether the plugin should be
     * autoactivated on startup or not.
     * 
     * @param ret
     */
    void setEnabled(boolean ret);

    /**
     * The enabled field is used to store whether the plugin should be
     * autoactivated on startup or not. <br>
     * <b>DO NOT USE THIS TO CHECK WHETHER A EXTENSION RUNNING OR NOT</b> Use
     * extension.isEnabled() instead
     * 
     * @param ret
     */
    boolean isEnabled();

    /**
     * Not for external use. Extensions use this to save gui enabled/disabled
     * status across sessions
     * 
     * @param b
     */
    void setGuiEnabled(boolean b);

    /**
     * if true, the ExtensionController will restore the extension's gui after
     * startup
     * 
     * @return
     */
    boolean isGuiEnabled();

    /**
     * Is true by default. Helps the extension to Load defaultvalues on the
     * first start. for internal use only
     * 
     * @return
     */
    @DefaultBooleanValue(true)
    boolean isFreshInstall();

    /**
     * IS set to false on first start. for internal usage only.
     * 
     * @param b
     */
    void setFreshInstall(boolean b);

}
