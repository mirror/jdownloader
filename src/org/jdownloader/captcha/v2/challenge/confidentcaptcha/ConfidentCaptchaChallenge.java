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
import org.appwork.utils.net.httpserver.requests.HttpRequest;
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
    protected String getCaptchaNameSpace() {
        return "confident";
    }

    @Override
    public boolean onRawPostRequest(final BrowserReference browserReference, final PostRequest request, final HttpResponse response) throws IOException, RemoteAPIException {
        if (request.getRequestedURL().endsWith("/confidentincludes/callback.php")) {
            // we need to send this in jd as the referrer info in browser will be wrong!
            final Browser c = new Browser();
            c.getHeaders().put("Referer", getPluginBrowser().getURL());
            for (final HTTPHeader header1 : getPluginBrowser().getRequest().getHeaders()) {
                // we just want the headers that sends in request.. we do this with our session browser for validation
                // first add all existing browser headers
                final String h1K = header1.getKey();
                final String h1V = header1.getValue();
                c.getHeaders().put(h1K, h1V);
            }
            // now put what's in user browser back into next request.
            for (final HTTPHeader header2 : request.getRequestHeaders()) {
                final String h2K = header2.getKey();
                final String h2V = header2.getValue();
                final String hcV = c.getHeaders().get(h2K);
                if (!h2V.contains("127.0.0.1") && !h2K.equals("User-Agent") && !h2K.equals("Connection") && !h2K.equals("Content-Length")) {
                    if (!h2K.equals(hcV)) {
                        c.getHeaders().put(h2K, h2V);
                    }
                }
            }
            // now delete stuff that shouldn't be present in c
            for (final HTTPHeader header3 : c.getHeaders()) {
                final String h3K = header3.getKey();
                final String r3V = request.getRequestHeaders().getValue(h3K);
                if (r3V == null) {
                    c.getHeaders().remove(h3K);
                }
            }
            String postargs = "";
            final List<KeyValuePair> params = request.getPostParameter();
            for (final KeyValuePair param : params) {
                final String key = param.key;
                final String value = param.value;
                postargs += key + "=" + Encoding.urlEncode(value) + "&";
            }
            postargs = (String) postargs.subSequence(0, postargs.length() - 1);
            c.postPage(getPluginBrowser().getBaseURL() + "confidentincludes/callback.php", postargs);
            response.setResponseCode(ResponseCode.SUCCESS_OK);
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));
            response.getOutputStream(true).write(c.getRequest().getHtmlCode().getBytes("UTF-8"));
            return true;
        }
        return false;
    }

    @Override
    public String getHTML(HttpRequest request, String id) {
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
