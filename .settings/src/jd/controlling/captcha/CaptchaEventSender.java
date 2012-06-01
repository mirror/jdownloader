package jd.controlling.captcha;

import org.appwork.utils.event.Eventsender;

public class CaptchaEventSender extends Eventsender<CaptchaEventListener, CaptchaEvent> {

    private static final CaptchaEventSender INSTANCE = new CaptchaEventSender();

    public static final CaptchaEventSender getInstance() {
        return INSTANCE;
    }

    private CaptchaEventSender() {
    }

    @Override
    protected void fireEvent(CaptchaEventListener listener, CaptchaEvent event) {
        if (event instanceof CaptchaTodoEvent) {
            listener.captchaTodo(event.getCaptchaController());
        } else if (event instanceof CaptchaFinishEvent) {
            listener.captchaFinish(event.getCaptchaController());
        }
    }

}
