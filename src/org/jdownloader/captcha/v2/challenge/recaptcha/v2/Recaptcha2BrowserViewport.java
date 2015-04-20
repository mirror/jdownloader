package org.jdownloader.captcha.v2.challenge.recaptcha.v2;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;

import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;

public class Recaptcha2BrowserViewport extends BrowserViewport {

    private Rectangle     recaptchaIframe;
    private double        scale;
    private BrowserWindow browser;

    public double getScale() {
        return scale;
    }

    @Override
    public void onLoaded() {
        super.onLoaded();

        Point oldloc = MouseInfo.getPointerInfo().getLocation();
        int clickX = recaptchaIframe.x + scale(22) + scale(Math.random() * 20);
        int clickY = recaptchaIframe.y + scale(32) + scale(Math.random() * 20);

        getRobot().mouseMove(clickX, clickY);

        getRobot().mousePress(InputEvent.BUTTON1_MASK);
        getRobot().mouseRelease(InputEvent.BUTTON1_MASK);

        getRobot().mouseMove(oldloc.x, oldloc.y);
    }

    public Recaptcha2BrowserViewport(BrowserWindow screenResource, Rectangle rect) {
        super(screenResource);

        recaptchaIframe = rect;
        scale = recaptchaIframe.width / 362d;

        this.width = screenResource.getViewportWidth();
        this.height = screenResource.getViewportHeight();

        this.x = Math.max(screenResource.getX(), rect.x - scale(10));
        this.y = Math.max(screenResource.getY(), rect.y - scale(10));

        // showImage(getRobot().createScreenCapture(new Rectangle(x, y, width, height)));
    }

    private int scale(Number i) {
        return (int) (scale * i.doubleValue());
    }
}
