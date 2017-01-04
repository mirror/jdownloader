package org.jdownloader.captcha.v2.solver.captchasolutions;

import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;

public class CaptchaSolutionsResponse extends CaptchaResponse {
    private String captchaID;

    public String getCaptchaID() {
        return captchaID;
    }

    public CaptchaSolutionsResponse(Challenge<String> challenge, CaptchaSolutionsSolver solver, String id, String text, int priority) {
        super(challenge, solver, text, priority);
        this.captchaID = id;
    }
}
