package org.jdownloader.captcha.v2.challenge.recaptcha.v2.phantomjs;

import java.awt.image.BufferedImage;

public class Payload {
    public Payload(BufferedImage img, String url2) {
        this.image = img;
        this.url = url2;
    }

    public final BufferedImage image;
    public final String        url;

}