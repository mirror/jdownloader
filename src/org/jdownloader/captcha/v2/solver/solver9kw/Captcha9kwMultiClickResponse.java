package org.jdownloader.captcha.v2.solver.solver9kw;

import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.multiclickcaptcha.MultiClickedPoint;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.MultiClickCaptchaResponse;

public class Captcha9kwMultiClickResponse extends MultiClickCaptchaResponse implements Captcha9KWResponseInterface {
    private final String Captcha9kwID;

    public Captcha9kwMultiClickResponse(Challenge<MultiClickedPoint> challenge, Object solver, MultiClickedPoint captchaCode, int priority, final String captcha9kwID) {
        super(challenge, solver, captchaCode, priority);
        this.Captcha9kwID = captcha9kwID;
    }

    @Override
    public String getCaptcha9kwID() {
        return Captcha9kwID;
    }
}
