package org.jdownloader.api.captcha;

import java.util.concurrent.CopyOnWriteArraySet;

import org.appwork.remoteapi.events.EventObject;
import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.EventsSender;
import org.appwork.remoteapi.events.SimpleEventObject;
import org.jdownloader.captcha.event.ChallengeResponseListener;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class CaptchaAPIEventPublisher implements EventPublisher, ChallengeResponseListener {

    private enum EVENTID {
        NEW,
        DONE,
        NEW_ANSWER,
        SOLVER_START,
        SOLVER_END
    }

    private CopyOnWriteArraySet<EventsSender> eventSenders = new CopyOnWriteArraySet<EventsSender>();
    private final String[]                    eventIDs;

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

    @Override
    public void onNewJobAnswer(SolverJob<?> job, AbstractResponse<?> response) {
        EventObject eventObject = new SimpleEventObject(this, EVENTID.NEW_ANSWER.name(), job.getChallenge().getId().getID());
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onJobDone(SolverJob<?> job) {
        EventObject eventObject = new SimpleEventObject(this, EVENTID.DONE.name(), job.getChallenge().getId().getID());
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onNewJob(SolverJob<?> job) {
        EventObject eventObject = new SimpleEventObject(this, EVENTID.NEW.name(), job.getChallenge().getId().getID());
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onJobSolverEnd(ChallengeSolver<?> solver, SolverJob<?> job) {
        EventObject eventObject = new SimpleEventObject(this, EVENTID.SOLVER_START.name(), job.getChallenge().getId().getID());
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onJobSolverStart(ChallengeSolver<?> solver, SolverJob<?> job) {
        EventObject eventObject = new SimpleEventObject(this, EVENTID.SOLVER_END.name(), job.getChallenge().getId().getID());
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public synchronized void register(EventsSender eventsAPI) {
        boolean wasEmpty = eventSenders.isEmpty();
        eventSenders.add(eventsAPI);
        if (wasEmpty && eventSenders.isEmpty() == false) {
            ChallengeResponseController.getInstance().getEventSender().addListener(this, true);
        }
    }

    @Override
    public synchronized void unregister(EventsSender eventsAPI) {
        eventSenders.remove(eventsAPI);
        if (eventSenders.isEmpty()) {
            ChallengeResponseController.getInstance().getEventSender().removeListener(this);
        }
    }
}
