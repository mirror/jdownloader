package org.jdownloader.captcha.v2.challenge.oauth;

import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

import jd.controlling.captcha.SkipException;

public class AccountOAuthSolver extends ChallengeSolver<Boolean> {
    private static final AccountOAuthSolver INSTANCE = new AccountOAuthSolver();

    public static AccountOAuthSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean canHandle(Challenge<?> c) {

        return c != null && (c instanceof OAuthChallenge);
    }

    @Override
    public boolean isEnabled() {
        // I see no reason why we should disable it.
        return true;
    }

    private AccountOAuthSolver() {
        super(JACSolver.getInstance().getService(), 1);
    }

    @Override
    public void solve(SolverJob<Boolean> solverJob) throws InterruptedException, SolverException, SkipException {
        final OAuthChallenge challenge = ((OAuthChallenge) solverJob.getChallenge());

        try {
            if (challenge instanceof AccountLoginOAuthChallenge) {
                if (((AccountLoginOAuthChallenge) challenge).autoSolveChallenge()) {
                    solverJob.addAnswer(new AbstractResponse<Boolean>(challenge, this, 100, true));
                    return;
                }
            }

        } catch (Throwable e) {
            solverJob.getLogger().log(e);
            challenge.sendStatsError(this, e);
        }
    }
}
