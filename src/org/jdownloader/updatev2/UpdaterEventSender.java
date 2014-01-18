package org.jdownloader.updatev2;

import org.appwork.utils.event.Eventsender;

public class UpdaterEventSender extends Eventsender<UpdaterListener, UpdaterEvent> {

    @Override
    protected void fireEvent(UpdaterListener listener, UpdaterEvent event) {
        try {
            switch (event.getType()) {
            case UPDATES_AVAILABLE:
                listener.onUpdatesAvailable((Boolean) event.getParameter(0), (InstallLog) event.getParameter(1));
                break;
            case UPDATE_STATUS:
                listener.onUpdaterStatusUpdate(((UpdateStatusUpdateEvent) event).getLabel(), ((UpdateStatusUpdateEvent) event).getIcon(), ((UpdateStatusUpdateEvent) event).getProgress());
                break;
            // fill
            default:
                System.out.println("Unhandled Event: " + event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}