package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "pietsmiet.de", type = Type.CRAWLER)
public interface PietsmietDeConfig extends PluginConfigInterface {
    @DefaultBooleanValue(false)
    @Order(10)
    boolean isGrabBestVideoVersionEnabled();

    void setGrabBestVideoVersionEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(20)
    boolean isGrab1080pVideoEnabled();

    void setGrab1080pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(30)
    boolean isGrab720pVideoEnabled();

    void setGrab720pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(40)
    boolean isGrab480pVideoEnabled();

    void setGrab480pVideoEnabled(boolean b);
    /*
     * @DefaultBooleanValue(true)
     * 
     * @Order(50) boolean isGrab360pVideoEnabled();
     * 
     * void setGrab360pVideoEnabled(boolean b);
     */
}