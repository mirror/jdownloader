package org.jdownloader.api.captcha;

import org.appwork.utils.event.DefaultEvent;

public abstract class CaptchaAPISolverEvent extends DefaultEvent {

    public CaptchaAPISolverEvent(Object caller) {
        super(caller);
    }

    abstract public void sendTo(CaptchaAPISolverListener listener);
}