package jd.controlling.captcha;

import java.util.EventListener;

public interface CaptchaEventListener extends EventListener {

    public void captchaTodo(CaptchaController controller);

    public void captchaFinish(CaptchaController controller);

}
