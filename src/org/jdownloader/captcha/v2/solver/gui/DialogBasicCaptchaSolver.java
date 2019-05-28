package org.jdownloader.captcha.v2.solver.gui;

import jd.controlling.captcha.BasicCaptchaDialogHandler;
import jd.controlling.captcha.CaptchaSettings;
import jd.controlling.captcha.SkipException;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solverjob.ChallengeSolverJobListener;
import org.jdownloader.captcha.v2.solverjob.ResponseList;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class DialogBasicCaptchaSolver extends AbstractDialogSolver<String> {
    private CaptchaSettings                       config;
    private BasicCaptchaDialogHandler             handler;
    private Thread                                waitingThread;
    private boolean                               focusRequested;
    private static final DialogBasicCaptchaSolver INSTANCE = new DialogBasicCaptchaSolver();

    public static DialogBasicCaptchaSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    @Override
    protected boolean isChallengeSupported(Challenge<?> c) {
        return c instanceof BasicCaptchaChallenge;
    }

    private DialogBasicCaptchaSolver() {
        super(1);
        config = JsonConfig.create(CaptchaSettings.class);
        AdvancedConfigManager.getInstance().register(JsonConfig.create(DialogCaptchaSolverConfig.class));
    }

    @Override
    public void solve(final SolverJob<String> job) throws InterruptedException, SkipException {
        synchronized (this) {
            if (job.isDone()) {
                return;
            }
            if (job.getChallenge() instanceof BasicCaptchaChallenge) {
                BasicCaptchaChallenge captchaChallenge = (BasicCaptchaChallenge) job.getChallenge();
                String result = solveBasicCaptchaChallenge(job, captchaChallenge);
                if (result != null) {
                    job.addAnswer(new CaptchaResponse(captchaChallenge, this, result, 100));
                }
            }
        }
    }

    /**
     * @param job
     * @param captchaChallenge
     * @return
     * @throws InterruptedException
     * @throws SkipException
     */
    public String solveBasicCaptchaChallenge(final SolverJob<String> job, BasicCaptchaChallenge captchaChallenge) throws InterruptedException, SkipException {
        job.getLogger().info("Waiting for Other Solvers");
        try {
            focusRequested = false;
            waitingThread = Thread.currentThread();
            job.waitFor(9, JACSolver.getInstance());
        } catch (InterruptedException e) {
            e.printStackTrace();
            if (!focusRequested) {
                throw e;
            }
        } finally {
            waitingThread = null;
            focusRequested = false;
        }
        checkInterruption();
        job.getLogger().info("Waits are done. Response so far: " + job.getResponse());
        ChallengeSolverJobListener jacListener = null;
        checkSilentMode(job);
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
                return null;
            }
            handler.run();
            return handler.getCaptchaCode();
        } finally {
            job.getLogger().info("Dialog closed. Response far: " + job.getResponse());
            if (jacListener != null) {
                job.getEventSender().removeListener(jacListener);
            }
            handler = null;
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
