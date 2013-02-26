package org.jdownloader.captcha.event;

import java.util.EventListener;

import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public interface ChallengeResponseListener extends EventListener {

    void onNewJobAnswer(SolverJob<?> job, AbstractResponse<?> response);

    void onJobDone(SolverJob<?> job);

    void onNewJob(SolverJob<?> job);

    void onJobSolverEnd(ChallengeSolver<?> solver, SolverJob<?> job);

    void onJobSolverStart(ChallengeSolver<?> solver, SolverJob<?> job);

}