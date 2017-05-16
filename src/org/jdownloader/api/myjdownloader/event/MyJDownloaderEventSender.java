package org.jdownloader.api.myjdownloader.event;

import org.appwork.utils.event.Eventsender;
import org.jdownloader.api.myjdownloader.MyJDownloaderConnectionStatus;

public class MyJDownloaderEventSender extends Eventsender<MyJDownloaderListener, MyJDownloaderEvent> {
    @Override
    protected void fireEvent(MyJDownloaderListener listener, MyJDownloaderEvent event) {
        switch (event.getType()) {
        case CONNECTION_STATUS_UPDATE:
            listener.onMyJDownloaderConnectionStatusChanged((MyJDownloaderConnectionStatus) event.getParameter(), (Integer) event.getParameter(1));
            break;
        // fill
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }
}