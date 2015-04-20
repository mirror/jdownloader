package org.jdownloader.captcha.v2.challenge.recaptcha.v2;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.URL;

import jd.plugins.Plugin;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.IO;
import org.jdownloader.captcha.v2.solver.browser.AbstractBrowserChallenge;
import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;

public abstract class RecaptchaV2Challenge extends AbstractBrowserChallenge {

    private String siteKey;

    public String getSiteKey() {
        return siteKey;
    }

    @Override
    public BrowserViewport getBrowserViewport(BrowserWindow screenResource) {

        Rectangle rect = screenResource.getRectangleByColor(0xff9900, 0, 0, 1d, 0, 0);

        return new Recaptcha2BrowserViewport(screenResource, rect);
    }

    public RecaptchaV2Challenge(String siteKey, Plugin pluginForHost) {
        super("recaptchav2", pluginForHost);
        this.siteKey = siteKey;
        if (siteKey == null || !siteKey.matches("^[\\w-]+$")) {
            throw new WTFException("Bad SiteKey");
        }

    }

    @Override
    public String getHTML() {
        String html;
        try {
            URL url = RecaptchaV2Challenge.class.getResource("recaptcha.html");
            html = IO.readURLToString(url);

            html = html.replace("%%%sitekey%%%", siteKey);
            return html;
        } catch (IOException e) {
            throw new WTFException(e);
        }
    }
}
