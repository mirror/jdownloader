package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultOnNull;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "send.cm", type = Type.HOSTER)
public interface XFSConfigSendCm extends XFSConfig {
    public static final XFSConfigSendCm.TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        public String getLoginMode_label() {
            return "Login mode";
        }
    }

    public static final LoginMode DEFAULT_MODE = LoginMode.AUTO;

    public static enum LoginMode implements LabelInterface {
        AUTO {
            @Override
            public String getLabel() {
                return "Auto";
            }
        },
        API {
            @Override
            public String getLabel() {
                return "[Recommended] API";
            }
        },
        WEBSITE {
            @Override
            public String getLabel() {
                return "Website";
            }
        },
        DEFAULT {
            @Override
            public String getLabel() {
                return "Default: " + AUTO.getLabel();
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DEFAULT")
    @Order(100)
    @DescriptionForConfigEntry("Switch between API and website mode. API mode = Login via API Key, Website mode = Login via username and password.")
    @DefaultOnNull
    LoginMode getLoginMode();

    void setLoginMode(final LoginMode mode);
}