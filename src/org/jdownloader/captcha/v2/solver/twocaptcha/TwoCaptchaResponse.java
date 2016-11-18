package org.jdownloader.captcha.v2.solver.twocaptcha;

import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;

public class TwoCaptchaResponse extends CaptchaResponse {

    private String captchaID;

    public String getCaptchaID() {
        return captchaID;
    }

    public TwoCaptchaResponse(BasicCaptchaChallenge challenge, TwoCaptchaSolver solver, String id, String text) {
        super(challenge, solver, text, 100);
        this.captchaID = id;

    }

}
