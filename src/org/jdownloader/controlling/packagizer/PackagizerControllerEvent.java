package org.jdownloader.controlling.packagizer;

import org.appwork.utils.event.DefaultEvent;

public abstract class PackagizerControllerEvent extends DefaultEvent {

    public PackagizerControllerEvent() {
        super(PackagizerController.getInstance());
    }

    public abstract void sendTo(PackagizerControllerListener listener);
}