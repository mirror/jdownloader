package org.jdownloader.captcha.v2.challenge.clickcaptcha;

import java.io.File;

import jd.plugins.Plugin;

import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;

public abstract class ClickCaptchaChallenge extends ImageCaptchaChallenge<ClickedPoint> {

    public ClickCaptchaChallenge(File imagefile, String explain, Plugin plugin) {
        super(imagefile, plugin.getHost(), explain, plugin);

    }

    @Override
    public boolean isSolved() {
        return this.getResult() != null;
    }

}
