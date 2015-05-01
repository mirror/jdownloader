package org.jdownloader.captcha.v2.challenge.confidentcaptcha;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Plugin;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.exceptions.BadRequestException;
import org.appwork.remoteapi.exceptions.RemoteAPIException;
import org.appwork.storage.JSonStorage;
import org.appwork.utils.IO;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.requests.KeyValuePair;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.captcha.v2.solver.browser.AbstractBrowserChallenge;
import org.jdownloader.captcha.v2.solver.browser.BrowserReference;

public abstract class ConfidentCaptchaChallenge extends AbstractBrowserChallenge {

    private String siteKey;

    public String getSiteKey() {
        return siteKey;
    }

    public ConfidentCaptchaChallenge(final Plugin plugin, final String siteKey) {
        super("confidentcaptcha", plugin);
        this.siteKey = siteKey;
        if (siteKey == null) {
            throw new WTFException("Bad SiteKey");
        }
    }

    @Override
    public boolean onPostRequest(BrowserReference browserReference, PostRequest request, HttpResponse response) throws IOException, RemoteAPIException {
        List<KeyValuePair> params = request.getPostParameter();
        ArrayList<String[]> t = new ArrayList<String[]>();
        if (params != null) {
            for (final KeyValuePair param : params) {
                // we want all.
                final String[] tt = { param.key, param.value };
                t.add(tt);
            }
        }

        if (t.size() == 0) {
            // some exception
            throw new BadRequestException("Missing 'confidentcaptcha' values!");
        }
        String[][] output = new String[t.size()][2];
        for (int i = 0; i != t.size(); i++) {
            output[i] = t.get(i);
        }
        browserReference.onResponse(JSonStorage.serializeToJson(output));
        response.setResponseCode(ResponseCode.SUCCESS_OK);
        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));

        response.getOutputStream(true).write("Please Close the Browser now".getBytes("UTF-8"));
        return true;
    }

    @Override
    public boolean onRawPostRequest(final BrowserReference browserReference, final PostRequest request, final HttpResponse response) throws IOException, RemoteAPIException {
        if (request.getRequestedURL().endsWith("/confidentincludes/callback.php")) {
            // we need to send this in jd as the referrer info in browser will be wrong!
            Browser c = getBr().cloneBrowser();
            for (HTTPHeader r : request.getRequestHeaders()) {
                // we just want the headers that sends in request.. we do this with our session browser for validation
                if (!r.getKey().equalsIgnoreCase(c.getHeaders().get(r.getKey()))) {
                    c.getHeaders().remove(r.getKey());
                }
            }
            // we want to add ajax stuff
            c.setHeader("Accept", "*/*");
            c.setHeader("X-Requested-With", "XMLHttpRequest");
            String postargs = "";
            final List<KeyValuePair> params = request.getPostParameter();
            for (final KeyValuePair param : params) {
                final String key = param.key;
                final String value = param.value;
                postargs += key + "=" + Encoding.urlEncode(value) + "&";
            }
            postargs = (String) postargs.subSequence(0, postargs.length() - 1);
            c.postPage("/confidentincludes/callback.php", postargs);
            response.setResponseCode(ResponseCode.SUCCESS_OK);
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));
            response.getOutputStream(true).write(c.getRequest().getHtmlCode().getBytes("UTF-8"));
            return true;
        }
        return false;
    }

    @Override
    public String getHTML() {
        String html;
        try {
            URL url = ConfidentCaptchaChallenge.class.getResource("confidentcaptcha.html");
            html = IO.readURLToString(url);

            html = html.replace("%%%sitekey%%%", siteKey);
            return html;
        } catch (IOException e) {
            throw new WTFException(e);
        }
    }
}
