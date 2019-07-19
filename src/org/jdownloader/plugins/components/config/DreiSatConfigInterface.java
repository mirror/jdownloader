package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "3sat.de", type = Type.HOSTER)
public interface DreiSatConfigInterface extends PluginConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("Q_BEST")
    boolean isLoadBestVersionOnlyEnabled();

    void setLoadBestVersionOnlyEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isLoadLowVersionEnabled();

    void setLoadLowVersionEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @TakeValueFromSubconfig("Q_HIGH")
    boolean isLoadHighVersionEnabled();

    void setLoadHighVersionEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @TakeValueFromSubconfig("Q_VERYHIGH")
    boolean isLoadVeryHighVersionEnabled();

    void setLoadVeryHighVersionEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("Q_HD")
    boolean isLoadHDVersionEnabled();

    void setLoadhDVersionEnabled(boolean b);
}