package org.jdownloader.captcha.v2.challenge.recaptcha.v1;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

import org.appwork.utils.Application;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
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

        Point oldloc = MouseInfo.getPointerInfo().getLocation();
        int clickX = recaptchaIframe.x + scale(22) + scale(Math.random() * 20);
        int clickY = recaptchaIframe.y + scale(32) + scale(Math.random() * 20);

        getRobot().mouseMove(clickX, clickY);

        getRobot().mousePress(InputEvent.BUTTON1_MASK);
        getRobot().mouseRelease(InputEvent.BUTTON1_MASK);

        getRobot().mouseMove(oldloc.x, oldloc.y);
        if (!Application.isJared(null)) {
            new Thread() {
                public void run() {
                    try {
                        long start = System.currentTimeMillis();
                        Thread.sleep(500);

                        while (System.currentTimeMillis() - start < 10000) {
                            captchaPopupRectangle = find();
                            if (captchaPopupRectangle != null) {
                                onFoundCaptchaRectangle(captchaPopupRectangle);
                                return;
                            }
                            Thread.sleep(500);

                        }

                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    protected void onFoundCaptchaRectangle(Rectangle rectangle) {

        if (rectangle.height < scale(350)) {
            // text
            image = getRobot().createScreenCapture(new Rectangle(rectangle.x + scale(1), rectangle.y + scale(69), rectangle.width - scale(3), scale(57)));
            showImage(image);
            image = IconIO.getScaledInstance(image, (int) (image.getWidth() / scale), (int) (image.getHeight() / scale));
            String text;
            try {
                text = Dialog.getInstance().showInputDialog(0, "enter", "", "", new ImageIcon(image), null, null);

                Point oldloc = MouseInfo.getPointerInfo().getLocation();
                int clickX = rectangle.x + scale(120) + scale(Math.random() * 48);
                int clickY = rectangle.y + scale(30) + scale(Math.random() * 20);

                getRobot().mouseMove(clickX, clickY);

                getRobot().mousePress(InputEvent.BUTTON1_MASK);
                getRobot().mouseRelease(InputEvent.BUTTON1_MASK);

                type(text);
                Thread.sleep(100);
                type('\n');
                getRobot().mouseMove(oldloc.x, oldloc.y);
            } catch (DialogClosedException e) {
                e.printStackTrace();
            } catch (DialogCanceledException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            // image click

            image = getRobot().createScreenCapture(new Rectangle(rectangle.x + scale(15), rectangle.y + scale(15), rectangle.width - scale(2 * 15), scale(495)));
            image = IconIO.getScaledInstance(image, (int) (image.getWidth() / scale), (int) (image.getHeight() / scale));
            showImage(image);
        }

    }

    public void type(CharSequence characters) {
        int length = characters.length();
        for (int i = 0; i < length; i++) {
            char character = characters.charAt(i);
            type(character);
        }
    }

    public void type(char character) {
        switch (character) {
        case 'a':
            doType(KeyEvent.VK_A);
            break;
        case 'b':
            doType(KeyEvent.VK_B);
            break;
        case 'c':
            doType(KeyEvent.VK_C);
            break;
        case 'd':
            doType(KeyEvent.VK_D);
            break;
        case 'e':
            doType(KeyEvent.VK_E);
            break;
        case 'f':
            doType(KeyEvent.VK_F);
            break;
        case 'g':
            doType(KeyEvent.VK_G);
            break;
        case 'h':
            doType(KeyEvent.VK_H);
            break;
        case 'i':
            doType(KeyEvent.VK_I);
            break;
        case 'j':
            doType(KeyEvent.VK_J);
            break;
        case 'k':
            doType(KeyEvent.VK_K);
            break;
        case 'l':
            doType(KeyEvent.VK_L);
            break;
        case 'm':
            doType(KeyEvent.VK_M);
            break;
        case 'n':
            doType(KeyEvent.VK_N);
            break;
        case 'o':
            doType(KeyEvent.VK_O);
            break;
        case 'p':
            doType(KeyEvent.VK_P);
            break;
        case 'q':
            doType(KeyEvent.VK_Q);
            break;
        case 'r':
            doType(KeyEvent.VK_R);
            break;
        case 's':
            doType(KeyEvent.VK_S);
            break;
        case 't':
            doType(KeyEvent.VK_T);
            break;
        case 'u':
            doType(KeyEvent.VK_U);
            break;
        case 'v':
            doType(KeyEvent.VK_V);
            break;
        case 'w':
            doType(KeyEvent.VK_W);
            break;
        case 'x':
            doType(KeyEvent.VK_X);
            break;
        case 'y':
            doType(KeyEvent.VK_Y);
            break;
        case 'z':
            doType(KeyEvent.VK_Z);
            break;
        case 'A':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_A);
            break;
        case 'B':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_B);
            break;
        case 'C':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_C);
            break;
        case 'D':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_D);
            break;
        case 'E':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_E);
            break;
        case 'F':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_F);
            break;
        case 'G':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_G);
            break;
        case 'H':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_H);
            break;
        case 'I':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_I);
            break;
        case 'J':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_J);
            break;
        case 'K':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_K);
            break;
        case 'L':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_L);
            break;
        case 'M':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_M);
            break;
        case 'N':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_N);
            break;
        case 'O':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_O);
            break;
        case 'P':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_P);
            break;
        case 'Q':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_Q);
            break;
        case 'R':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_R);
            break;
        case 'S':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_S);
            break;
        case 'T':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_T);
            break;
        case 'U':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_U);
            break;
        case 'V':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_V);
            break;
        case 'W':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_W);
            break;
        case 'X':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_X);
            break;
        case 'Y':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_Y);
            break;
        case 'Z':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_Z);
            break;
        case '`':
            doType(KeyEvent.VK_BACK_QUOTE);
            break;
        case '0':
            doType(KeyEvent.VK_0);
            break;
        case '1':
            doType(KeyEvent.VK_1);
            break;
        case '2':
            doType(KeyEvent.VK_2);
            break;
        case '3':
            doType(KeyEvent.VK_3);
            break;
        case '4':
            doType(KeyEvent.VK_4);
            break;
        case '5':
            doType(KeyEvent.VK_5);
            break;
        case '6':
            doType(KeyEvent.VK_6);
            break;
        case '7':
            doType(KeyEvent.VK_7);
            break;
        case '8':
            doType(KeyEvent.VK_8);
            break;
        case '9':
            doType(KeyEvent.VK_9);
            break;
        case '-':
            doType(KeyEvent.VK_MINUS);
            break;
        case '=':
            doType(KeyEvent.VK_EQUALS);
            break;
        case '~':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_BACK_QUOTE);
            break;
        case '!':
            doType(KeyEvent.VK_EXCLAMATION_MARK);
            break;
        case '@':
            doType(KeyEvent.VK_AT);
            break;
        case '#':
            doType(KeyEvent.VK_NUMBER_SIGN);
            break;
        case '$':
            doType(KeyEvent.VK_DOLLAR);
            break;
        case '%':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_5);
            break;
        case '^':
            doType(KeyEvent.VK_CIRCUMFLEX);
            break;
        case '&':
            doType(KeyEvent.VK_AMPERSAND);
            break;
        case '*':
            doType(KeyEvent.VK_ASTERISK);
            break;
        case '(':
            doType(KeyEvent.VK_LEFT_PARENTHESIS);
            break;
        case ')':
            doType(KeyEvent.VK_RIGHT_PARENTHESIS);
            break;
        case '_':
            doType(KeyEvent.VK_UNDERSCORE);
            break;
        case '+':
            doType(KeyEvent.VK_PLUS);
            break;
        case '\t':
            doType(KeyEvent.VK_TAB);
            break;
        case '\n':
            doType(KeyEvent.VK_ENTER);
            break;
        case '[':
            doType(KeyEvent.VK_OPEN_BRACKET);
            break;
        case ']':
            doType(KeyEvent.VK_CLOSE_BRACKET);
            break;
        case '\\':
            doType(KeyEvent.VK_BACK_SLASH);
            break;
        case '{':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_OPEN_BRACKET);
            break;
        case '}':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_CLOSE_BRACKET);
            break;
        case '|':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_BACK_SLASH);
            break;
        case ';':
            doType(KeyEvent.VK_SEMICOLON);
            break;
        case ':':
            doType(KeyEvent.VK_COLON);
            break;
        case '\'':
            doType(KeyEvent.VK_QUOTE);
            break;
        case '"':
            doType(KeyEvent.VK_QUOTEDBL);
            break;
        case ',':
            doType(KeyEvent.VK_COMMA);
            break;
        case '<':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_COMMA);
            break;
        case '.':
            doType(KeyEvent.VK_PERIOD);
            break;
        case '>':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_PERIOD);
            break;
        case '/':
            doType(KeyEvent.VK_SLASH);
            break;
        case '?':
            doType(KeyEvent.VK_SHIFT, KeyEvent.VK_SLASH);
            break;
        case ' ':
            doType(KeyEvent.VK_SPACE);
            break;
        default:
            throw new IllegalArgumentException("Cannot type character " + character);
        }
    }

    private void doType(int... keyCodes) {
        doType(keyCodes, 0, keyCodes.length);
    }

    private void doType(int[] keyCodes, int offset, int length) {
        if (length == 0) {
            return;
        }

        getRobot().keyPress(keyCodes[offset]);
        doType(keyCodes, offset + 1, length - 1);
        getRobot().keyRelease(keyCodes[offset]);
    }

    protected Rectangle find() {
        Rectangle spoken = getRectangleByColor(0xCCCCCC, scale(48), scale(48), 1d, scale(22), scale(32));

        if (spoken != null && spoken.height > scale(48) && spoken.width > scale(48)) {
            return spoken;
        }
        return null;
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
