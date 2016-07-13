package org.jdownloader.plugins.components.google;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.AccountConfigInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.translate._JDT;

public interface GoogleAccountConfig extends AccountConfigInterface {
    public class Translation {
        public String getUsageRecaptchaV1Enabled_label() {
            return _JDT.T.plugins_google_use_for_recaptcha1();
        }

        public String getUsageRecaptchaV1Enabled_description() {
            return _JDT.T.plugins_google_recaptcha2_description();
        }

        public String getUsageRecaptchaV2Enabled_label() {
            return _JDT.T.plugins_google_use_for_recaptcha2();
        }
    }

    public static final GoogleAccountConfig.Translation TRANSLATION = new Translation();

    @DefaultBooleanValue(true)
    @Order(10)
    boolean isUsageRecaptchaV1Enabled();

    void setUsageRecaptchaV1Enabled(boolean b);

    @Order(20)
    @DefaultBooleanValue(true)
    boolean isUsageRecaptchaV2Enabled();

    void setUsageRecaptchaV2Enabled(boolean b);

}