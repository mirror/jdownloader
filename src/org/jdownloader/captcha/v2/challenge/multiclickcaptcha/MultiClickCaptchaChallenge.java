package org.jdownloader.captcha.v2.challenge.multiclickcaptcha;

import java.io.File;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.MultiClickCaptchaResponse;
import jd.plugins.Plugin;

public class MultiClickCaptchaChallenge extends ImageCaptchaChallenge<MultiClickedPoint> {

    public MultiClickCaptchaChallenge(File imagefile, String explain, Plugin plugin) {
        super(imagefile, plugin.getHost(), explain, plugin);

    }

    @Override
    public AbstractResponse<MultiClickedPoint> parseAPIAnswer(String result, String resultFormat, ChallengeSolver<?> solver) {
        MultiClickedPoint res = JSonStorage.restoreFromString(result, new TypeRef<MultiClickedPoint>() {
        });

        return new MultiClickCaptchaResponse(this, solver, res, 100);

    }

    @Override
    public boolean isSolved() {
        return this.getResult() != null;
    }

}
