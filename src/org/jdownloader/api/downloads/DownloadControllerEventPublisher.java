package org.jdownloader.api.downloads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageProperty;
import jd.plugins.LinkStatusProperty;

import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.EventsSender;
import org.appwork.remoteapi.events.SimpleEventObject;
import org.jdownloader.controlling.download.DownloadControllerEvent;
import org.jdownloader.controlling.download.DownloadControllerListener;

public class DownloadControllerEventPublisher implements EventPublisher, DownloadControllerListener {

    private enum EVENTID {
        REFRESH_STRUCTURE,
        REMOVE_CONTENT,
        ADD_CONTENT,
        REFRESH_CONTENT
    }

    private CopyOnWriteArraySet<EventsSender> eventSenders = new CopyOnWriteArraySet<EventsSender>();
    private final List<String>                eventIDs;

    public DownloadControllerEventPublisher() {
        eventIDs = new ArrayList<String>();
        for (EVENTID t : EVENTID.values()) {
            eventIDs.add(t.name());
        }
    }

    @Override
    public String[] getPublisherEventIDs() {
        return (String[]) eventIDs.toArray(new String[] {});
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

    @Override
    public void onDownloadControllerAddedPackage(FilePackage pkg) {
        SimpleEventObject eventObject = new SimpleEventObject(this, DownloadControllerEvent.TYPE.ADD_CONTENT.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onDownloadControllerStructureRefresh(FilePackage pkg) {
        SimpleEventObject eventObject = new SimpleEventObject(this, DownloadControllerEvent.TYPE.REFRESH_STRUCTURE.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onDownloadControllerStructureRefresh() {
        SimpleEventObject eventObject = new SimpleEventObject(this, DownloadControllerEvent.TYPE.REFRESH_STRUCTURE.name(), DownloadControllerEvent.TYPE.REFRESH_STRUCTURE.name());
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onDownloadControllerStructureRefresh(AbstractNode node, Object param) {
        SimpleEventObject eventObject = new SimpleEventObject(this, DownloadControllerEvent.TYPE.REFRESH_STRUCTURE.name(), DownloadControllerEvent.TYPE.REFRESH_STRUCTURE.name());
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onDownloadControllerRemovedPackage(FilePackage pkg) {
        SimpleEventObject eventObject = new SimpleEventObject(this, DownloadControllerEvent.TYPE.REMOVE_CONTENT.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onDownloadControllerRemovedLinklist(List<DownloadLink> list) {
        SimpleEventObject eventObject = new SimpleEventObject(this, DownloadControllerEvent.TYPE.REMOVE_CONTENT.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink, DownloadLinkProperty property) {
        SimpleEventObject eventObject = new SimpleEventObject(this, DownloadControllerEvent.TYPE.REFRESH_CONTENT.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg, FilePackageProperty property) {
        SimpleEventObject eventObject = new SimpleEventObject(this, DownloadControllerEvent.TYPE.REFRESH_CONTENT.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink, LinkStatusProperty property) {
        SimpleEventObject eventObject = new SimpleEventObject(this, DownloadControllerEvent.TYPE.REFRESH_CONTENT.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink) {
        SimpleEventObject eventObject = new SimpleEventObject(this, DownloadControllerEvent.TYPE.REFRESH_CONTENT.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg) {
        SimpleEventObject eventObject = new SimpleEventObject(this, DownloadControllerEvent.TYPE.REFRESH_CONTENT.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void terminatedSubscription(EventsSender eventsSender, long subscriptionid) {
    }

}
