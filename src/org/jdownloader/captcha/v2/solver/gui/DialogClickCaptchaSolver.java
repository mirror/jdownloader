package org.jdownloader.captcha.v2.solver.gui;

import jd.controlling.captcha.CaptchaDialogQueue;
import jd.controlling.captcha.CaptchaSettings;
import jd.controlling.captcha.ClickCaptchaDialogQueueEntry;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.SolverJob;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;

public class DialogClickCaptchaSolver implements ChallengeSolver<ClickedPoint> {
    private CaptchaSettings config;

    private DialogClickCaptchaSolver() {
        config = JsonConfig.create(CaptchaSettings.class);
    }

    private static final DialogClickCaptchaSolver INSTANCE = new DialogClickCaptchaSolver();

    public static DialogClickCaptchaSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<ClickedPoint> getResultType() {
        return ClickedPoint.class;
    }

    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public void solve(SolverJob<ClickedPoint> solverJob) {
        try {
            if (solverJob.getChallenge() instanceof ClickCaptchaChallenge) {
                solverJob.waitFor(JACSolver.getInstance());
                ClickCaptchaChallenge captchaChallenge = (ClickCaptchaChallenge) solverJob.getChallenge();

                ClickCaptchaDialogQueueEntry queue = new ClickCaptchaDialogQueueEntry(captchaChallenge);
                CaptchaDialogQueue.getInstance().addWait(queue);
            }
        } catch (final Exception e) {
            return;
        }
    }

}
