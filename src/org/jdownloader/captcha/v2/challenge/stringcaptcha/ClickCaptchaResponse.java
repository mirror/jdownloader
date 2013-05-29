package org.jdownloader.captcha.v2.challenge.stringcaptcha;

import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;

public class ClickCaptchaResponse extends AbstractResponse<ClickedPoint> {

    public ClickCaptchaResponse(Challenge<ClickedPoint> challenge, Object solver, ClickedPoint captchaCode, int priority) {
        super(challenge, solver, priority, captchaCode);
    }

}
