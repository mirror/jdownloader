package org.jdownloader.captcha.v2.solverjob;

import org.appwork.utils.event.Eventsender;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeSolver;

public class ChallengeSolverJobEventSender extends Eventsender<ChallengeSolverJobListener, ChallengeSolverJobEvent> {

    @Override
    protected void fireEvent(ChallengeSolverJobListener listener, ChallengeSolverJobEvent event) {
        switch (event.getType()) {
        case NEW_ANSWER:
            listener.onSolverJobReceivedNewResponse((AbstractResponse<?>) event.getParameter(0));
            return;
        case SOLVER_DONE:
            listener.onSolverDone((ChallengeSolver<?>) event.getParameter());
            return;
        case SOLVER_START:
            listener.onSolverStarts((ChallengeSolver<?>) event.getParameter());
            return;
        case SOLVER_TIMEOUT:
            listener.onSolverTimedOut((ChallengeSolver<?>) event.getParameter());
            return;
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }
}