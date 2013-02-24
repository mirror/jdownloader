package org.jdownloader.captcha.v2.solver.gui;

import jd.controlling.captcha.BasicCaptchaDialogQueueEntry;
import jd.controlling.captcha.CaptchaDialogQueue;
import jd.controlling.captcha.CaptchaSettings;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.SolverJob;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;

public class DialogBasicCaptchaSolver implements ChallengeSolver<String> {
    private CaptchaSettings                       config;
    private static final DialogBasicCaptchaSolver INSTANCE = new DialogBasicCaptchaSolver();

    public static DialogBasicCaptchaSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    private DialogBasicCaptchaSolver() {
        config = JsonConfig.create(CaptchaSettings.class);
    }

    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public void solve(SolverJob<String> solverJob) {
        try {
            if (solverJob.getChallenge() instanceof BasicCaptchaChallenge) {
                solverJob.waitFor(JACSolver.getInstance());
                if (solverJob.isSolved()) return;
                BasicCaptchaChallenge captchaChallenge = (BasicCaptchaChallenge) solverJob.getChallenge();
                BasicCaptchaDialogQueueEntry queue = new BasicCaptchaDialogQueueEntry(captchaChallenge);
                CaptchaDialogQueue.getInstance().addWait(queue);
                if (StringUtils.isNotEmpty(queue.getCaptchaCode())) {
                    solverJob.addAnswer(new CaptchaResponse(queue.getCaptchaCode(), 100));
                }
            }
        } catch (final Exception e) {
            return;
        }
    }

}
