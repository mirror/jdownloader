package org.jdownloader.captcha.v2.solver.gui;

import jd.controlling.captcha.CaptchaSettings;
import jd.controlling.captcha.ClickCaptchaDialogHandler;
import jd.controlling.captcha.SkipException;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ClickCaptchaResponse;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class DialogClickCaptchaSolver extends ChallengeSolver<ClickedPoint> {
    private CaptchaSettings config;

    private DialogClickCaptchaSolver() {
        super(1);
        config = JsonConfig.create(CaptchaSettings.class);
    }

    private static final DialogClickCaptchaSolver INSTANCE = new DialogClickCaptchaSolver();

    public static DialogClickCaptchaSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public void solve(SolverJob<ClickedPoint> solverJob) throws InterruptedException, SkipException {
        synchronized (DialogBasicCaptchaSolver.getInstance()) {
            if (solverJob.getChallenge() instanceof ClickCaptchaChallenge) {
                solverJob.waitFor(config.getCaptchaDialogJAntiCaptchaTimeout(), JACSolver.getInstance());
                checkInterruption();
                ClickCaptchaChallenge captchaChallenge = (ClickCaptchaChallenge) solverJob.getChallenge();
                checkInterruption();
                ClickCaptchaDialogHandler handler = new ClickCaptchaDialogHandler(captchaChallenge);

                handler.run();

                if (handler.getPoint() != null) {
                    solverJob.addAnswer(new ClickCaptchaResponse(this, handler.getPoint(), 100));
                }
            }
        }

    }

}
