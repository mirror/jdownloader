package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "turbobit.net", type = Type.HOSTER)
public interface TurbobitConfig extends PluginConfigInterface {
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
    PreferredDomain getPreferredDomain();
}