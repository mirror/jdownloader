package jd;

import java.awt.Image;

public class SplashProgressImage {

    private Image image;

    private long startTime = 0;
    private final int dur = 500;

    public SplashProgressImage(Image i) {
        image = i;
    }

    public Image getImage() {
        return image;
    }

    public float getAlpha() {
        if (this.startTime == 0) {
            this.startTime = System.currentTimeMillis();
        }
        return Math.min((System.currentTimeMillis() - startTime) / (float) dur, 1.0f);
    }

}
