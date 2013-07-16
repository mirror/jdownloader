package org.jdownloader.api.captcha;

import java.util.concurrent.CopyOnWriteArraySet;

import org.appwork.remoteapi.events.EventObject;
import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.EventsSender;
import org.appwork.remoteapi.events.SimpleEventObject;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class CaptchaAPIEventPublisher implements EventPublisher {

    private enum EVENTID {
        NEW,
        DONE;
        // NEW_ANSWER,
        // SOLVER_START,
        // SOLVER_END
    }

    private CopyOnWriteArraySet<EventsSender> eventSenders = new CopyOnWriteArraySet<EventsSender>();
    private final String[]                    eventIDs;

    /**
     * access this publisher view CaptchaAPISolver.getInstance().getEventPublisher
     */
    public CaptchaAPIEventPublisher() {
        eventIDs = new String[] { EVENTID.NEW.name() };
    }

    @Override
    public String[] getPublisherEventIDs() {
        return eventIDs;
    }

    @Override
    public String getPublisherName() {
        return "captchas";
    }

    // public void onNewJobAnswer(SolverJob<?> job) {
    // EventObject eventObject = new SimpleEventObject(this, EVENTID.NEW_ANSWER.name(), job.getChallenge().getId().getID());
    // for (EventsSender eventSender : eventSenders) {
    // eventSender.publishEvent(eventObject, null);
    // }
    // }

    public void fireJobDoneEvent(SolverJob<?> job) {
        EventObject eventObject = new SimpleEventObject(this, EVENTID.DONE.name(), job.getChallenge().getId().getID());
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    public void fireNewJobEvent(SolverJob<?> job) {

        EventObject eventObject = new SimpleEventObject(this, EVENTID.NEW.name(), job.getChallenge().getId().getID());
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    // @Override
    // public void onJobSolverEnd(ChallengeSolver<?> solver, SolverJob<?> job) {
    // EventObject eventObject = new SimpleEventObject(this, EVENTID.SOLVER_START.name(), job.getChallenge().getId().getID());
    // for (EventsSender eventSender : eventSenders) {
    // eventSender.publishEvent(eventObject, null);
    // }
    // }
    //
    // @Override
    // public void onJobSolverStart(ChallengeSolver<?> solver, SolverJob<?> job) {
    // EventObject eventObject = new SimpleEventObject(this, EVENTID.SOLVER_END.name(), job.getChallenge().getId().getID());
    // for (EventsSender eventSender : eventSenders) {
    // eventSender.publishEvent(eventObject, null);
    // }
    // }

    @Override
    public synchronized void register(EventsSender eventsAPI) {
        boolean wasEmpty = eventSenders.isEmpty();
        eventSenders.add(eventsAPI);

    }

    @Override
    public synchronized void unregister(EventsSender eventsAPI) {
        eventSenders.remove(eventsAPI);

    }
}
