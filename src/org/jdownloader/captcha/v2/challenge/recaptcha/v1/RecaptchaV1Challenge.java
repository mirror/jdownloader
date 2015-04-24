package org.jdownloader.captcha.v2.challenge.recaptcha.v1;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.URL;

import jd.plugins.Plugin;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.utils.IO;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.jdownloader.captcha.v2.solver.browser.AbstractBrowserChallenge;
import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;

public abstract class RecaptchaV1Challenge extends AbstractBrowserChallenge {

    private String siteKey;

    public String getSiteKey() {
        return siteKey;
    }

    @Override
    public BrowserViewport getBrowserViewport(BrowserWindow screenResource) {

        Rectangle rect = screenResource.getRectangleByColor(0xff9900, 0, 0, 1d, 0, 0);

        return new Recaptcha1BrowserViewport(screenResource, rect);
    }

    @Override
    public String handleRequest(PostRequest request) throws IOException {
        String challenge = request.getParameterbyKey("recaptcha_challenge_field");
        String responseString = request.getParameterbyKey("recaptcha_response_field");

        return JSonStorage.serializeToJson(new String[] { challenge, responseString });
    }

    public RecaptchaV1Challenge(String siteKey, Plugin pluginForHost) {
        super("recaptchav1", pluginForHost);

        if (siteKey == null || !siteKey.trim().matches("^[\\w]+$")) {
            throw new WTFException("Bad SiteKey");
        }
        this.siteKey = siteKey.trim();
    }

    @Override
    public String getHTML() {
        String html;
        try {
            URL url = RecaptchaV1Challenge.class.getResource("recaptcha.html");
            html = IO.readURLToString(url);

            html = html.replace("%%%sitekey%%%", siteKey);
            return html;
        } catch (IOException e) {
            throw new WTFException(e);
        }
    }

}
