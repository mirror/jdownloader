package org.jdownloader.captcha.v2.challenge.recaptcha.v1;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import jd.controlling.captcha.SkipRequest;
import jd.http.Browser;
import jd.plugins.Plugin;
import jd.plugins.hoster.DirectHTTP.Recaptcha;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.storage.JSonStorage;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.browser.AbstractBrowserChallenge;
import org.jdownloader.captcha.v2.solver.browser.BrowserReference;
import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;
import org.jdownloader.captcha.v2.solverjob.ResponseList;
import org.jdownloader.controlling.UniqueAlltimeID;

public abstract class RecaptchaV1Challenge extends AbstractBrowserChallenge {

    private String                        siteKey;
    private BasicCaptchaChallengeDelegate basicCaptchaChallenge;

    public String getSiteKey() {
        return siteKey;
    }

    @Override
    public BrowserViewport getBrowserViewport(BrowserWindow screenResource, Rectangle elementBounds) {

        Rectangle rect = screenResource.getRectangleByColor(0xff9900, 0, 0, 1d, 0, 0);

        return new Recaptcha1BrowserViewport(screenResource, rect);
    }

    @Override
    public boolean onPostRequest(BrowserReference browserReference, PostRequest request, HttpResponse response) throws IOException {
        String challenge = request.getParameterbyKey("recaptcha_challenge_field");
        String responseString = request.getParameterbyKey("recaptcha_response_field");

        if (StringUtils.isNotEmpty(challenge) && StringUtils.isNotEmpty(responseString)) {
            browserReference.onResponse(JSonStorage.serializeToJson(new String[] { challenge, responseString }));
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
    public boolean onGetRequest(BrowserReference browserReference, GetRequest request, HttpResponse response) throws IOException {
        String pDo = request.getParameterbyKey("do");
        if (pDo.equals("setChallenge")) {
            String challenge = request.getParameterbyKey("challenge");

            response.getOutputStream(true).write("Thanks".getBytes("UTF-8"));
        }
        return super.onGetRequest(browserReference, request, response);
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

    public static class BasicCaptchaChallengeDelegate extends BasicCaptchaChallenge {

        private RecaptchaV1Challenge actualChallenge;
        private String               challengeKey;

        public BasicCaptchaChallengeDelegate(RecaptchaV1Challenge recaptchaV1Challenge, File captchaFile, String challenge) {
            super("recaptcha", captchaFile, null, null, recaptchaV1Challenge.getPlugin(), 0);
            this.actualChallenge = recaptchaV1Challenge;
            this.challengeKey = challenge;
        }

        public String getChallengeKey() {
            return challengeKey;
        }

        //
        @Override
        public UniqueAlltimeID getId() {
            return actualChallenge.getId();
        }

        @Override
        public void setResult(ResponseList<String> result) {
            actualChallenge.setResult(result);
        }

        @Override
        public int getTimeout() {
            return actualChallenge.getTimeout();
        }

        @Override
        public boolean isAccountLogin() {
            return actualChallenge.isAccountLogin();
        }

        @Override
        public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
            return actualChallenge.canBeSkippedBy(skipRequest, solver, challenge);
        }
    }

    public synchronized BasicCaptchaChallengeDelegate getBasicCaptchaChallenge() {
        if (basicCaptchaChallenge != null) {
            return basicCaptchaChallenge;
        }
        try {
            File captchaFile = getPlugin().getLocalCaptchaFile();

            // final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            Browser br = new Browser();
            final Recaptcha rc = new Recaptcha(br);
            rc.setId(siteKey);

            AbstractBrowserChallenge dummyChallenge = new AbstractBrowserChallenge("recaptcha", getPlugin()) {

                @Override
                public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
                    return false;
                }

                @Override
                public String getHTML() {
                    String html;
                    try {
                        URL url = RecaptchaV1Challenge.class.getResource("recaptchaGetChallenge.html");
                        html = IO.readURLToString(url);

                        html = html.replace("%%%sitekey%%%", siteKey);
                        return html;
                    } catch (IOException e) {
                        throw new WTFException(e);
                    }
                }

                @Override
                public boolean onGetRequest(BrowserReference browserReference, GetRequest request, HttpResponse response) throws IOException {
                    String pDo = request.getParameterbyKey("do");
                    if (pDo.equals("setChallenge")) {
                        String url = request.getParameterbyKey("url");
                        rc.setChallenge(url);
                        response.getOutputStream(true).write("true".getBytes("UTF-8"));
                        synchronized (rc) {
                            rc.notifyAll();
                        }
                        return true;
                    }
                    return super.onGetRequest(browserReference, request, response);
                }

                @Override
                public BrowserViewport getBrowserViewport(BrowserWindow screenResource, Rectangle elementBounds) {
                    return null;
                }
            };
            BrowserReference ref = new BrowserReference(dummyChallenge) {

                @Override
                public void onResponse(String request) {
                }

            };
            ref.open();
            try {
                synchronized (rc) {
                    rc.wait(10000);
                }
            } finally {
                ref.dispose();
            }
            if (rc.getChallenge() == null) {
                rc.load();

            } else {
                rc.setCaptchaAddress(rc.getChallenge());
            }

            rc.downloadCaptcha(captchaFile);

            basicCaptchaChallenge = new BasicCaptchaChallengeDelegate(this, captchaFile, rc.getChallenge()) {

            };
            return basicCaptchaChallenge;
        } catch (Exception e) {
            e.printStackTrace();
            throw new WTFException(e);
        }
    }
}
