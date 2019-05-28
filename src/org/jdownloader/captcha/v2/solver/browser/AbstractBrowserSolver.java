package org.jdownloader.captcha.v2.solver.browser;

import jd.controlling.captcha.SkipException;
import jd.controlling.captcha.SkipRequest;
import jd.gui.swing.jdgui.JDGui;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solver.gui.DialogBasicCaptchaSolver;
import org.jdownloader.captcha.v2.solver.service.BrowserSolverService;
import org.jdownloader.captcha.v2.solverjob.ChallengeSolverJobListener;
import org.jdownloader.captcha.v2.solverjob.ResponseList;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.settings.staticreferences.CFG_SILENTMODE;

public abstract class AbstractBrowserSolver extends ChallengeSolver<String> {
    protected final BrowserCaptchaSolverConfig config;
    private volatile BrowserDialogHandler      handler;

    public AbstractBrowserSolver(int i) {
        super(BrowserSolverService.getInstance(), i);
        config = BrowserSolverService.getInstance().getConfig();
        threadPool.allowCoreThreadTimeOut(true);
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    @Override
    protected boolean isChallengeSupported(Challenge<?> c) {
        return c instanceof AbstractBrowserChallenge;
    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        if (super.canHandle(c)) {
            final String[] browserCommandLine = BrowserSolverService.getInstance().getConfig().getBrowserCommandline();
            if (!CrossSystem.isOpenBrowserSupported() && (browserCommandLine == null || browserCommandLine.length == 0)) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public void checkSilentMode(final SolverJob<String> job) throws SkipException, InterruptedException {
        if (JDGui.getInstance().isSilentModeActive()) {
            switch (CFG_SILENTMODE.CFG.getOnCaptchaDuringSilentModeAction()) {
            case WAIT_IN_BACKGROUND_UNTIL_WINDOW_GETS_FOCUS_OR_TIMEOUT:
                break;
            case DISABLE_DIALOG_SOLVER:
                job.getEventSender().addListener(new ChallengeSolverJobListener() {
                    @Override
                    public void onSolverTimedOut(ChallengeSolver<?> parameter) {
                    }

                    @Override
                    public void onSolverStarts(ChallengeSolver<?> parameter) {
                    }

                    @Override
                    public void onSolverJobReceivedNewResponse(AbstractResponse<?> response) {
                    }

                    @Override
                    public void onSolverDone(ChallengeSolver<?> solver) {
                        if (job.isDone()) {
                            if (!job.isSolved()) {
                                ChallengeResponseController.getInstance().setSkipRequest(SkipRequest.SINGLE, AbstractBrowserSolver.this, job.getChallenge());
                            }
                            job.getEventSender().removeListener(this);
                        }
                    }
                });
                return;
            case SKIP_LINK:
                throw new SkipException(job.getChallenge(), SkipRequest.SINGLE);
            }
        }
        checkInterruption();
    }

    @Override
    public void solve(final SolverJob<String> job) throws InterruptedException, SkipException {
        synchronized (DialogBasicCaptchaSolver.getInstance()) {
            if (job.isDone()) {
                return;
            }
            if (job.getChallenge() instanceof AbstractBrowserChallenge) {
                ChallengeSolverJobListener jacListener = null;
                checkSilentMode(job);
                AbstractBrowserChallenge captchaChallenge = (AbstractBrowserChallenge) job.getChallenge();
                // we do not need another queue
                handler = new BrowserDialogHandler(captchaChallenge);
                job.getEventSender().addListener(jacListener = new ChallengeSolverJobListener() {
                    @Override
                    public void onSolverTimedOut(ChallengeSolver<?> parameter) {
                    }

                    @Override
                    public void onSolverStarts(ChallengeSolver<?> parameter) {
                    }

                    @Override
                    public void onSolverJobReceivedNewResponse(AbstractResponse<?> response) {
                        final ResponseList<String> resp = job.getResponse();
                        final BrowserDialogHandler hndlr = handler;
                        if (hndlr != null && resp != null) {
                            hndlr.setSuggest(resp.getValue());
                            job.getLogger().info("Received Suggestion: " + resp);
                        }
                    }

                    @Override
                    public void onSolverDone(ChallengeSolver<?> solver) {
                    }
                });
                try {
                    final ResponseList<String> resp = job.getResponse();
                    if (resp != null) {
                        handler.setSuggest(resp.getValue());
                    }
                    checkInterruption();
                    handler.run();
                    String response = handler.getResponseString();
                    if (response != null) {
                        job.addAnswer(new BrowserResponse(captchaChallenge, this, response, 100));
                    }
                } finally {
                    job.getLogger().info("Dialog closed. Response far: " + job.getResponse());
                    if (jacListener != null) {
                        job.getEventSender().removeListener(jacListener);
                    }
                    handler = null;
                }
            }
        }
    }

    public void requestFocus(Challenge<?> challenge) {
        final BrowserDialogHandler hndlr = handler;
        if (hndlr != null) {
            hndlr.requestFocus();
        }
    }
}
