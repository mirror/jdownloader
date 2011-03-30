package jd.controlling.captcha;

import org.appwork.utils.event.DefaultEvent;

public abstract class CaptchaEvent extends DefaultEvent {

    public CaptchaEvent(CaptchaController caller) {
        super(caller);
    }

    public CaptchaController getCaptchaController() {
        return (CaptchaController) super.getCaller();
    }
}
