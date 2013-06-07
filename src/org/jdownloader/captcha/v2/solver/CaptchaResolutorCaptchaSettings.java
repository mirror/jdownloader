package org.jdownloader.captcha.v2.solver;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;

public interface CaptchaResolutorCaptchaSettings extends ConfigInterface {
	@AboutConfig
	String getUser();

	@DescriptionForConfigEntry("User ResolutorCaptcha")
	void setUser(String jser);

	@AboutConfig
	String getPass();

	@DescriptionForConfigEntry("Password ResolutorCaptcha")
	void setPass(String jser);

	@AboutConfig
	@DefaultBooleanValue(false)
	@DescriptionForConfigEntry("Activate account ResolutorCaptcha")
	boolean isEnabled();

	void setEnabled(boolean b);
}
