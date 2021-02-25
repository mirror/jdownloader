package org.jdownloader.captcha.v2.solver.browser;

import java.awt.Rectangle;

public abstract class BrowserWindow extends ScreenResource {
    private final double viewportWidth;

    public double getViewportWidth() {
        return viewportWidth;
    }

    public double getViewportHeight() {
        return viewportHeight;
    }

    private final double viewportHeight;
    private final String userAgent;
    private final Double dpi;

    public String getUserAgent() {
        return userAgent;
    }

    public BrowserWindow(String userAgent, int x, int y, int width, int height, double viewportWidth, double viewportHeight, Double dpi) {
        super(x, y, width, height);
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.userAgent = userAgent;
        this.dpi = dpi;
    }

    public double getDPI() {
        if (dpi != null) {
            return dpi.doubleValue();
        } else {
            return 1.0d;
        }
    }

    public void screenShot() {
        showImage(getRobot().createScreenCapture(new Rectangle(x, y, width, height)), null);
    }
}
