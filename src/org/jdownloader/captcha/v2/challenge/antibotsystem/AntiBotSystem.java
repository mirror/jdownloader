package org.jdownloader.captcha.v2.challenge.antibotsystem;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.appwork.utils.StringUtils;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.UserAgents;

/**
 *
 * supports <a href="antibotsystem.com">antibotsystem.com</a> based captchas.<br />
 * based off video(mp4/webm) and fail over of 4 digit captcha static image(jpg). This implementation focuses on image.
 *
 * TODO: refresh image.
 *
 * @author raztoki
 *
 */
public class AntiBotSystem {

    // preserve original browser
    private final Browser br;
    // for antibotsystem related requests
    private Browser       abr;
    // captcha always within form, we can either provide form or find it ourselves.
    private Form          form;
    // for the api/site key
    private String        siteKey;
    // captcha image
    private String        captchaAddress;
    // one job user-agent
    private String        userAgent    = UserAgents.stringUserAgent();
    // does the captcha provider allow clearling of original request referer?
    private boolean       clearReferer = true;
    // job start time.
    private final long    jobStarted   = System.currentTimeMillis();

    public AntiBotSystem(final Browser br) {
        this.br = br;
    }

    public AntiBotSystem(final Browser br, final Form form) {
        this.br = br;
        this.form = form;
    }

    public File downloadCaptcha(final File captchaFile) throws Exception {
        load();
        // referrer is always of original browser.
        abr = br.cloneBrowser();
        // headers are for an image
        abr.getHeaders().put("Accept", "image/webp,image/*,*/*;q=0.8");
        // this prevents captcha provider from seeing referrer
        if (clearReferer) {
            abr.getHeaders().put("Referer", "");
        }
        // set temp user-agent
        abr.getHeaders().put("User-Agent", userAgent);
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
        // feedback in browser post image download
        processFeedback();
        return captchaFile;
    }

    /**
     * they validate one or both of these, it will always be invalid on the hoster end on submission of form.
     */
    private void processFeedback() throws IOException {
        abr = br.cloneBrowser();
        if (clearReferer) {
            // here the referrer can not be missing... otherwise errors happen, this is best solution.
            abr.getHeaders().put("Referer", new Regex(br.getURL(), "https?://[^/]*").getMatch(-1));
        }
        abr.clearCookies("antibotsystem.com/");
        abr.getHeaders().put("User-Agent", userAgent);
        abr.getHeaders().put("Accept", "*/*");
        abr.getHeaders().put("Cache-Control", "");
        final String callback = "jQuery" + new Random().nextLong() + "_" + jobStarted;
        {
            final Browser fb = abr.cloneBrowser();
            // videocaptcha_token is also within the image url path.
            final String r = "[\"" + this.siteKey + "\",[[\"validate_site_key\",{\"token\":\"" + this.form.getInputField("videocaptcha_token").getValue() + "\"}]]]";
            fb.getPage("//antibotsystem.com/data?callback=" + callback + "&r=" + Encoding.urlEncode(r) + "&_" + System.currentTimeMillis());
        }
        abr.clearCookies("antibotsystem.com/");
        {
            final Browser fb = abr.cloneBrowser();
            final String r = "[\"" + this.siteKey + "\",[[\"setPlay\",{\"key\":\"" + this.form.getInputField("videocaptcha_skey").getValue() + "\",\"token\":\"" + this.form.getInputField("videocaptcha_token").getValue() + "\"}]]]";
            fb.getPage("//antibotsystem.com/stat?callback=" + callback + "&r=" + Encoding.urlEncode(r) + "&_" + System.currentTimeMillis());
        }

    }

    public void load() throws Exception {
        // if form not found lets find it
        if (this.form == null) {
            findForm();
        }
        // find the captcha image.
        findCaptchaImage();
        // feedback, requires key
        findSiteKey();
    }

    private void findSiteKey() throws PluginException {
        this.siteKey = new Regex(this.form.getHtmlCode(), "key\\s*:\\s*'([a-f0-9]{32})'").getMatch(0);
        if (this.siteKey == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private void findCaptchaImage() throws PluginException {
        // we look inside form!
        final String[] imgs = new Regex(this.form.getHtmlCode(), "<img[^>]+>").getColumn(-1);
        if (imgs != null) {
            for (final String img : imgs) {
                if (new Regex(img, "class=(\"|')mcmp_img\\1").matches()) {
                    this.captchaAddress = new Regex(img, "\\s+src\\s*=\\s*('|\")(.*?\\.jpg)\\1").getMatch(1);
                    if (this.captchaAddress != null) {
                        return;
                    }
                }
            }
        }
        if (this.captchaAddress == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private final void findForm() throws PluginException {
        // find on original browser.
        final Form[] forms = br.getForms();
        for (final Form form : forms) {
            if (containsAntiBotSystem(form)) {
                this.form = form;
            }
        }
        if (this.form == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    public static final boolean containsAntiBotSystem(final Form form) {
        final boolean containsAntibotsystemUrl = form.containsHTML("://(\\w+\\.)?antibotsystem\\.com/");
        if (!containsAntibotsystemUrl) {
            return false;
        }
        // two points of check
        final List<InputField> inputFields = form.getInputFields();
        if (inputFields != null) {
            for (InputField i : inputFields) {
                if (i.getKey().matches("videocaptcha_.+")) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setResponse(final String captchaCode) throws PluginException {
        if (StringUtils.isEmpty(captchaCode)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.form.put("videocaptcha_word", Encoding.urlEncode(captchaCode));
        // you seem to need to also set cookie at least with relink.to.
        this.br.setCookie(this.br.getHost(), "vcaptcha_skey", this.form.getInputField("videocaptcha_skey").getValue());
    }

}
