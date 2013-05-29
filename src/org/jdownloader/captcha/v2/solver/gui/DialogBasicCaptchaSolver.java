package org.jdownloader.captcha.v2.solver.gui;

import jd.controlling.captcha.BasicCaptchaDialogHandler;
import jd.controlling.captcha.CaptchaSettings;
import jd.controlling.captcha.SkipException;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;
import org.jdownloader.captcha.v2.solver.CBSolver;
import org.jdownloader.captcha.v2.solver.Captcha9kwSolver;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solverjob.ChallengeSolverJobListener;
import org.jdownloader.captcha.v2.solverjob.ResponseList;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class DialogBasicCaptchaSolver extends ChallengeSolver<String> {
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
        super(1);
        config = JsonConfig.create(CaptchaSettings.class);
    }

    @Override
    public void solve(final SolverJob<String> job) throws InterruptedException, SkipException {
        synchronized (this) {

            if (job.getChallenge() instanceof BasicCaptchaChallenge) {
                job.getLogger().info("Waiting for JAC");
                job.waitFor(config.getCaptchaDialogJAntiCaptchaTimeout(), JACSolver.getInstance());

                if (config.getCaptchaDialog9kwTimeout() > 0) job.waitFor(config.getCaptchaDialog9kwTimeout(), Captcha9kwSolver.getInstance());
                if (config.getCaptchaDialogCaptchaBroptherhoodTimeout() > 0) job.waitFor(config.getCaptchaDialogCaptchaBroptherhoodTimeout(), CBSolver.getInstance());

                job.getLogger().info("JAC is done. Response so far: " + job.getResponse());
                ChallengeSolverJobListener jacListener = null;
                checkInterruption();
                BasicCaptchaChallenge captchaChallenge = (BasicCaptchaChallenge) job.getChallenge();
                // we do not need another queue
                final BasicCaptchaDialogHandler handler = new BasicCaptchaDialogHandler(captchaChallenge);
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
                }
            }
        }

    }

}
