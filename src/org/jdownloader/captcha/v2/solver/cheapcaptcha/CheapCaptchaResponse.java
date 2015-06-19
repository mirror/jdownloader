package org.jdownloader.captcha.v2.solver.cheapcaptcha;

import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;

public class CheapCaptchaResponse extends CaptchaResponse {

    private String captchaID;

    public String getCaptchaID() {
        return captchaID;
    }

    public CheapCaptchaResponse(BasicCaptchaChallenge challenge, CheapCaptchaSolver solver, String id, String text) {
        super(challenge, solver, text, 100);
        this.captchaID = id;

    }

}
