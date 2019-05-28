package org.jdownloader.captcha.v2.challenge.keycaptcha.jac;

import java.util.ArrayList;

import jd.controlling.captcha.SkipException;
import jd.controlling.captcha.SkipRequest;

import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaPuzzleChallenge;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaResponse;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class KeyCaptchaJACSolver extends ChallengeSolver<String> {
    private static final KeyCaptchaJACSolver INSTANCE = new KeyCaptchaJACSolver();

    public static KeyCaptchaJACSolver getInstance() {
        return INSTANCE;
    }

    @Override
    protected boolean isChallengeSupported(Challenge<?> c) {
        return c instanceof KeyCaptchaPuzzleChallenge && !((KeyCaptchaPuzzleChallenge) c).isNoAutoSolver();
    }

    private KeyCaptchaJACSolver() {
        super(JACSolver.getInstance().getService(), 1);
    }

    @Override
    public void solve(SolverJob<String> solverJob) throws InterruptedException, SolverException, SkipException {
        final KeyCaptchaPuzzleChallenge challenge = ((KeyCaptchaPuzzleChallenge) solverJob.getChallenge());
        try {
            final KeyCaptchaAutoSolver kcSolver = new KeyCaptchaAutoSolver();
            final String out = kcSolver.solve(challenge.getHelper().getPuzzleData().getImages());
            final ArrayList<Integer> marray = new ArrayList<Integer>();
            marray.addAll(kcSolver.getMouseArray());
            if (out == null) {
                if (challenge.getRound() > 3) {
                    // show dialog if autosolver did not succeed within 3 rounds
                    return;
                }
                throw new SkipException(solverJob.getChallenge(), SkipRequest.REFRESH);
            }
            challenge.sendStatsSolving(this);
            final String token = challenge.getHelper().sendPuzzleResult(marray, out);
            if (token != null) {
                solverJob.addAnswer(new KeyCaptchaResponse(challenge, this, token, 100));
            } else {
                if (challenge.getRound() > 3) {
                    // show dialog if autosolver did not succeed within 3 rounds
                    return;
                }
                throw new SkipException(solverJob.getChallenge(), SkipRequest.REFRESH);
            }
        } catch (Throwable e) {
            solverJob.getLogger().log(e);
            challenge.sendStatsError(this, e);
        }
    }
}
