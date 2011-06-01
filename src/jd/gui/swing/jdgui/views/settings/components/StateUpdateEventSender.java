package jd.gui.swing.jdgui.views.settings.components;

import org.appwork.utils.event.Eventsender;

public class StateUpdateEventSender<Caller> extends Eventsender<StateUpdateListener, StateUpdateEvent<Caller>> {

    @Override
    protected void fireEvent(StateUpdateListener listener, StateUpdateEvent<Caller> event) {
        listener.onStateUpdated();
    }

}
