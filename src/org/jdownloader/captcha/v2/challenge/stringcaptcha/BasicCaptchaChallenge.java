package org.jdownloader.captcha.v2.challenge.stringcaptcha;

import java.io.File;

import jd.plugins.Plugin;

import org.appwork.utils.StringUtils;

public abstract class BasicCaptchaChallenge extends ImageCaptchaChallenge<String> {

    public BasicCaptchaChallenge(final String method, final File file, final String defaultValue, final String explain, Plugin plugin, int flag) {
        super(file, method, explain, plugin);

    }

    public boolean isSolved() {
        if (getResult() == null) return false;
        return StringUtils.isNotEmpty(getResult().getValue());
    }

}
