package org.jdownloader.captcha.v2.solver;

import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;

public class Captcha9kwResponse extends CaptchaResponse {

    private final String Captcha9kwID;

    public Captcha9kwResponse(Challenge<String> challenge, Object solver, String captchaCode, int priority, final String captcha9kwID) {
        super(challenge, solver, captchaCode, priority);
        this.Captcha9kwID = captcha9kwID;
    }

    /**
     * @return the captcha9kwID
     */
    public String getCaptcha9kwID() {
        return Captcha9kwID;
    }

}
