package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "wikifeet.com", type = Type.CRAWLER)
public interface WikifeetComConfig extends PluginConfigInterface {
    public static final WikifeetComConfig.TRANSLATION TRANSLATION                           = new TRANSLATION();
    final String                                      text_SelectPreferredPackagenameScheme = "Select preferred packagename scheme";
    final String                                      text_GetCustomPackagenameScheme       = "Enter custom packagename scheme";

    public static class TRANSLATION {
        public String getSelectPreferredPackagenameScheme_label() {
            return text_SelectPreferredPackagenameScheme;
        }

        public String getAlbumPackagenameScheme_label() {
            return text_GetCustomPackagenameScheme;
        }
    }

    public static enum AlbumPackagenameScheme implements LabelInterface {
        DEFAULT {
            @Override
            public String getLabel() {
                return "Default: *user*";
            }
        },
        CUSTOM {
            @Override
            public String getLabel() {
                return "Custom";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DEFAULT")
    @Order(10)
    @DescriptionForConfigEntry(text_SelectPreferredPackagenameScheme)
    AlbumPackagenameScheme getAlbumPackagenameScheme();

    void setAlbumPackagenameScheme(final AlbumPackagenameScheme quality);

    @AboutConfig
    @DefaultStringValue("*user* - *birth_place* - *birth_date* - *shoe_size* - *imdb_url*")
    @DescriptionForConfigEntry(text_GetCustomPackagenameScheme)
    @Order(10)
    String getCustomPackagenameScheme();

    public void setCustomPackagenameScheme(final String str);
}