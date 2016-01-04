package org.jdownloader.captcha.v2.solver.captchasolutions;

import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;

public class CaptchaSolutionsResponse extends CaptchaResponse {

    private String captchaID;

    public String getCaptchaID() {
        return captchaID;
    }

    public CaptchaSolutionsResponse(BasicCaptchaChallenge challenge, CaptchaSolutionsSolver solver, String id, String text) {
        super(challenge, solver, text, 100);
        this.captchaID = id;

    }

}
