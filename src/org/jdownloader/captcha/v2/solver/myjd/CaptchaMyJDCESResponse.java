package org.jdownloader.captcha.v2.solver.myjd;

import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;
import org.jdownloader.myjdownloader.client.json.MyCaptchaSolution;

public class CaptchaMyJDCESResponse extends CaptchaResponse {

    private MyCaptchaSolution solution;

    public MyCaptchaSolution getSolution() {
        return solution;
    }

    public CaptchaMyJDCESResponse(Challenge<String> challenge, Object solver, String captchaCode, int priority, MyCaptchaSolution solution) {
        super(challenge, solver, captchaCode, priority);
        this.solution = solution;

    }

}
