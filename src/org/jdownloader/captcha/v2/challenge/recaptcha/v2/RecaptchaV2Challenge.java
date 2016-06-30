package org.jdownloader.captcha.v2.challenge.recaptcha.v2;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.URL;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.exceptions.RemoteAPIException;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.phantomjs.Recaptcha2FallbackChallengeViaPhantomJS;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.browser.AbstractBrowserChallenge;
import org.jdownloader.captcha.v2.solver.browser.BrowserReference;
import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.phantomjs.PhantomJS;
import org.jdownloader.phantomjs.installation.InstallThread;

import jd.http.Browser;
import jd.plugins.Plugin;

public class RecaptchaV2Challenge extends AbstractBrowserChallenge {

    public static final String             RECAPTCHAV2 = "recaptchav2";

    private final String                   siteKey;
    private volatile BasicCaptchaChallenge basicChallenge;

    private final String                   siteDomain;

    private final String                   siteUrl;

    private final String                   secureToken;

    public String getSiteKey() {
        return siteKey;
    }

    @Override
    public BrowserViewport getBrowserViewport(BrowserWindow screenResource, Rectangle elementBounds) {
        Rectangle rect = null;
        int sleep = 500;
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            rect = screenResource.getRectangleByColor(0xff9900, 0, 0, 1d, elementBounds.x, elementBounds.y);
            if (rect == null) {
                sleep *= 2;

                continue;
            }
            break;
        }
        return new Recaptcha2BrowserViewport(screenResource, rect, elementBounds);
    }

    @Override
    public void cleanup() {
        super.cleanup();
        final BasicCaptchaChallenge basicChallenge = this.basicChallenge;
        if (basicChallenge != null) {
            basicChallenge.cleanup();
        }
    }

    public RecaptchaV2Challenge(String siteKey, String secureToken, Plugin pluginForHost, Browser br, String siteDomain, String siteUrl) {
        super(RECAPTCHAV2, pluginForHost);
        this.secureToken = secureToken;
        this.pluginBrowser = br;
        this.siteKey = siteKey;
        this.siteDomain = siteDomain;
        this.siteUrl = siteUrl;
        if (siteKey == null || !siteKey.matches("^[\\w-]+$")) {
            throw new WTFException("Bad SiteKey");
        }
    }

    public String getSecureToken() {
        return secureToken;
    }

    public String getSiteDomain() {
        return siteDomain;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    @Override
    public boolean onGetRequest(BrowserReference browserReference, GetRequest request, HttpResponse response) throws IOException, RemoteAPIException {
        String pDo = request.getParameterbyKey("do");
        if ("solve".equals(pDo)) {
            String responsetoken = request.getParameterbyKey("response");
            browserReference.onResponse(responsetoken);
            response.setResponseCode(ResponseCode.SUCCESS_OK);
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));
            response.getOutputStream(true).write("Please Close the Browser now".getBytes("UTF-8"));
            return true;
        }
        return false;
    }

    @Override
    public String getHTML() {
        try {
            final URL url = RecaptchaV2Challenge.class.getResource("recaptcha.html");
            String html = IO.readURLToString(url);
            html = html.replace("%%%sitekey%%%", siteKey);
            String stoken = getSecureToken();
            if (StringUtils.isNotEmpty(stoken)) {
                html = html.replace("%%%optionals%%%", "data-stoken=\"" + stoken + "\"");
            } else {
                html = html.replace("%%%optionals%%%", "");
            }
            return html;
        } catch (IOException e) {
            throw new WTFException(e);
        }
    }

    /**
     * Used to validate result against expected pattern. <br />
     * This is different to AbstractBrowserChallenge.isSolved, as we don't want to throw the same error exception.
     *
     * @param result
     * @return
     * @author raztoki
     */
    protected final boolean isCaptchaResponseValid() {
        String v = getResult().getValue();
        if (isSolved() && RecaptchaV2Challenge.isValidToken(v)) {
            return true;
        }
        return false;
    }

    public static boolean isValidToken(String v) {
        return v != null && v.matches("[\\w-]{30,}");
    }

    @Override
    public boolean validateResponse(AbstractResponse<String> response) {
        if (response.getPriority() <= 0) {
            return false;
        }
        return true;
    }

    @Override
    public void onHandled() {
        super.onHandled();
        final BasicCaptchaChallenge basicChallenge = this.basicChallenge;
        if (basicChallenge != null) {
            basicChallenge.onHandled();
        }
    }

    public synchronized BasicCaptchaChallenge createBasicCaptchaChallenge() {
        if (basicChallenge != null) {
            return basicChallenge;
        }
        final PhantomJS binding = new PhantomJS();
        if (!binding.isAvailable()) {
            try {
                InstallThread.install(null, _GUI.T.phantomjs_usage());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!binding.isAvailable()) {
            return null;
        }
        basicChallenge = new Recaptcha2FallbackChallengeViaPhantomJS(this); //
        return basicChallenge;

    }

}
