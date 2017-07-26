package org.jdownloader.captcha.v2.solver.gui;

import jd.controlling.captcha.SkipException;

import org.appwork.utils.Application;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptcha2FallbackChallenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.RecaptchaV2Challenge;
import org.jdownloader.captcha.v2.solver.browser.BrowserSolver;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.plugins.SkipReason;

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
        if (isBrowserSolverEnabled(c) && !(c instanceof AbstractRecaptcha2FallbackChallenge)) {
            return false;
        }
        if (c instanceof RecaptchaV2Challenge || c instanceof AbstractRecaptcha2FallbackChallenge) {
            return true;
        }
        return false;
    }

    private boolean isBrowserSolverEnabled(Challenge<?> c) {
        // try {
        // not yet
        // if (!CFG_BROWSER_CAPTCHA_SOLVER.CFG.isRecaptcha2Enabled()) {
        // return false;
        // }
        // if (CFG_GENERAL.CFG.isJxBrowserEnabled() && c.getClass().getSimpleName().equals("Recaptcha2FallbackChallengeViaJxBrowser")) {
        // return false;
        // }
        // if (CFG_GENERAL.CFG.isJxBrowserEnabled() && c instanceof RecaptchaV2Challenge) {
        // // Load via reflection until evaluation tests are done
        //
        // Class.forName("com.teamdev.jxbrowser.chromium.Browser");
        // if (((RecaptchaV2Challenge) c).createBasicCaptchaChallenge() != null) {
        // return false;
        // }
        // }
        // } catch (ClassNotFoundException e) {
        // // e.printStackTrace();
        // }
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
            if (!canHandle(solverJob.getChallenge())) {
                return;
            }
            Challenge<?> challenge = solverJob.getChallenge();
            if (challenge instanceof RecaptchaV2Challenge) {
                checkSilentMode(solverJob);
                AbstractRecaptcha2FallbackChallenge captchaChallenge = (AbstractRecaptcha2FallbackChallenge) ((RecaptchaV2Challenge) solverJob.getChallenge()).createBasicCaptchaChallenge(true);
                if (captchaChallenge == null) {
                    throw new SolverException(SkipReason.PHANTOM_JS_MISSING.getExplanation(null));
                }
                checkInterruption();
                handler = new RecaptchaChooseFrom3x3DialogHandler(captchaChallenge);
                handler.run();
                if (handler.getResult() != null) {
                    solverJob.addAnswer(new AbstractResponse<String>(captchaChallenge, this, 100, handler.getResult()));
                }
            } else if (solverJob.getChallenge() instanceof AbstractRecaptcha2FallbackChallenge) {
                checkSilentMode(solverJob);
                AbstractRecaptcha2FallbackChallenge captchaChallenge = (AbstractRecaptcha2FallbackChallenge) solverJob.getChallenge();
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
