package jd.controlling.captcha;

import java.io.File;

import javax.swing.ImageIcon;

public interface CaptchaSolver {

    public String solveCaptcha(final String host, final ImageIcon icon, final File captchaFile, final String suggestion, final String explain);

}
