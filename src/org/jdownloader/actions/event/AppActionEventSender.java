package org.jdownloader.actions.event;

import java.beans.PropertyChangeEvent;

import org.appwork.utils.event.Eventsender;

public class AppActionEventSender extends Eventsender<AppActionListener, AppActionEvent> {

    @Override
    protected void fireEvent(AppActionListener listener, AppActionEvent event) {
        switch (event.getType()) {
        case PROPERTY_CHANGE:
            listener.onActionPropertyChanged((PropertyChangeEvent) event.getParameter());
            break;
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }
}