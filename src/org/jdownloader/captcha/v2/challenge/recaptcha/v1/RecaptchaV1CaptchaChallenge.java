package org.jdownloader.captcha.v2.challenge.recaptcha.v1;

import java.io.File;

import jd.plugins.Plugin;

import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;

public class RecaptchaV1CaptchaChallenge extends BasicCaptchaChallenge {
    public final static String RECAPTCHAV1 = "recaptcha";

    public RecaptchaV1CaptchaChallenge(File file, String defaultValue, String explain, Plugin plg, int flag) {
        super(RECAPTCHAV1, file, defaultValue, explain, plg, flag);
    }
}
