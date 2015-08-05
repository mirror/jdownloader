package org.jdownloader.captcha.v2.challenge.keycaptcha;

import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;

public class KeyCaptchaResponse extends CaptchaResponse {

    public KeyCaptchaResponse(Challenge<String> captchaChallenge, ChallengeSolver<?> solver, String captchaCode, int priority) {
        super(captchaChallenge, solver, captchaCode, priority);

    }

}
