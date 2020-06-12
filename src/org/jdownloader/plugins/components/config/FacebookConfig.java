package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "facebook.com", type = Type.HOSTER)
public interface FacebookConfig extends PluginConfigInterface {
    @AboutConfig
    @DescriptionForConfigEntry("Prefer HD quality?")
    @DefaultBooleanValue(true)
    @TakeValueFromSubconfig("PREFERHD")
    @Order(20)
    boolean isPreferHD();

    void setPreferHD(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Enable fast linkcheck? [Filesize won't be displayed until downloads start!]")
    @DefaultBooleanValue(true)
    @Order(30)
    boolean isEnableFastLinkcheck();

    void setEnableFastLinkcheck(boolean b);
    // @AboutConfig
    // @DescriptionForConfigEntry("Photos: Use album name in filename [note that filenames change once the download starts]?")
    // @DefaultBooleanValue(false)
    // @Order(30)
    // @TakeValueFromSubconfig("USE_ALBUM_NAME_IN_FILENAME")
    // boolean isUseAlbumNameInFilename();
    //
    // void setUseAlbumNameInFilename(boolean b);
}