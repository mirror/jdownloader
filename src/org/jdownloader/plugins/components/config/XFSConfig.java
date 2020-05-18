package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;

// @PluginHost(host = "TODO.todo", type = Type.HOSTER)
public interface XFSConfig extends PluginConfigInterface {
    public static class TRANSLATION {
        public String getPreferHTTP_label() {
            return "Prefer http instead of https (not recommended)?";
        }
    }

    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(30)
    boolean isPreferHTTP();

    void setPreferHTTP(boolean b);
}