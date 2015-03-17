package org.jdownloader.api.captcha;

import org.appwork.utils.event.Eventsender;

public class CaptchaAPISolverEventSender extends Eventsender<CaptchaAPISolverListener, CaptchaAPISolverEvent> {

    @Override
    protected void fireEvent(CaptchaAPISolverListener listener, CaptchaAPISolverEvent event) {
        event.sendTo(listener);
    }
}