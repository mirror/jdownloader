package org.jdownloader.api.useragent;

import org.appwork.utils.event.Eventsender;

public class UserAgentEventSender extends Eventsender<UserAgentListener, UserAgentEvent> {

    @Override
    protected void fireEvent(UserAgentListener listener, UserAgentEvent event) {
        event.fireTo(listener);
    }
}