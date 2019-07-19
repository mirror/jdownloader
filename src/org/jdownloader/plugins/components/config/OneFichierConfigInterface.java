package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;
import org.jdownloader.translate._JDT;

@PluginHost(host = "1fichier.com", type = Type.HOSTER)
public interface OneFichierConfigInterface extends PluginConfigInterface {
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
            return "Use premium API? This helps to get around 2-factor-authentification login issues. Works ONLY for premium accounts! If not done before, you should enable 2-factor-authentification afterwards.";
        }
    }

    public static final OneFichierConfigInterface.OneFichierConfigInterfaceTranslation TRANSLATION = new OneFichierConfigInterfaceTranslation();

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
    @DefaultBooleanValue(false)
    @Order(30)
    @TakeValueFromSubconfig("USE_PREMIUM_API")
    boolean isUsePremiumAPIEnabled();

    void setUsePremiumAPIEnabled(boolean b);

    @AboutConfig
    @DefaultIntValue(10)
    @SpinnerValidator(min = 0, max = 60)
    @Order(40)
    int getSmallFilesWaitInterval();

    void setSmallFilesWaitInterval(int i);
}