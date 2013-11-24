package org.jdownloader.captcha.v2.solver.gui;

import javax.swing.Icon;

import jd.controlling.captcha.BasicCaptchaDialogHandler;
import jd.controlling.captcha.CaptchaSettings;
import jd.controlling.captcha.SkipException;
import jd.controlling.captcha.SkipRequest;
import jd.gui.swing.jdgui.JDGui;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;
import org.jdownloader.captcha.v2.solver.captchabrotherhood.CBSolver;
import org.jdownloader.captcha.v2.solver.captchabrotherhood.CaptchaBrotherHoodSettings;
import org.jdownloader.captcha.v2.solver.captcharesolutor.CaptchaResolutorCaptchaSettings;
import org.jdownloader.captcha.v2.solver.captcharesolutor.CaptchaResolutorCaptchaSolver;
import org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSettings;
import org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSolver;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSettings;
import org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver;
import org.jdownloader.captcha.v2.solverjob.ChallengeSolverJobListener;
import org.jdownloader.captcha.v2.solverjob.ResponseList;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;
import org.jdownloader.settings.staticreferences.CFG_SILENTMODE;

public class DialogBasicCaptchaSolver extends ChallengeSolver<String> {
    private CaptchaSettings                       config;
    private Captcha9kwSettings                    config9kw;
    private CaptchaBrotherHoodSettings            configcbh;
    private CaptchaResolutorCaptchaSettings       configresolutor;
    private BasicCaptchaDialogHandler             handler;
    private DeathByCaptchaSettings                configDBC;
    private Thread                                waitingThread;
    private boolean                               focusRequested;
    private static final DialogBasicCaptchaSolver INSTANCE = new DialogBasicCaptchaSolver();

    @Override
    public Icon getIcon(int size) {
        return NewTheme.I().getIcon(IconKey.ICON_OCR, size);
    }

    public static DialogBasicCaptchaSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    public void enqueue(SolverJob<String> job) {
        if (job.getChallenge() instanceof BasicCaptchaChallenge) {
            super.enqueue(job);
        }
    }

    @Override
    public String getName() {
        return "Dialog";
    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        return CFG_CAPTCHA.CAPTCHA_DIALOGS_ENABLED.isEnabled() && super.canHandle(c);
    }

    private DialogBasicCaptchaSolver() {
        super(1);
        config = JsonConfig.create(CaptchaSettings.class);
        config9kw = JsonConfig.create(Captcha9kwSettings.class);
        configDBC = JsonConfig.create(DeathByCaptchaSettings.class);
        configcbh = JsonConfig.create(CaptchaBrotherHoodSettings.class);
        configresolutor = JsonConfig.create(CaptchaResolutorCaptchaSettings.class);
    }

    /**
     * returns true if the dialog solver waits for invisible solvers like captcha exchange services
     * 
     * @return
     */
    public boolean hasToWaitForInvisibleSolvers() {
        if (configDBC.isEnabled() && config.getCaptchaDialogDBCTimeout() > 0) return true;
        if (config9kw.isEnabled() && config.getCaptchaDialog9kwTimeout() > 0) return true;
        if (configcbh.isEnabled() && config.getCaptchaDialogCaptchaBrotherhoodTimeout() > 0) return true;
        if (configresolutor.isEnabled() && config.getCaptchaDialogResolutorCaptchaTimeout() > 0) return true;
        return false;
    }

    @Override
    public void solve(final SolverJob<String> job) throws InterruptedException, SkipException {
        synchronized (this) {

            if (job.getChallenge() instanceof BasicCaptchaChallenge && CFG_CAPTCHA.CAPTCHA_DIALOGS_ENABLED.isEnabled()) {
                job.getLogger().info("Waiting for Other Solvers");
                try {
                    focusRequested = false;
                    waitingThread = Thread.currentThread();
                    job.waitFor(config.getCaptchaDialogJAntiCaptchaTimeout(), JACSolver.getInstance());
                    if (configDBC.isEnabled() && config.getCaptchaDialogDBCTimeout() > 0) job.waitFor(config.getCaptchaDialogDBCTimeout(), DeathByCaptchaSolver.getInstance());

                    if (config9kw.isEnabled() && config.getCaptchaDialog9kwTimeout() > 0) job.waitFor(config.getCaptchaDialog9kwTimeout(), Captcha9kwSolver.getInstance());
                    if (configcbh.isEnabled() && config.getCaptchaDialogCaptchaBrotherhoodTimeout() > 0) job.waitFor(config.getCaptchaDialogCaptchaBrotherhoodTimeout(), CBSolver.getInstance());
                    if (configresolutor.isEnabled() && config.getCaptchaDialogResolutorCaptchaTimeout() > 0) job.waitFor(config.getCaptchaDialogResolutorCaptchaTimeout(), CaptchaResolutorCaptchaSolver.getInstance());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                    if (!focusRequested) throw e;
                } finally {
                    waitingThread = null;
                    focusRequested = false;
                }
                checkInterruption();
                job.getLogger().info("Waits are done. Response so far: " + job.getResponse());
                ChallengeSolverJobListener jacListener = null;
                if (JDGui.getInstance().isSilentModeActive()) {
                    switch (CFG_SILENTMODE.CFG.getOnCaptchaDuringSilentModeAction()) {
                    case DEFAULT_DIALOG_HANDLING:
                        break;
                    case DISABLE_DIALOG_SOLVER:
                        return;
                    case SKIP_LINK:
                        throw new SkipException(SkipRequest.SINGLE);
                    }
                }
                checkInterruption();
                BasicCaptchaChallenge captchaChallenge = (BasicCaptchaChallenge) job.getChallenge();
                // we do not need another queue
                handler = new BasicCaptchaDialogHandler(captchaChallenge);
                job.getEventSender().addListener(jacListener = new ChallengeSolverJobListener() {

                    @Override
                    public void onSolverTimedOut(ChallengeSolver<?> parameter) {
                    }

                    @Override
                    public void onSolverStarts(ChallengeSolver<?> parameter) {
                    }

                    @Override
                    public void onSolverJobReceivedNewResponse(AbstractResponse<?> response) {
                        ResponseList<String> resp = job.getResponse();
                        handler.setSuggest(resp.getValue());
                        job.getLogger().info("Received Suggestion: " + resp);

                    }

                    @Override
                    public void onSolverDone(ChallengeSolver<?> solver) {

                    }
                });
                try {
                    ResponseList<String> resp = job.getResponse();
                    if (resp != null) {
                        handler.setSuggest(resp.getValue());
                    }
                    checkInterruption();
                    if (!captchaChallenge.getImageFile().exists()) {

                        job.getLogger().info("Cannot solve. image does not exist");
                        return;
                    }

                    handler.run();

                    if (StringUtils.isNotEmpty(handler.getCaptchaCode())) {
                        job.addAnswer(new CaptchaResponse(captchaChallenge, this, handler.getCaptchaCode(), 100));
                    }
                } finally {
                    job.getLogger().info("Dialog closed. Response far: " + job.getResponse());
                    if (jacListener != null) job.getEventSender().removeListener(jacListener);
                    handler = null;
                }
            }
        }

    }

    public void requestFocus(Challenge<?> challenge) {
        if (waitingThread != null) {
            focusRequested = true;
            waitingThread.interrupt();
        }
        BasicCaptchaDialogHandler hndlr = handler;
        if (hndlr != null) {
            hndlr.requestFocus();
        }
    }

}
