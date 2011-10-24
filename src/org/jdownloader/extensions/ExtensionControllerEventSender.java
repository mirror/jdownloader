package org.jdownloader.extensions;

import org.appwork.utils.event.Eventsender;

public class ExtensionControllerEventSender extends Eventsender<ExtensionControllerListener, ExtensionControllerEvent> {

    @Override
    protected void fireEvent(ExtensionControllerListener listener, ExtensionControllerEvent event) {
        switch (event.getType()) {
        case UPDATED:
            listener.onUpdated();
            break;
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }
}