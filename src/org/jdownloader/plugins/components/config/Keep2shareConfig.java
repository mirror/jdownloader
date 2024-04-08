package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "k2s.cc", type = Type.HOSTER)
public interface Keep2shareConfig extends PluginConfigInterface {
    public static class TRANSLATION {
        public String getEnableReconnectWorkaround_label() {
            return "Enable reconnect workaround for free mode?";
        }

        public String getReferer_label() {
            return "Custom referer e.g. 'refererexamplewebsite.tld'";
        }

        public String getForceCustomReferer_label() {
            return "Force custom referer even if referer is given in URL via '?site=refererexamplewebsite.tld'?";
        }

        public String getEnableSSL_label() {
            return "Use Secure Communication over SSL (HTTPS://)";
        }

        public String getMaxSimultaneousFreeDownloads_label() {
            return "Max. number of simultaneous downloads in free mode";
        }

        public String getFileLinkAddMode_label() {
            return "File link add mode";
        }

        public String getFileLinkcheckMode_label() {
            return "File linkcheck mode";
        }

        public String getCaptchaTimeoutBehavior_label() {
            return "What to do on captcha timeout?";
        }
    }

    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(10)
    @TakeValueFromSubconfig("EXPERIMENTALHANDLING")
    @DescriptionForConfigEntry("This may avoid unnecessary captchas when an IP limit is reached in free download mode.")
    boolean isEnableReconnectWorkaround();

    void setEnableReconnectWorkaround(boolean b);

    @AboutConfig
    @Order(20)
    @TakeValueFromSubconfig("CUSTOM_REFERER")
    @DescriptionForConfigEntry("Custom referer value to be used.")
    String getReferer();

    void setReferer(String referer);

    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(25)
    @DescriptionForConfigEntry("Always use custom referer even if added URL contains another referer.")
    boolean isForceCustomReferer();

    void setForceCustomReferer(boolean b);

    @AboutConfig
    @DefaultIntValue(1)
    @SpinnerValidator(min = 1, max = 20, step = 1)
    @Order(40)
    int getMaxSimultaneousFreeDownloads();

    void setMaxSimultaneousFreeDownloads(int maxFree);

    final FileLinkAddMode defaultFileLinkAddMode = FileLinkAddMode.HOSTER_PLUGIN_LINKCHECK;

    public static enum FileLinkAddMode implements LabelInterface {
        HOSTER_PLUGIN_LINKCHECK {
            @Override
            public String getLabel() {
                return "Pass to hoster plugin for file linkcheck";
            }
        },
        CRAWLER_PLUGIN_VIA_API_GETFILESTATUS {
            @Override
            public String getLabel() {
                return "Check for folder: Use this if you plan to add single file links that are folders";
            }
        },
        DEFAULT {
            @Override
            public String getLabel() {
                return "Default: " + defaultFileLinkAddMode.getLabel();
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DEFAULT")
    @Order(50)
    @DescriptionForConfigEntry("Configure how '/file/' links are processed when they are initially added.")
    FileLinkAddMode getFileLinkAddMode();

    void setFileLinkAddMode(FileLinkAddMode mode);

    final LinkcheckMode defaultFileLinkcheckMode = LinkcheckMode.MASS_LINKCHECK;

    public static enum LinkcheckMode implements LabelInterface {
        MASS_LINKCHECK {
            @Override
            public String getLabel() {
                return "Mass linkcheck: Checks up to 100 items with a single request via API '/api/v2/getfilesinfo'";
            }
        },
        SINGLE_LINKCHECK {
            @Override
            public String getLabel() {
                return "Single linkcheck: Check links one by one via '/api/v2/getfilestatus'";
            }
        },
        AUTO {
            @Override
            public String getLabel() {
                return "Auto: Use single linkcheck if premium account is available, else mass-linkcheck";
            }
        },
        DEFAULT {
            @Override
            public String getLabel() {
                return "Default: " + defaultFileLinkcheckMode.getLabel();
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DEFAULT")
    @Order(51)
    @DescriptionForConfigEntry("Configure how '/file/' links are checked.")
    LinkcheckMode getFileLinkcheckMode();

    void setFileLinkcheckMode(LinkcheckMode mode);

    public static enum CaptchaTimeoutBehavior implements LabelInterface {
        GLOBAL_SETTING {
            @Override
            public String getLabel() {
                return "Use global (default) behavior";
            }
        },
        SKIP_HOSTER {
            @Override
            public String getLabel() {
                return "Skip all items of this hoster";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("GLOBAL_SETTING")
    @Order(60)
    @DescriptionForConfigEntry("Define what should happen when a captcha runs into a timeout.")
    CaptchaTimeoutBehavior getCaptchaTimeoutBehavior();

    void setCaptchaTimeoutBehavior(CaptchaTimeoutBehavior behavior);
}