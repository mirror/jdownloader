package org.jdownloader.captcha.v2.challenge.stringcaptcha;

import org.jdownloader.captcha.v2.AbstractResponse;

public class CaptchaResponse extends AbstractResponse<String> {

    public CaptchaResponse(Object solver, String captchaCode, int priority) {
        super(solver, priority, captchaCode);
    }

}
