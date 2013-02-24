package org.jdownloader.captcha.v2.challenge.stringcaptcha;

import org.jdownloader.captcha.v2.AbstractResponse;

public class CaptchaResponse extends AbstractResponse<String> {

    public CaptchaResponse(String captchaCode, int priority) {
        super(priority, captchaCode);
    }

}
