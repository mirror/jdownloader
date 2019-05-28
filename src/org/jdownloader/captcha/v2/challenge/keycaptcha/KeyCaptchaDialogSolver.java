package org.jdownloader.captcha.v2.challenge.keycaptcha;

import java.io.IOException;

import jd.controlling.captcha.SkipException;
import jd.controlling.captcha.SkipRequest;
import jd.gui.swing.jdgui.JDGui;

import org.appwork.exceptions.WTFException;
import org.jdownloader.captcha.v2.AbstractDialogHandler;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.keycaptcha.dialog.KeyCaptchaCategoryDialogHandler;
import org.jdownloader.captcha.v2.challenge.keycaptcha.dialog.KeyCaptchaPuzzleDialogHandler;
import org.jdownloader.captcha.v2.solver.gui.DialogBasicCaptchaSolver;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solver.service.DialogSolverService;
import org.jdownloader.captcha.v2.solverjob.ChallengeSolverJobListener;
import org.jdownloader.captcha.v2.solverjob.ResponseList;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.settings.staticreferences.CFG_SILENTMODE;

public class KeyCaptchaDialogSolver extends ChallengeSolver<String> {
    private static final KeyCaptchaDialogSolver INSTANCE = new KeyCaptchaDialogSolver();

    public static KeyCaptchaDialogSolver getInstance() {
        return INSTANCE;
    }

    @Override
    protected boolean isChallengeSupported(Challenge<?> c) {
        return c instanceof KeyCaptchaPuzzleChallenge || c instanceof KeyCaptchaCategoryChallenge;
    }

    private KeyCaptchaDialogSolver() {
        super(DialogSolverService.getInstance(), 1);
    }

    // protected BrowserCaptchaSolverConfig config;
    private AbstractDialogHandler<?, ?, ?> handler;

    @Override
    public Class<String> getResultType() {
        return String.class;
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
                                ChallengeResponseController.getInstance().setSkipRequest(SkipRequest.SINGLE, KeyCaptchaDialogSolver.this, job.getChallenge());
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
        AbstractDialogHandler<?, ?, ?> hndlr = handler;
        if (hndlr != null) {
            hndlr.requestFocus();
        }
    }

    @Override
    public void solve(final SolverJob<String> job) throws InterruptedException, SolverException, SkipException {
        synchronized (DialogBasicCaptchaSolver.getInstance()) {
            ChallengeSolverJobListener jacListener = null;
            checkSilentMode(job);
            KeyCaptcha helper = null;
            if (job.getChallenge() instanceof KeyCaptchaPuzzleChallenge) {
                helper = ((KeyCaptchaPuzzleChallenge) job.getChallenge()).getHelper();
                handler = new KeyCaptchaPuzzleDialogHandler((KeyCaptchaPuzzleChallenge) job.getChallenge());
            } else if (job.getChallenge() instanceof KeyCaptchaCategoryChallenge) {
                helper = ((KeyCaptchaCategoryChallenge) job.getChallenge()).getHelper();
                handler = new KeyCaptchaCategoryDialogHandler((KeyCaptchaCategoryChallenge) job.getChallenge());
            } else {
                return;
            }
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
                job.getChallenge().sendStatsSolving(this);
                handler.run();
                String token = null;
                if (job.getChallenge() instanceof KeyCaptchaPuzzleChallenge) {
                    KeyCaptchaPuzzleResponseData response = ((KeyCaptchaPuzzleDialogHandler) handler).getDialog().getReturnValue();
                    if (response == null) {
                        return;
                    }
                    token = helper.sendPuzzleResult(response.getMouseArray(), response.getOut());
                } else if (job.getChallenge() instanceof KeyCaptchaCategoryChallenge) {
                    String response = ((KeyCaptchaCategoryDialogHandler) handler).getDialog().getReturnValue();
                    if (response == null) {
                        return;
                    }
                    token = helper.sendCategoryResult(response);
                }
                if (token != null) {
                    job.addAnswer(new KeyCaptchaResponse(job.getChallenge(), this, token, 100));
                }
            } catch (IOException e) {
                job.getChallenge().sendStatsError(this, e);
                throw new WTFException(e);
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
