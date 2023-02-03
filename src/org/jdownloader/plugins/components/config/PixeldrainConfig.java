package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "pixeldrain.com", type = Type.HOSTER)
public interface PixeldrainConfig extends PluginConfigInterface {
    final String                                     text_ActionOnSpeedLimitReached = "Action to perform when downloads over your IP are speed limited";
    final String                                     text_ActionOnCaptchaRequired   = "Action to perform when captcha is required for downloading";
    public static final PixeldrainConfig.TRANSLATION TRANSLATION                    = new TRANSLATION();

    public static class TRANSLATION {
        public String getActionOnSpeedLimitReached_label() {
            return text_ActionOnSpeedLimitReached;
        }

        public String getActionOnCaptchaRequired_label() {
            return text_ActionOnCaptchaRequired;
        }
    }

    public static enum ActionOnSpeedLimitReached implements LabelInterface {
        TRIGGER_RECONNECT_TO_CHANGE_IP {
            @Override
            public String getLabel() {
                return "Trigger reconnect event to try to change IP";
            }
        },
        ALLOW_SPEED_LIMITED_DOWNLOAD {
            @Override
            public String getLabel() {
                return "Allow speed limited downloads";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("TRIGGER_RECONNECT_TO_CHANGE_IP")
    @Order(10)
    @DescriptionForConfigEntry(text_ActionOnSpeedLimitReached)
    ActionOnSpeedLimitReached getActionOnSpeedLimitReached();

    void setActionOnSpeedLimitReached(final ActionOnSpeedLimitReached action);

    public static enum ActionOnCaptchaRequired implements LabelInterface {
        PROCESS_CAPTCHA {
            @Override
            public String getLabel() {
                return "Ask for captcha";
            }
        },
        RETRY_LATER {
            @Override
            public String getLabel() {
                return "Try again later";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("PROCESS_CAPTCHA")
    @Order(20)
    @DescriptionForConfigEntry(text_ActionOnCaptchaRequired)
    ActionOnCaptchaRequired getActionOnCaptchaRequired();

    void setActionOnCaptchaRequired(final ActionOnCaptchaRequired action);
}