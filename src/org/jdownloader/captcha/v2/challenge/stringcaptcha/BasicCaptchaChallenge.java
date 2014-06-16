package org.jdownloader.captcha.v2.challenge.stringcaptcha;

import java.io.File;

import jd.plugins.Plugin;

import org.jdownloader.captcha.v2.solverjob.ResponseList;

public abstract class BasicCaptchaChallenge extends ImageCaptchaChallenge<String> {

    public BasicCaptchaChallenge(final String method, final File file, final String defaultValue, final String explain, Plugin plugin, int flag) {
        super(file, method, explain, plugin);

    }

    public boolean isSolved() {
        final ResponseList<String> results = getResult();
        return results != null && results.getValue() != null;
    }

}
