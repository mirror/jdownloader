package org.jdownloader.captcha.v2.challenge.clickcaptcha;

import java.io.File;

import jd.plugins.Plugin;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ClickCaptchaResponse;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;

public class ClickCaptchaChallenge extends ImageCaptchaChallenge<ClickedPoint> {

    public ClickCaptchaChallenge(File imagefile, String explain, Plugin plugin) {
        super(imagefile, plugin.getHost(), explain, plugin);

    }

    @Override
    public AbstractResponse<ClickedPoint> parseAPIAnswer(String result, String resultFormat, ChallengeSolver<?> solver) {
        ClickedPoint res = JSonStorage.restoreFromString(result, new TypeRef<ClickedPoint>() {
        });

        return new ClickCaptchaResponse(this, solver, res, 100);

    }

    @Override
    public boolean isSolved() {
        return this.getResult() != null;
    }

}
