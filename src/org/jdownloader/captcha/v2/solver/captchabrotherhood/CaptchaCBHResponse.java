package org.jdownloader.captcha.v2.solver.captchabrotherhood;

import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;

public class CaptchaCBHResponse extends CaptchaResponse {

    private final String CaptchaCBHID;

    public CaptchaCBHResponse(Challenge<String> challenge, Object solver, String captchaCode, int priority, final String captchaCBHID) {
        super(challenge, solver, captchaCode, priority);
        this.CaptchaCBHID = captchaCBHID;
    }

    /**
     * @return the captchaCBHID
     */
    public String getCaptchaCBHID() {
        return CaptchaCBHID;
    }

}
