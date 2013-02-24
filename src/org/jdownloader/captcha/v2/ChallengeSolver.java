package org.jdownloader.captcha.v2;

public interface ChallengeSolver<T> {

    void solve(SolverJob<T> solverJob);

    Class<T> getResultType();

}
