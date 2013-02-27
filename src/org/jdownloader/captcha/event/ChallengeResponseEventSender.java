package org.jdownloader.captcha.event;

import org.appwork.utils.event.Eventsender;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class ChallengeResponseEventSender extends Eventsender<ChallengeResponseListener, ChallengeResponseEvent> {

    private LogSource logger;

    public ChallengeResponseEventSender(LogSource logger) {
        this.logger = logger;
    }

    @Override
    protected void fireEvent(ChallengeResponseListener listener, ChallengeResponseEvent event) {
        try {
            switch (event.getType()) {
            case JOB_ANSWER:
                listener.onNewJobAnswer((SolverJob<?>) event.getParameter(1), (AbstractResponse<?>) event.getParameter(0));
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
        } catch (Throwable e) {
            logger.log(e);
        }
    }
}