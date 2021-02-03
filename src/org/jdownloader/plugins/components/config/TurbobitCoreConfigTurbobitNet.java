package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "turbobit.net", type = Type.HOSTER)
public interface TurbobitCoreConfigTurbobitNet extends TurbobitCoreConfig {
    public static enum PreferredDomain implements LabelInterface {
        DEFAULT {
            @Override
            public String getLabel() {
                return "default (= turbobit.net)";
            }
        },
        DOMAIN1 {
            @Override
            public String getLabel() {
                return "turbo-bit.pw";
            }
        },
        DOMAIN2 {
            @Override
            public String getLabel() {
                return "turbobi.pw";
            }
        };
        // DOMAIN2 {
        // @Override
        // public String getLabel() {
        // return "default (= turbobit.net)";
        // }
        // };
    }

    @AboutConfig
    @DefaultEnumValue("DEFAULT")
    @Order(10)
    PreferredDomain getPreferredDomain();

    void setPreferredDomain(PreferredDomain domain);

    @AboutConfig
    // @DefaultStringValue("")
    @DescriptionForConfigEntry("Define custom preferred domain. If given this will be preferred over the above selection")
    @Order(20)
    String getCustomDomain();

    void setCustomDomain(String str);
}