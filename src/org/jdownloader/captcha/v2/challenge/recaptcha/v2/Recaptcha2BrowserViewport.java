package org.jdownloader.captcha.v2.challenge.recaptcha.v2;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;

import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;

public class Recaptcha2BrowserViewport extends BrowserViewport {
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
        try {
            Thread.sleep((long) (Math.random() * 1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Point oldloc = MouseInfo.getPointerInfo().getLocation();
        int clickX = recaptchaIframe.x + scale(22) + scale(Math.random() * 20);
        int clickY = recaptchaIframe.y + scale(32) + scale(Math.random() * 20);
        // System.out.println("Press " + clickX + ":" + clickY);
        getRobot().mouseMove(clickX, clickY);
        // first click ensure focus
        getRobot().mousePress(InputEvent.BUTTON1_MASK);
        getRobot().mouseRelease(InputEvent.BUTTON1_MASK);
        // second click: press button
        getRobot().mousePress(InputEvent.BUTTON1_MASK);
        getRobot().mouseRelease(InputEvent.BUTTON1_MASK);
        getRobot().mouseMove(oldloc.x, oldloc.y);
        // if (!Application.isJared(null)) {
        // new Thread() {
        // public void run() {
        // try {
        // long start = System.currentTimeMillis();
        // Thread.sleep(500);
        //
        // while (System.currentTimeMillis() - start < 10000) {
        // captchaPopupRectangle = find();
        // if (captchaPopupRectangle != null) {
        // onFoundCaptchaRectangle(captchaPopupRectangle);
        // return;
        // }
        // Thread.sleep(500);
        //
        // }
        //
        // } catch (Throwable e) {
        // e.printStackTrace();
        // }
        // }
        // }.start();
        // }
    }
    // protected void onFoundCaptchaRectangle(Rectangle rectangle) {
    //
    // if (rectangle.height < scale(350)) {
    // // text
    // image = getRobot().createScreenCapture(new Rectangle(rectangle.x + scale(1), rectangle.y + scale(69), rectangle.width - scale(3),
    // scale(57)));
    // showImage(image, null);
    // image = IconIO.getScaledInstance(image, (int) (image.getWidth() / scale), (int) (image.getHeight() / scale));
    // String text;
    // try {
    // text = Dialog.getInstance().showInputDialog(0, "enter", "", "", new ImageIcon(image), null, null);
    //
    // Point oldloc = MouseInfo.getPointerInfo().getLocation();
    // int clickX = rectangle.x + scale(120) + scale(Math.random() * 48);
    // int clickY = rectangle.y + scale(30) + scale(Math.random() * 20);
    //
    // getRobot().mouseMove(clickX, clickY);
    //
    // getRobot().mousePress(InputEvent.BUTTON1_MASK);
    // getRobot().mouseRelease(InputEvent.BUTTON1_MASK);
    //
    // // type(text);
    // // Thread.sleep(100);
    // // type('\n');
    // getRobot().mouseMove(oldloc.x, oldloc.y);
    // } catch (DialogClosedException e) {
    // e.printStackTrace();
    // } catch (DialogCanceledException e) {
    // e.printStackTrace();
    // }
    // } else {
    // // image click
    //
    // image = getRobot().createScreenCapture(new Rectangle(rectangle.x + scale(15), rectangle.y + scale(15), rectangle.width - scale(2 *
    // 15), scale(495)));
    // image = IconIO.getScaledInstance(image, (int) (image.getWidth() / scale), (int) (image.getHeight() / scale));
    // showImage(image, null);
    // }
    //
    // }

    // protected Rectangle find() {
    // Rectangle spoken = getRectangleByColor(0xCCCCCC, scale(48), scale(48), 1d, scale(22), scale(32));
    //
    // if (spoken != null && spoken.height > scale(48) && spoken.width > scale(48)) {
    // return spoken;
    // }
    // return null;
    // }
    public Recaptcha2BrowserViewport(BrowserWindow screenResource, Rectangle rect, Rectangle elementBounds) {
        super(screenResource);
        recaptchaIframe = rect;
        scale = recaptchaIframe.width / 306d;
        this.width = (int) (screenResource.getViewportWidth() * scale);
        this.height = (int) (screenResource.getViewportHeight() * scale);
        if (elementBounds == null) {
            this.x = Math.max(screenResource.getX(), rect.x - scale(48));
            this.y = Math.max(screenResource.getY(), rect.y - scale(164));
        } else {
            this.x = rect.x - elementBounds.x;
            this.y = rect.y - elementBounds.y;
        }
        // showImage(getRobot().createScreenCapture(new Rectangle(x, y, width, height)), null);
        // 48 164
    }
}
