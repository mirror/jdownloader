package org.jdownloader.captcha.v2.challenge.clickcaptcha;

import org.appwork.storage.Storable;

public class ClickedPoint implements Storable {
    public ClickedPoint(/* storable */) {

    }

    public ClickedPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    private int x;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    private int y;
}
