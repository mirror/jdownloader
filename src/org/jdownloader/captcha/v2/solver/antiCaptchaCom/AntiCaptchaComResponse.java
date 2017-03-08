package org.jdownloader.captcha.v2.solver.antiCaptchaCom;

import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;

public class AntiCaptchaComResponse extends CaptchaResponse {
    private int taskId;

    public int getTaskId() {
        return taskId;
    }

    public AntiCaptchaComResponse(Challenge<String> challenge, AntiCaptchaComSolver solver, int id, String text) {
        super(challenge, solver, text, 100);
        this.taskId = id;
    }
}
