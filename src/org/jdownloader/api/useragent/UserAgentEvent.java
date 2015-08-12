package org.jdownloader.api.useragent;

import org.appwork.utils.event.DefaultEvent;

public abstract class UserAgentEvent extends DefaultEvent {

    public UserAgentEvent() {
        super(null);
    }

    public abstract void fireTo(UserAgentListener listener);
}