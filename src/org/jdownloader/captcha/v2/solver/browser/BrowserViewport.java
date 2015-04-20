package org.jdownloader.captcha.v2.solver.browser;

import java.awt.Robot;

public class BrowserViewport extends ScreenResource {

    private BrowserWindow browserWindow;

    public BrowserViewport(BrowserWindow screenResource) {
        super();

        this.browserWindow = screenResource;
    }

    @Override
    protected Robot getRobot() {
        return browserWindow.getRobot();
    }

    public void onLoaded() {
    }

}
