package org.jdownloader.captcha.event;

import org.appwork.utils.event.Eventsender;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class ChallengeResponseEventSender extends Eventsender<ChallengeResponseListener, ChallengeResponseEvent> {

    @Override
    protected void fireEvent(ChallengeResponseListener listener, ChallengeResponseEvent event) {
        switch (event.getType()) {
        case JOB_ANSWER:
            listener.onNewJobAnswer((SolverJob<?>) event.getParameter(0), (AbstractResponse<?>) event.getParameter(1));
            return;
        case JOB_DONE:
            listener.onJobDone((SolverJob<?>) event.getParameter(0));
            return;
        case NEW_JOB:
            listener.onNewJob((SolverJob<?>) event.getParameter(0));
            return;
        case SOLVER_END:
            listener.onJobSolverEnd((ChallengeSolver<?>) event.getParameter(0), (SolverJob<?>) event.getParameter(1));
            return;
        case SOLVER_START:
            listener.onJobSolverStart((ChallengeSolver<?>) event.getParameter(0), (SolverJob<?>) event.getParameter(1));
            return;
            // fill
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }
}