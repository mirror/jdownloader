package org.jdownloader.captcha.v2.solver.solver9kw;

import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ClickCaptchaResponse;

public class Captcha9kwClickResponse extends ClickCaptchaResponse {

    private final String Captcha9kwID;

    public Captcha9kwClickResponse(Challenge<ClickedPoint> challenge, Object solver, ClickedPoint captchaCode, int priority, final String captcha9kwID) {
        super(challenge, solver, captchaCode, priority);
        this.Captcha9kwID = captcha9kwID;
    }

    public String getCaptcha9kwID() {
        return Captcha9kwID;
    }
}
