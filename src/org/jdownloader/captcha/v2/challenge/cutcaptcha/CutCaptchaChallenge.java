package org.jdownloader.captcha.v2.challenge.cutcaptcha;

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
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.solver.browser.AbstractBrowserChallenge;
import org.jdownloader.captcha.v2.solver.browser.BrowserReference;

import jd.plugins.Plugin;

public abstract class CutCaptchaChallenge extends AbstractBrowserChallenge {
    private final String siteKey;
    private final String apiKey;

    public String getSiteKey() {
        return siteKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getSiteUrl() {
        return this.getPluginBrowser().getURL();
    }

    public CutCaptchaChallenge(final Plugin plugin, final String siteKey, final String apiKey) {
        super("cutcaptcha", plugin);
        if (!looksLikeValidSiteKey(siteKey)) {
            // default: SAs61IAI
            throw new WTFException("Bad SiteKey:" + siteKey);
        } else if (!looksLikeValidApiKey(apiKey)) {
            throw new WTFException("Bad APIKey:" + apiKey);
        }
        this.siteKey = siteKey;
        this.apiKey = apiKey;
    }

    private static boolean looksLikeValidSiteKey(final String siteKey) {
        if (siteKey == null) {
            return false;
        } else if (siteKey.matches("^[a-f0-9]{40}$")) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean looksLikeValidApiKey(final String siteKey) {
        if (siteKey == null) {
            return false;
        } else if (siteKey.matches("^[\\w-]{5,}$")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onGetRequest(BrowserReference browserReference, GetRequest request, HttpResponse response) throws IOException, RemoteAPIException {
        String parameter = request.getParameterbyKey("response");
        if (StringUtils.isNotEmpty(parameter)) {
            browserReference.onResponse(parameter);
            response.setResponseCode(ResponseCode.SUCCESS_OK);
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));
            response.getOutputStream(true).write("Please Close the Browser now".getBytes("UTF-8"));
            return true;
        }
        return super.onGetRequest(browserReference, request, response);
    }

    @Override
    public boolean onPostRequest(BrowserReference browserReference, PostRequest request, HttpResponse response) throws IOException, RemoteAPIException {
        return false;
    }

    @Override
    public String getHTML(HttpRequest request, String id) {
        String html;
        try {
            URL url = CutCaptchaChallenge.class.getResource("cutcaptcha.html");
            html = IO.readURLToString(url);
            html = html.replace("%%%sitekey%%%", siteKey);
            return html;
        } catch (IOException e) {
            throw new WTFException(e);
        }
    }

    protected final boolean isCaptchaResponseValid() {
        final String v = getResult().getValue();
        if (isSolved() && looksLikeValidToken(v)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected String getCaptchaNameSpace() {
        return "cut";
    }

    public static boolean looksLikeValidToken(String v) {
        return v != null && v.matches("[\\w-]{10,}");
    }

    @Override
    public boolean validateResponse(AbstractResponse<String> response) {
        return super.validateResponse(response) && looksLikeValidToken(response.getValue());
    }
}
