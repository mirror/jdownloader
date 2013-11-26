package org.jdownloader.captcha.v2.solver.gui;

import javax.swing.Icon;

import jd.controlling.captcha.CaptchaSettings;
import jd.controlling.captcha.ClickCaptchaDialogHandler;
import jd.controlling.captcha.SkipException;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ClickCaptchaResponse;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSettings;
import org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolverClick;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;

public class DialogClickCaptchaSolver extends AbstractDialogSolver<ClickedPoint> {
    private CaptchaSettings           config;
    private Captcha9kwSettings        config9kw;
    private ClickCaptchaDialogHandler handler;

    private DialogClickCaptchaSolver() {
        super(1);
        config = JsonConfig.create(CaptchaSettings.class);
        config9kw = JsonConfig.create(Captcha9kwSettings.class);
    }

    @Override
    public Icon getIcon(int size) {
        return NewTheme.I().getIcon(IconKey.ICON_OCR, size);
    }

    private static final DialogClickCaptchaSolver INSTANCE = new DialogClickCaptchaSolver();

    public static DialogClickCaptchaSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        return CFG_CAPTCHA.CAPTCHA_DIALOGS_ENABLED.isEnabled() && super.canHandle(c);
    }

    public void enqueue(SolverJob<ClickedPoint> solverJob) {
        if (solverJob.getChallenge() instanceof ClickCaptchaChallenge) {
            super.enqueue(solverJob);
        }

    }

    @Override
    public String getName() {
        return "Dialog";
    }

    public void requestFocus(Challenge<?> challenge) {
        ClickCaptchaDialogHandler hndlr = handler;
        if (hndlr != null) {
            hndlr.requestFocus();
        }
    }

    /**
     * returns true if the dialog solver waits for invisible solvers like captcha exchange services
     * 
     * @return
     */
    public boolean hasToWaitForInvisibleSolvers() {

        if (config9kw.isEnabled() && config.getCaptchaDialog9kwTimeout() > 0) return true;

        return false;
    }

    @Override
    public void solve(SolverJob<ClickedPoint> solverJob) throws InterruptedException, SkipException {
        synchronized (DialogBasicCaptchaSolver.getInstance()) {
            if (solverJob.getChallenge() instanceof ClickCaptchaChallenge && CFG_CAPTCHA.CAPTCHA_DIALOGS_ENABLED.isEnabled()) {
                solverJob.getLogger().info("Waiting for JAC (Click/Mouse)");
                solverJob.waitFor(config.getCaptchaDialogJAntiCaptchaTimeout(), JACSolver.getInstance());
                // StringUtils.isEmpty(config.getApiKey())
                if (config9kw.ismouse() && config.getCaptchaDialog9kwTimeout() > 0) solverJob.waitFor(config.getCaptchaDialog9kwTimeout(), Captcha9kwSolverClick.getInstance());

                solverJob.getLogger().info("JAC (Click/Mouse) is done. Response so far: " + solverJob.getResponse());
                checkSilentMode(solverJob);
                ClickCaptchaChallenge captchaChallenge = (ClickCaptchaChallenge) solverJob.getChallenge();
                checkInterruption();
                handler = new ClickCaptchaDialogHandler(captchaChallenge);

                handler.run();

                if (handler.getPoint() != null) {
                    solverJob.addAnswer(new ClickCaptchaResponse(captchaChallenge, this, handler.getPoint(), 100));
                }
            }
        }

    }

}
