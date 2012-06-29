package jd.controlling.captcha;

import org.appwork.storage.Storable;

public class CaptchaResult implements Storable {

    public CaptchaResult(/* Storable */) {
    }

    public CaptchaResult(String text) {
        this.captchaText = text;
    }

    public CaptchaResult(int x, int y) {
        captchaClick = new int[] { x, y };
    }

    private String captchaText    = null;
    private int    captchaClick[] = { 0, 0 };

    public String getCaptchaText() {
        return captchaText;
    }

    public void setCaptchaText(String captchaText) {
        this.captchaText = captchaText;
    }

    public int[] getCaptchaClick() {
        return captchaClick;
    }

    public void setCaptchaClick(int[] captchaClick) {
        this.captchaClick = captchaClick;
    }
}
