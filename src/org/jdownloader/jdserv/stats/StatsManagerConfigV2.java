package org.jdownloader.jdserv.stats;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultDoubleValue;

public interface StatsManagerConfigV2 extends ConfigInterface {

    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isEnabled();

    void setEnabled(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    void setAlwaysAllowLogUploads(boolean dontShowAgainSelected);

    boolean isAlwaysAllowLogUploads();

    @AboutConfig
    @DefaultDoubleValue(0.95d)
    double getCaptchaUploadPercentage();

    void setCaptchaUploadPercentage(double d);

    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isCaptchaUploadEnabled();

    void setCaptchaUploadEnabled(boolean b);
}
