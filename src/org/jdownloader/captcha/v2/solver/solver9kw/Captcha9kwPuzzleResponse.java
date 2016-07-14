package org.jdownloader.captcha.v2.solver.solver9kw;

import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaResponse;

public class Captcha9kwPuzzleResponse extends KeyCaptchaResponse implements Captcha9KWResponseInterface {
    private final String Captcha9kwID;

    public Captcha9kwPuzzleResponse(Challenge<String> captchaChallenge, ChallengeSolver<?> solver, String captchaCode, int priority, final String captcha9kwID) {
        super(captchaChallenge, solver, captchaCode, priority);
        this.Captcha9kwID = captcha9kwID;
    }

    @Override
    public String getCaptcha9kwID() {
        return Captcha9kwID;
    }
}
