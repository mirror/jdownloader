package org.jdownloader.captcha.v2.solverjob;

import java.util.EventListener;

import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeSolver;

public interface ChallengeSolverJobListener extends EventListener {

    void onSolverJobReceivedNewResponse(AbstractResponse<?> response);

    void onSolverDone(ChallengeSolver<?> solver);

    void onSolverStarts(ChallengeSolver<?> parameter);

    void onSolverTimedOut(ChallengeSolver<?> parameter);

}