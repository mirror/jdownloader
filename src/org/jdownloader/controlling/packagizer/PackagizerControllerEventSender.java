package org.jdownloader.controlling.packagizer;

import org.appwork.utils.event.Eventsender;

public class PackagizerControllerEventSender extends Eventsender<PackagizerControllerListener, PackagizerControllerEvent> {

    @Override
    protected void fireEvent(PackagizerControllerListener listener, PackagizerControllerEvent event) {
        event.sendTo(listener);
    }
}