package org.jdownloader.captcha.v2.solver.endcaptcha;

import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;

public class EndCaptchaResponse extends CaptchaResponse {

    private String captchaID;

    public String getCaptchaID() {
        return captchaID;
    }

    public EndCaptchaResponse(BasicCaptchaChallenge challenge, EndCaptchaSolver solver, String id, String text) {
        super(challenge, solver, text, 100);
        this.captchaID = id;

    }

}
