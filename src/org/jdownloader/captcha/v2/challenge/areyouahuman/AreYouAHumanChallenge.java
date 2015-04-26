package org.jdownloader.captcha.v2.challenge.areyouahuman;

import java.awt.AWTException;
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

public abstract class AreYouAHumanChallenge extends AbstractBrowserChallenge {

    private String siteKey;

    public String getSiteKey() {
        return siteKey;
    }

    public AreYouAHumanChallenge(String siteKey, Plugin pluginForHost) {
        super("areyouahuman", pluginForHost);
        this.siteKey = siteKey;
        if (siteKey == null || !siteKey.matches("^[a-f0-9]{40}$")) {
            throw new WTFException("Bad SiteKey");
        }

    }

    @Override
    public boolean onPostRequest(BrowserReference browserReference, PostRequest request, HttpResponse response) throws IOException {

        String parameter = request.getParameterbyKey("session_secret");
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
            URL url = AreYouAHumanChallenge.class.getResource("areyouahuman.html");
            html = IO.readURLToString(url);

            html = html.replace("%%%sitekey%%%", siteKey);
            return html;
        } catch (IOException e) {
            throw new WTFException(e);
        }
    }
}
