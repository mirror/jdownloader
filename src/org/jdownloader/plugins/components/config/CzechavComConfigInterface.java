package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "czechav.com", type = Type.HOSTER)
public interface CzechavComConfigInterface extends PluginConfigInterface {
    @DefaultBooleanValue(true)
    @Order(9)
    boolean isFastLinkcheckEnabled();

    void setFastLinkcheckEnabled(boolean b);

    @DefaultBooleanValue(false)
    @Order(10)
    boolean isGrabBestVideoVersionEnabled();

    void setGrabBestVideoVersionEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(20)
    boolean isGrab2160pVideoEnabled();

    void setGrab2160pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(30)
    boolean isGrab1080pVideoEnabled();

    void setGrab1080pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(40)
    boolean isGrab720pVideoEnabled();

    void setGrab720pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(50)
    boolean isGrab540pVideoEnabled();

    void setGrab540pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(60)
    boolean isGrab360pVideoEnabled();

    void setGrab360pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(70)
    boolean isGrabOtherResolutionsVideoEnabled();

    void setGrabOtherResolutionsVideoEnabled(boolean b);

}