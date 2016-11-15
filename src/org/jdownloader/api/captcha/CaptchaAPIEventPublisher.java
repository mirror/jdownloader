package org.jdownloader.api.captcha;

import java.util.concurrent.CopyOnWriteArraySet;

import org.appwork.remoteapi.events.EventObject;
import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.RemoteAPIEventsSender;
import org.appwork.remoteapi.events.SimpleEventObject;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class CaptchaAPIEventPublisher implements EventPublisher {
    private enum EVENTID {
        NEW,
        DONE;
        // NEW_ANSWER,
        // SOLVER_START,
        // SOLVER_END
    }

    private final CopyOnWriteArraySet<RemoteAPIEventsSender> eventSenders = new CopyOnWriteArraySet<RemoteAPIEventsSender>();
    private final String[]                                   eventIDs;

    /**
     * access this publisher view CaptchaAPISolver.getInstance().getEventPublisher
     */
    public CaptchaAPIEventPublisher() {
        eventIDs = new String[] { EVENTID.NEW.name(), EVENTID.DONE.name() };
    }

    @Override
    public String[] getPublisherEventIDs() {
        return eventIDs;
    }

    @Override
    public String getPublisherName() {
        return "captchas";
    }

    private final boolean hasSubscriptionFor(final String eventID) {
        if (eventSenders.size() > 0) {
            for (final RemoteAPIEventsSender eventSender : eventSenders) {
                if (eventSender.hasSubscriptionFor(this, eventID)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void fireJobDoneEvent(SolverJob<?> job) {
        if (hasSubscriptionFor(EVENTID.DONE.name())) {
            final EventObject eventObject = new SimpleEventObject(this, EVENTID.DONE.name(), job.getChallenge().getId().getID());
            for (final RemoteAPIEventsSender eventSender : eventSenders) {
                eventSender.publishEvent(eventObject, null);
            }
        }
    }

    public void fireNewJobEvent(SolverJob<?> job, Challenge<?> challenge) {
        if (hasSubscriptionFor(EVENTID.NEW.name())) {
            final EventObject eventObject = new SimpleEventObject(this, EVENTID.NEW.name(), challenge.getId().getID());
            for (final RemoteAPIEventsSender eventSender : eventSenders) {
                eventSender.publishEvent(eventObject, null);
            }
        }
    }

    @Override
    public synchronized void register(RemoteAPIEventsSender eventsAPI) {
        eventSenders.add(eventsAPI);
    }

    @Override
    public synchronized void unregister(RemoteAPIEventsSender eventsAPI) {
        eventSenders.remove(eventsAPI);
    }
}
