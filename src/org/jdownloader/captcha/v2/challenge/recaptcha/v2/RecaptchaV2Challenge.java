package org.jdownloader.captcha.v2.challenge.recaptcha.v2;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URL;

import jd.plugins.Plugin;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.captcha.v2.solver.browser.AbstractBrowserChallenge;
import org.jdownloader.captcha.v2.solver.browser.BrowserReference;
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
    public boolean onPostRequest(BrowserReference browserReference, PostRequest request, HttpResponse response) throws IOException {
        String parameter = request.getParameterbyKey("g-recaptcha-response");

        if (StringUtils.isNotEmpty(parameter)) {
            browserReference.onResponse(parameter);
            response.setResponseCode(ResponseCode.SUCCESS_OK);
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));

            response.getOutputStream(true).write("Please Close the Browser now".getBytes("UTF-8"));
            // Close Browser Tab
            Robot robot;
            try {
                robot = new Robot();

                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_W);

                robot.keyRelease(KeyEvent.VK_CONTROL);
                robot.keyRelease(KeyEvent.VK_W);
            } catch (AWTException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
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
