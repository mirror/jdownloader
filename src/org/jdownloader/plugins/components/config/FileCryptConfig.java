package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "filecrypt.cc", type = Type.CRAWLER)
public interface FileCryptConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultIntValue(0)
    @SpinnerValidator(min = 0, max = 10, step = 1)
    @Order(10)
    @DescriptionForConfigEntry("Define max number of retries to avoid cutcaptcha")
    int getMaxCutCaptchaAvoidaneRetries();

    void setMaxCutCaptchaAvoidaneRetries(int maxretries);
}