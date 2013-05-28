package org.jdownloader.api.downloads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadControllerEvent;
import jd.controlling.downloadcontroller.DownloadControllerListener;

import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.EventsSender;
import org.appwork.remoteapi.events.SimpleEventObject;

public class DownloadControllerEventPublisher implements EventPublisher, DownloadControllerListener {

    private CopyOnWriteArraySet<EventsSender> eventSenders = new CopyOnWriteArraySet<EventsSender>();
    private final List<String>                eventIDs;

    public DownloadControllerEventPublisher() {
        eventIDs = new ArrayList<String>();
        for (DownloadControllerEvent.TYPE t : DownloadControllerEvent.TYPE.values()) {
            eventIDs.add(t.name());
        }
    }

    @Override
    public void onDownloadControllerEvent(DownloadControllerEvent event) {
        SimpleEventObject eventObject = new SimpleEventObject(this, event.getType().name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public String[] getPublisherEventIDs() {
        return (String[]) eventIDs.toArray();
    }

    @Override
    public String getPublisherName() {
        return "downloads";
    }

    @Override
    public synchronized void register(EventsSender eventsAPI) {
        boolean wasEmpty = eventSenders.isEmpty();
        eventSenders.add(eventsAPI);
        if (wasEmpty && eventSenders.isEmpty() == false) {
            DownloadController.getInstance().addListener(this, true);
        }
    }

    @Override
    public synchronized void unregister(EventsSender eventsAPI) {
        eventSenders.remove(eventsAPI);
        if (eventSenders.isEmpty()) {
            DownloadController.getInstance().removeListener(this);
        }
    }

}
