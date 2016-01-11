package org.jdownloader.captcha.v2.solver.gui;

import org.appwork.utils.Application;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.RecaptchaV2Challenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.RecaptchaV2Challenge.Recaptcha2FallbackChallenge;
import org.jdownloader.captcha.v2.solver.browser.BrowserSolver;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

import jd.controlling.captcha.SkipException;

public class RecaptchaChooseFrom3x3Solver extends AbstractDialogSolver<String> {

    private static final RecaptchaChooseFrom3x3Solver INSTANCE = new RecaptchaChooseFrom3x3Solver();

    public static RecaptchaChooseFrom3x3Solver getInstance() {
        return INSTANCE;
    }

    private RecaptchaChooseFrom3x3DialogHandler handler;

    private RecaptchaChooseFrom3x3Solver() {
        super(1);
        // AdvancedConfigManager.getInstance().register(BrowserSolverService.getInstance().getConfig());
    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        if (!validateBlackWhite(c)) {
            return false;
        }
        if (isBrowserSolverEnabled(c)) {
            return false;
        }
        if (c instanceof RecaptchaV2Challenge || c instanceof Recaptcha2FallbackChallenge) {

            return true;
        }
        return false;
    }

    private boolean isBrowserSolverEnabled(Challenge<?> c) {
        if (!BrowserSolver.getInstance().isEnabled()) {
            return false;
        }
        if (Application.isHeadless()) {
            return false;
        }
        if (!BrowserSolver.getInstance().validateBlackWhite(c)) {
            return false;
        }
        return true;
    }

    @Override
    public void solve(SolverJob<String> solverJob) throws InterruptedException, SolverException, SkipException {

        synchronized (DialogBasicCaptchaSolver.getInstance()) {
            if (solverJob.isDone()) {
                return;
            }
            if (isBrowserSolverEnabled(solverJob.getChallenge())) {
                return;
            }
            if (solverJob.getChallenge() instanceof RecaptchaV2Challenge) {

                checkSilentMode(solverJob);
                Recaptcha2FallbackChallenge captchaChallenge = (Recaptcha2FallbackChallenge) ((RecaptchaV2Challenge) solverJob.getChallenge()).createBasicCaptchaChallenge();
                checkInterruption();
                handler = new RecaptchaChooseFrom3x3DialogHandler(captchaChallenge);

                handler.run();

                if (handler.getResult() != null) {
                    solverJob.addAnswer(new AbstractResponse<String>(captchaChallenge, this, 100, handler.getResult()));
                }
            } else if (solverJob.getChallenge() instanceof Recaptcha2FallbackChallenge) {

                checkSilentMode(solverJob);
                Recaptcha2FallbackChallenge captchaChallenge = (Recaptcha2FallbackChallenge) solverJob.getChallenge();
                checkInterruption();
                handler = new RecaptchaChooseFrom3x3DialogHandler(captchaChallenge);

                handler.run();

                if (handler.getResult() != null) {
                    solverJob.addAnswer(new AbstractResponse<String>(captchaChallenge, this, 100, handler.getResult()));
                }
            }
        }

    }

}
