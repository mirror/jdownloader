package org.jdownloader.captcha.v2.solver.gui;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.multiclickcaptcha.MultiClickCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.multiclickcaptcha.MultiClickedPoint;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.MultiClickCaptchaResponse;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.settings.advanced.AdvancedConfigManager;
import jd.controlling.captcha.CaptchaSettings;
import jd.controlling.captcha.MultiClickCaptchaDialogHandler;
import jd.controlling.captcha.SkipException;

public class DialogMultiClickCaptchaSolver extends AbstractDialogSolver<MultiClickedPoint> {

    private CaptchaSettings                config;
    private MultiClickCaptchaDialogHandler handler;

    private DialogMultiClickCaptchaSolver() {
        super(1);
        config = JsonConfig.create(CaptchaSettings.class);

        AdvancedConfigManager.getInstance().register(JsonConfig.create(DialogCaptchaSolverConfig.class));
    }

    private static final DialogMultiClickCaptchaSolver INSTANCE = new DialogMultiClickCaptchaSolver();

    public static DialogMultiClickCaptchaSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        return super.canHandle(c);
    }

    public void enqueue(SolverJob<MultiClickedPoint> solverJob) {
        if (solverJob.getChallenge() instanceof MultiClickCaptchaChallenge) {
            super.enqueue(solverJob);
        }

    }

    public void requestFocus(Challenge<?> challenge) {
        MultiClickCaptchaDialogHandler hndlr = handler;
        if (hndlr != null) {
            hndlr.requestFocus();
        }
    }

    @Override
    public void solve(SolverJob<MultiClickedPoint> solverJob) throws InterruptedException, SkipException {
        synchronized (DialogBasicCaptchaSolver.getInstance()) {
            if (solverJob.isDone()) {
                return;
            }
            if (solverJob.getChallenge() instanceof MultiClickCaptchaChallenge) {
                solverJob.getLogger().info("Waiting for JAC (Click/Mouse)");
                solverJob.waitFor(9, JACSolver.getInstance());
                solverJob.getLogger().info("JAC (Click/Mouse) is done. Response so far: " + solverJob.getResponse());
                checkSilentMode(solverJob);
                MultiClickCaptchaChallenge captchaChallenge = (MultiClickCaptchaChallenge) solverJob.getChallenge();
                checkInterruption();
                handler = new MultiClickCaptchaDialogHandler(captchaChallenge);

                handler.run();

                if (handler.getPoint() != null) {
                    solverJob.addAnswer(new MultiClickCaptchaResponse(captchaChallenge, this, handler.getPoint(), 100));
                }
            }
        }

    }

}
