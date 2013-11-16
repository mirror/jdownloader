package org.jdownloader.captcha.v2.solver.dbc;

import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;
import org.jdownloader.captcha.v2.solver.dbc.api.Captcha;

public class DeathByCaptchaResponse extends CaptchaResponse {

    private Captcha captcha;

    public Captcha getCaptcha() {
        return captcha;
    }

    public DeathByCaptchaResponse(BasicCaptchaChallenge challenge, DeathByCaptchaSolver deathByCaptchaSolver, Captcha captcha) {
        super(challenge, deathByCaptchaSolver, captcha.text, 100);
        this.captcha = captcha;
    }

}
