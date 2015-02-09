package org.jdownloader.api.downloads;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogProperty;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.RemoteAPIEventsSender;
import org.appwork.remoteapi.events.SimpleEventObject;

public class DownloadWatchDogEventPublisher implements EventPublisher, DownloadWatchdogListener {

    private enum EVENTID {
        UPDATE,
        RUNNING,
        PAUSED,
        STOPSIGN,
        STOPPED
    }

    @Override
    public void onDownloadWatchDogPropertyChange(DownloadWatchDogProperty propertyChange) {
        switch (propertyChange.getProperty()) {
        case STOPSIGN:
            HashMap<String, Object> dls = new HashMap<String, Object>();
            Object data = propertyChange.getValue();
            if (data instanceof FilePackage) {
                data = ((FilePackage) propertyChange.getValue()).getUniqueID().getID();
            } else if (data instanceof DownloadLink) {
                data = ((DownloadLink) propertyChange.getValue()).getUniqueID().getID();
            } else {
                data = String.valueOf(propertyChange.getValue());
            }
            dls.put("data", data);
            SimpleEventObject eventObject = new SimpleEventObject(this, EVENTID.STOPSIGN.name(), dls);
            for (RemoteAPIEventsSender eventSender : eventSenders) {
                eventSender.publishEvent(eventObject, null);
            }
            break;
        }
    }

    private CopyOnWriteArraySet<RemoteAPIEventsSender> eventSenders = new CopyOnWriteArraySet<RemoteAPIEventsSender>();
    private final String[]                             eventIDs;

    public DownloadWatchDogEventPublisher() {
        eventIDs = new String[] { EVENTID.PAUSED.name(), EVENTID.RUNNING.name(), EVENTID.STOPPED.name(), EVENTID.UPDATE.name() };

    }

    @Override
    public String[] getPublisherEventIDs() {
        return eventIDs;
    }

    @Override
    public String getPublisherName() {
        return "downloadwatchdog";
    }

    @Override
    public void onDownloadWatchdogDataUpdate() {
    }

    @Override
    public void onDownloadWatchdogStateIsIdle() {
    }

    @Override
    public void onDownloadWatchdogStateIsPause() {
        SimpleEventObject eventObject = new SimpleEventObject(this, EVENTID.PAUSED.name(), null);
        for (RemoteAPIEventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onDownloadWatchdogStateIsRunning() {
        SimpleEventObject eventObject = new SimpleEventObject(this, EVENTID.RUNNING.name(), null);
        for (RemoteAPIEventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onDownloadWatchdogStateIsStopped() {
        SimpleEventObject eventObject = new SimpleEventObject(this, EVENTID.STOPPED.name(), null);
        for (RemoteAPIEventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onDownloadWatchdogStateIsStopping() {
    }

    @Override
    public synchronized void register(RemoteAPIEventsSender eventsAPI) {
        boolean wasEmpty = eventSenders.isEmpty();
        eventSenders.add(eventsAPI);
        if (wasEmpty && eventSenders.isEmpty() == false) {
            DownloadWatchDog.getInstance().getEventSender().addListener(this, true);
        }
    }

    @Override
    public synchronized void unregister(RemoteAPIEventsSender eventsAPI) {
        eventSenders.remove(eventsAPI);
        if (eventSenders.isEmpty()) {
            DownloadWatchDog.getInstance().getEventSender().removeListener(this);
        }
    }

    @Override
    public void onDownloadControllerStart(SingleDownloadController downloadController, DownloadLinkCandidate candidate) {
        // not implemented yet
    }

    @Override
    public void onDownloadControllerStopped(SingleDownloadController downloadController, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
        // not implemented yet
    }

}
