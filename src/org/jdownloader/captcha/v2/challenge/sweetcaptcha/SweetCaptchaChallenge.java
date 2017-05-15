package org.jdownloader.captcha.v2.challenge.sweetcaptcha;

import java.io.IOException;
import java.net.URL;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.exceptions.RemoteAPIException;
import org.appwork.storage.JSonStorage;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.captcha.v2.solver.browser.AbstractBrowserChallenge;
import org.jdownloader.captcha.v2.solver.browser.BrowserReference;

import jd.plugins.Plugin;

public abstract class SweetCaptchaChallenge extends AbstractBrowserChallenge {
    private String siteKey;
    private String appKey;

    public final String getAppKey() {
        return appKey;
    }

    public String getSiteKey() {
        return siteKey;
    }

    public SweetCaptchaChallenge(final String siteKey, final String appKey, final Plugin pluginForHost) {
        super("sweetcaptcha", pluginForHost);
        this.siteKey = siteKey;
        this.appKey = appKey;
        if (siteKey == null || !siteKey.matches("^sc_[a-f0-9]{7}$")) {
            throw new WTFException("Bad SiteKey");
        }
    }

    @Override
    public boolean onPostRequest(BrowserReference browserReference, PostRequest request, HttpResponse response) throws IOException, RemoteAPIException {
        final String sckey = request.getParameterbyKey("sckey");
        final String scvalue = request.getParameterbyKey("scvalue");
        final String scvalue2 = request.getParameterbyKey("scvalue2");
        if (StringUtils.isNotEmpty(sckey) && StringUtils.isNotEmpty(scvalue) && StringUtils.isNotEmpty(scvalue2)) {
            browserReference.onResponse(JSonStorage.serializeToJson(new String[][] { { "sckey", sckey }, { "scvalue", scvalue }, { "scvalue2", scvalue2 } }));
            response.setResponseCode(ResponseCode.SUCCESS_OK);
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));
            response.getOutputStream(true).write("Please Close the Browser now".getBytes("UTF-8"));
            return true;
        }
        return false;
    }

    @Override
    public String getHTML(String id) {
        String html;
        try {
            URL url = SweetCaptchaChallenge.class.getResource("sweetcaptcha.html");
            html = IO.readURLToString(url);
            html = html.replace("%%%sitekey%%%", siteKey).replace("%%%appkey%%%", appKey);
            return html;
        } catch (IOException e) {
            throw new WTFException(e);
        }
    }
}
