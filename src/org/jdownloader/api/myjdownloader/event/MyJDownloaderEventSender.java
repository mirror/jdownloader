package org.jdownloader.api.myjdownloader.event;

import org.appwork.utils.event.Eventsender;

public class MyJDownloaderEventSender extends Eventsender<MyJDownloaderListener, MyJDownloaderEvent> {

    @Override
    protected void fireEvent(MyJDownloaderListener listener, MyJDownloaderEvent event) {
        switch (event.getType()) {
        case CONNECTION_STATUS_UPDATE:
            listener.onMyJDownloaderConnectionStatusChanged((Boolean) event.getParameter());
            break;
        // fill
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }
}