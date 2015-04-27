package org.jdownloader.captcha.v2.challenge.recaptcha.v1;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;

public class Recaptcha1BrowserViewport extends BrowserViewport {

    private Rectangle     recaptchaIframe;

    private BrowserWindow browser;
    protected Rectangle   captchaPopupRectangle;
    private BufferedImage image;

    public double getScale() {
        return scale;
    }

    @Override
    public void onLoaded() {
        super.onLoaded();

    }

    public Recaptcha1BrowserViewport(BrowserWindow screenResource, Rectangle rect) {
        super(screenResource);

        recaptchaIframe = rect;
        scale = recaptchaIframe.width / 306d;

        this.width = (int) (screenResource.getViewportWidth() * scale);
        this.height = (int) (screenResource.getViewportHeight() * scale);

        this.x = Math.max(screenResource.getX(), rect.x - scale(10));
        this.y = Math.max(screenResource.getY(), rect.y - scale(10));

        // showImage(getRobot().createScreenCapture(new Rectangle(x, y, width, height)));
    }

}
