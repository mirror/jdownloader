package org.jdownloader.captcha.v2.challenge.oauth;

import jd.controlling.captcha.SkipException;
import jd.controlling.captcha.SkipRequest;
import jd.gui.swing.jdgui.JDGui;

import org.appwork.exceptions.WTFException;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solver.gui.DialogBasicCaptchaSolver;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solver.service.BrowserSolverService;
import org.jdownloader.captcha.v2.solverjob.ChallengeSolverJobListener;
import org.jdownloader.captcha.v2.solverjob.ResponseList;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.settings.staticreferences.CFG_SILENTMODE;

public class OAuthDialogSolver extends ChallengeSolver<Boolean> {
    private static final OAuthDialogSolver INSTANCE = new OAuthDialogSolver();

    public static OAuthDialogSolver getInstance() {
        return INSTANCE;
    }

    @Override
    protected boolean isChallengeSupported(Challenge<?> c) {
        return c instanceof OAuthChallenge;
    }

    private OAuthDialogSolver() {
        super(BrowserSolverService.getInstance(), 1);
    }

    // protected BrowserCaptchaSolverConfig config;
    private OAuthDialogHandler handler;

    @Override
    public boolean isEnabled() {
        // always enabled. else people cannot login
        return true;
    }

    @Override
    public Class<Boolean> getResultType() {
        return Boolean.class;
    }

    public void checkSilentMode(final SolverJob<Boolean> job) throws SkipException, InterruptedException {
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
                                ChallengeResponseController.getInstance().setSkipRequest(SkipRequest.SINGLE, OAuthDialogSolver.this, job.getChallenge());
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

    public void requestFocus(Challenge<?> challenge) {
        OAuthDialogHandler hndlr = handler;
        if (hndlr != null) {
            hndlr.requestFocus();
        }
    }

    @Override
    public void solve(final SolverJob<Boolean> job) throws InterruptedException, SolverException, SkipException {
        synchronized (DialogBasicCaptchaSolver.getInstance()) {
            ChallengeSolverJobListener jacListener = null;
            checkSilentMode(job);
            handler = new OAuthDialogHandler((OAuthChallenge) job.getChallenge());
            try {
                ResponseList<Boolean> resp = job.getResponse();
                checkInterruption();
                job.getChallenge().sendStatsSolving(this);
                handler.run();
            } catch (SkipException e) {
                throw e;
            } catch (Exception e) {
                job.getChallenge().sendStatsError(this, e);
                throw new WTFException(e);
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
