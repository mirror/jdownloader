package jd.controlling.captcha;

import java.io.File;

public interface CaptchaSolver {

    public String solveCaptcha(final String host, final File captchaFile, final String suggestion, final String explain);

}
