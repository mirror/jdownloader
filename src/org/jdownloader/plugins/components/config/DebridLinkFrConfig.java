package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "debrid-link.fr", type = Type.HOSTER)
public interface DebridLinkFrConfig extends PluginConfigInterface {
    public static enum PreferredDomain implements LabelInterface {
        DEFAULT {
            @Override
            public String getLabel() {
                return "default (= debrid-link.fr)";
            }
        },
        DOMAIN1 {
            @Override
            public String getLabel() {
                return "debrid-link.com";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DEFAULT")
    PreferredDomain getPreferredDomain();

    void setPreferredDomain(PreferredDomain domain);
}