package org.jdownloader.captcha.v2.solver.dbc;

import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;

public class DeathByCaptchaResponse extends CaptchaResponse {
    private DBCUploadResponse captcha;

    public DBCUploadResponse getCaptcha() {
        return captcha;
    }

    public DeathByCaptchaResponse(Challenge<String> challenge, DeathByCaptchaSolver deathByCaptchaSolver, DBCUploadResponse captcha, String value, int priority) {
        super(challenge, deathByCaptchaSolver, value, priority);
        this.captcha = captcha;
    }
}
