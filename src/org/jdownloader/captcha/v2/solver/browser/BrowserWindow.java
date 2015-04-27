package org.jdownloader.captcha.v2.solver.browser;

import java.awt.Rectangle;

public class BrowserWindow extends ScreenResource {

    private int viewportWidth;

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    private int    viewportHeight;
    private String userAgent;

    public String getUserAgent() {
        return userAgent;
    }

    public void setViewportWidth(int viewportWidth) {
        this.viewportWidth = viewportWidth;
    }

    public void setViewportHeight(int viewportHeight) {
        this.viewportHeight = viewportHeight;
    }

    public BrowserWindow(String userAgent, int x, int y, int width, int height, int viewportWidth, int viewportHeight) {
        super(x, y, width, height);
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.userAgent = userAgent;

    }

    public void screenShot() {
        showImage(getRobot().createScreenCapture(new Rectangle(x, y, width, height)), null);

    }
}
