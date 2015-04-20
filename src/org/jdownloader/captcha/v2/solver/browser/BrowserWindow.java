package org.jdownloader.captcha.v2.solver.browser;

public class BrowserWindow extends ScreenResource {

    private int viewportWidth;

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    private int viewportHeight;

    public BrowserWindow(int x, int y, int width, int height, int viewportWidth, int viewportHeight) {
        super(x, y, width, height);
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;

    }

}
