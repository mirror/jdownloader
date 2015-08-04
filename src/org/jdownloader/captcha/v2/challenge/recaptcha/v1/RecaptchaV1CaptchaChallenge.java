package org.jdownloader.captcha.v2.challenge.recaptcha.v1;

import java.io.File;

import jd.plugins.Plugin;

import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;

public class RecaptchaV1CaptchaChallenge extends BasicCaptchaChallenge {

    public RecaptchaV1CaptchaChallenge(File file, String defaultValue, String explain, Plugin plg, int flag) {
        super("recaptcha", file, defaultValue, explain, plg, flag);
    }

}
