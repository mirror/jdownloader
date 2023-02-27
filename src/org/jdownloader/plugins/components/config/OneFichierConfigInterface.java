package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;
import org.jdownloader.translate._JDT;

@PluginHost(host = "1fichier.com", type = Type.HOSTER)
public interface OneFichierConfigInterface extends PluginConfigInterface {
    public static final OneFichierConfigInterface.OneFichierConfigInterfaceTranslation TRANSLATION = new OneFichierConfigInterfaceTranslation();

    public static class OneFichierConfigInterfaceTranslation {
        public String getPreferReconnectEnabled_label() {
            return _JDT.T.lit_prefer_reconnect();
        }

        public String getPreferSSLEnabled_label() {
            return _JDT.T.lit_prefer_ssl();
        }

        public String getSmallFilesWaitInterval_label() {
            return "Wait x seconds for small files (smaller than 50 mbyte) to prevent IP block";
        }

        public String getUsePremiumAPIEnabled_label() {
            return "Use premium API[recommended]? If you want to add 1fichier free-accounts, disable this.";
        }

        public String getMaxPremiumChunks_label() {
            return "Max number of chunks(premium account)? See 1fichier.com/hlp.html#dllent";
        }

        public String getGlobalRequestIntervalLimit1fichierComMilliseconds_label() {
            return "Define global request limit for 1fichier.com milliseconds";
        }

        public String getGlobalRequestIntervalLimitAPI1fichierComMilliseconds_label() {
            return "Define global request limit for api.1fichier.com milliseconds";
        }
    }

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("PREFER_RECONNECT")
    @Order(10)
    boolean isPreferReconnectEnabled();

    void setPreferReconnectEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @TakeValueFromSubconfig("PREFER_SSL")
    @Order(20)
    boolean isPreferSSLEnabled();

    void setPreferSSLEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @Order(30)
    /**
     * Disabled by default because the API has very tight "account check" rate limits which will result in it failing to obtain account
     * information and/or even temporary account bans.
     */
    boolean isUsePremiumAPIEnabled();

    void setUsePremiumAPIEnabled(boolean b);

    @AboutConfig
    @DefaultIntValue(10)
    @SpinnerValidator(min = 0, max = 60)
    @Order(40)
    int getSmallFilesWaitInterval();

    void setSmallFilesWaitInterval(int i);

    @AboutConfig
    @DefaultIntValue(3)
    @Order(50)
    @SpinnerValidator(min = 0, max = 20, step = 1)
    int getMaxPremiumChunks();

    void setMaxPremiumChunks(int b);

    @AboutConfig
    @SpinnerValidator(min = 2500, max = 30000, step = 500)
    @DefaultIntValue(2500)
    @DescriptionForConfigEntry("Define global request limit for 1fichier.com milliseconds")
    @Order(60)
    int getGlobalRequestIntervalLimit1fichierComMilliseconds();

    void setGlobalRequestIntervalLimit1fichierComMilliseconds(int milliseconds);

    @AboutConfig
    @SpinnerValidator(min = 2500, max = 30000, step = 500)
    @DefaultIntValue(2500)
    @DescriptionForConfigEntry("Define global request limit for api.1fichier.com milliseconds")
    @Order(70)
    int getGlobalRequestIntervalLimitAPI1fichierComMilliseconds();

    void setGlobalRequestIntervalLimitAPI1fichierComMilliseconds(int milliseconds);
}