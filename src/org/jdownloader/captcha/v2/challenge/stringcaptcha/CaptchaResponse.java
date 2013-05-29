package org.jdownloader.captcha.v2.challenge.stringcaptcha;

import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;

public class CaptchaResponse extends AbstractResponse<String> {

    public CaptchaResponse(Challenge<String> challenge, Object solver, String captchaCode, int priority) {
        super(challenge, solver, priority, captchaCode);
    }

}
