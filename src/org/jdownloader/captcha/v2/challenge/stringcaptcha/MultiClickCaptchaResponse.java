package org.jdownloader.captcha.v2.challenge.stringcaptcha;

import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.multiclickcaptcha.MultiClickedPoint;

public class MultiClickCaptchaResponse extends AbstractResponse<MultiClickedPoint> {

    public MultiClickCaptchaResponse(Challenge<MultiClickedPoint> challenge, Object solver, MultiClickedPoint captchaCode, int priority) {
        super(challenge, solver, priority, captchaCode);
    }

}
