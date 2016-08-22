package org.jdownloader.captcha.v2.challenge.adverigo;

import java.io.File;
import java.io.IOException;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.UserAgents;

import org.appwork.utils.StringUtils;

/**
 *
 * supports adverigo based captchas. at this time they have text 4 letter within image... but url structure indicates that they would also
 * have video!
 *
 * TODO: refresh image.
 *
 * @author raztoki
 *
 */
public class Adverigo {

    // preserve original browser
    private final Browser       br;
    // for Averigo related requests
    private Browser             abr;
    // session id
    private String              sid;
    // websites unique id
    private String              apiKey;

    // primary api server
    private static final String SERVER       = "http://api.adverigo.com";
    // captcha image
    private String              captchaAddress;
    // one job user-agent
    private String              userAgent    = UserAgents.stringUserAgent();
    // does the captcha provider allow clearling of original request referer?
    private boolean             clearReferer = true;

    public Adverigo(final Browser br) {
        this.br = br;
    }

    public Adverigo(final Browser br, final String apiKey) {
        this.br = br;
        this.apiKey = apiKey;
    }

    /**
     * @param apiKey
     *            the apiKey to set
     */
    public final void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public File downloadCaptcha(final File captchaFile) throws Exception {
        load();
        // referrer is always of original browser.
        abr = br.cloneBrowser();
        // headers are for an image
        abr.getHeaders().put("Accept", "image/webp,image/*,*/*;q=0.8");
        if (abr.getURL() == null || !Browser.getHost(abr.getURL()).contains("adverigo.com")) {
            // this prevents solvemedia group from seeing referrer
            if (clearReferer) {
                abr.getHeaders().put("Referer", "");
            }
        }
        URLConnectionAdapter con = null;
        try {
            Browser.download(captchaFile, con = abr.openGetConnection(this.captchaAddress));
        } catch (IOException e) {
            captchaFile.delete();
            throw e;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return captchaFile;
    }

    public void load() throws Exception {
        if (this.apiKey == null) {
            throw new Exception("Missing apiKey (hash)");
        }
        abr = br.cloneBrowser();
        abr.getHeaders().put("Accept", "*/*");
        abr.getHeaders().put("User-Agent", userAgent);

        if (abr.getURL() == null || !Browser.getHost(abr.getURL()).contains("adverigo.com")) {
            if (clearReferer) {
                abr.getHeaders().put("Referer", "");
            }
        }
        // we don't want videos!
        abr.getPage(SERVER + "/info.json?sid=&hash=" + this.apiKey + "&videoSupport=false&" + System.currentTimeMillis());
        // now find the sid
        setSid();
        // now set the captcha address!
        setCaptchaAddress();

        // might need these
        abr = br.cloneBrowser();
        abr.getHeaders().put("Accept", "*/*");
        abr.getHeaders().put("User-Agent", userAgent);

        if (abr.getURL() == null || !Browser.getHost(abr.getURL()).contains("adverigo.com")) {
            if (clearReferer) {
                abr.getHeaders().put("Referer", "");
            }
        }
        abr.getPage(SERVER + "/checkAdriver.json?sid=&hash=" + this.apiKey + "&videoSupport=false&" + System.currentTimeMillis());
        if (abr.getURL() == null || !Browser.getHost(abr.getURL()).contains("adverigo.com")) {
            if (clearReferer) {
                abr.getHeaders().put("Referer", "");
            }
        }
        abr = br.cloneBrowser();
        abr.getHeaders().put("Accept", "*/*");
        abr.getHeaders().put("User-Agent", userAgent);
        abr.getPage(SERVER + "/refresh.json?sid=&hash=" + this.apiKey + "&videoSupport=false&" + System.currentTimeMillis());
        // the SID gets larger after this request, if you don't do this.... then you get 500 response code on the captcha image!
        setSid();
        setCaptchaAddress();
    }

    private void setCaptchaAddress() {
        this.captchaAddress = SERVER + "/captcha.png?sid=" + this.sid + "&hash=" + this.apiKey;
    }

    /**
     * @param sid
     *            the sid to set
     * @throws Exception
     */
    private final void setSid() throws Exception {
        this.sid = PluginJSonUtils.getJsonValue(abr, "sid");
        if (StringUtils.isEmpty(this.sid)) {
            throw new Exception("Could not determine \"sid\"");
        }
    }

    public final boolean validateUserResponse(final String response) throws IOException {
        if (StringUtils.isEmpty(response)) {
            // throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        abr = br.cloneBrowser();
        abr.getHeaders().put("Accept", "*/*");
        abr.getHeaders().put("User-Agent", userAgent);
        abr.getPage(SERVER + "/verify.json?sid=" + sid + "&hash=" + this.apiKey + "&answer=" + Encoding.urlEncode(response) + "&" + System.currentTimeMillis());
        final boolean success = PluginJSonUtils.parseBoolean(PluginJSonUtils.getJsonValue(abr, "status"));
        return success;
    }

}
