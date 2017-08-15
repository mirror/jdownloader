package org.jdownloader.captcha.v2.challenge.solvemedia;

import java.io.File;
import java.io.IOException;

import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.UserAgents;

import org.appwork.utils.StringUtils;

public class SolveMedia {
    private final Browser      br;
    private String             challenge;
    private String             chId;
    private String             captchaAddress;
    private String             server;
    private String             path;
    public static final String FAIL_CAUSE_CKEY_MISSING = "SolveMedia Module fails --> Probably a host side bug/wrong key";
    private Form               verify;
    private boolean            secure                  = false;
    private boolean            noscript                = true;
    private boolean            clearReferer            = true;
    public Browser             smBr;

    public SolveMedia(final Browser br) {
        this.br = br;
    }

    public File downloadCaptcha(final File captchaFile) throws Exception {
        load();
        URLConnectionAdapter con = null;
        try {
            captchaAddress = captchaAddress.replaceAll("%0D%0A", "").trim();
            Browser.download(captchaFile, con = smBr.openGetConnection(server + captchaAddress));
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

    private void load() throws Exception {
        smBr = br.cloneBrowser();
        // this has to be first as it sets secure value based on results!
        if (this.challenge == null) {
            getChallengeKey();
        }
        // then this next...
        setServer();
        setPath();
        // solvemedia works off API key, and javascript. The imported browser session isn't actually needed.
        /*
         * Randomise user-agent to prevent tracking by solvemedia, each time we load(). Without this they could make the captchas images
         * harder read, the more a user requests captcha'. Also algos could track captcha requests based on user-agent globally, which means
         * JD default user-agent been very old (firefox 3.x) negatively biased to JD clients! Tracking takes place on based on IP address,
         * User-Agent, and APIKey of request (site of APIKey), cookies session submitted, and combinations of those. Effectively this can
         * all be done with a new browser, with regex tasks from source browser (ids|keys|submitting forms).
         */
        /* we first have to load the plugin, before we can reference it */
        smBr.getHeaders().put("User-Agent", UserAgents.stringUserAgent());
        if (smBr.getURL() == null || !smBr.getURL().contains("solvemedia.com/")) {
            // this prevents solvemedia group from seeing referrer
            if (clearReferer) {
                smBr.getHeaders().put("Referer", "");
            }
            // end of privacy protection
            // when we retry solving a solvemedia session, we reuse smBr, browser already contains the info we need!
            smBr.getPage(server + path + challenge);
        }
        if (smBr.containsHTML(">error: domain / ckey mismatch")) {
            throw new Exception(FAIL_CAUSE_CKEY_MISSING);
        }
        if (noscript) {
            verify = smBr.getForm(0);
            captchaAddress = smBr.getRegex("<img src=\"(/papi/media\\?c=[^\"]+)").getMatch(0);
            if (captchaAddress == null) {
                captchaAddress = smBr.getRegex("src=\"(/papi/media\\?c=[^\"]+)").getMatch(0);
            }
            if (verify == null) {
                throw new Exception("SolveMedia Module fails");
            }
        } else {
            chId = smBr.getRegex("\"chid\"\\s+?:\\s+?\"(.*?)\",").getMatch(0);
            captchaAddress = chId != null ? "/papi/media?c=" + chId : null;
        }
        if (captchaAddress == null) {
            throw new Exception("SolveMedia Module fails");
        }
    }

    private void getChallengeKey() {
        challenge = br.getRegex("http://api\\.solvemedia\\.com/papi/_?challenge\\.script\\?k=(.{32})").getMatch(0);
        if (challenge == null) {
            // when we retry solving a solvemedia session.
            challenge = smBr.getRegex("<input type=hidden name=\"k\" value=\"([^\"]+)\">").getMatch(0);
        }
        if (challenge == null) {
            secure = true;
            challenge = br.getRegex("ckey:\'([\\w\\-\\.]+)\'").getMatch(0);
            if (challenge == null) {
                challenge = br.getRegex("https://api-secure\\.solvemedia\\.com/papi/_?challenge\\.script\\?k=(.{32})").getMatch(0);
            }
            if (challenge == null) {
                secure = false;
            }
        }
    }

    public String getChallenge() {
        if (captchaAddress == null) {
            return null;
        }
        return new Regex(captchaAddress, "/papi/media\\?c=(.*?)$").getMatch(0);
    }

    public String getChallenge(final String code) throws Exception {
        if (!noscript) {
            return chId;
        }
        if (StringUtils.isEmpty(code)) {
            // empty responses are not valid.
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        verify.put("adcopy_response", Encoding.urlEncode(code));
        // for backup purposes.
        final Browser smbr = smBr.cloneBrowser();
        final int retry = 4;
        for (int i = 0; i != retry; i++) {
            smBr = smbr.cloneBrowser();
            try {
                // less common here..
                smBr.submitForm(verify);
            } catch (BrowserException e) {
                // should cover socket related issues.
                if (i + 1 != retry) {
                    continue;
                }
                throw new Exception("SolveMedia Module fails");
            }
            break;
        }
        String verifyUrl = smBr.getRegex("URL=(http[^\"]+)").getMatch(0);
        if (verifyUrl == null) {
            return null;
        }
        String challenge = null;
        for (int i = 0; i != retry; i++) {
            smBr = smbr.cloneBrowser();
            try {
                // very common to get errors here! lets try a crude retry!
                smBr.getPage(verifyUrl);
            } catch (BrowserException e) {
                // should cover socket related issues.
                if (i + 1 != retry) {
                    continue;
                }
                throw new Exception("SolveMedia Module fails");
            }
            break;
        }
        /**
         * incorrect responses are validated by the server! for instance<br/>
         * they have redirect url with error=\d+ within when incorrect <br/>
         * and html <span id="adcopy-error-msg">Try again\s*</span> <br/>
         * challenge/gibberish will be null
         *
         */
        challenge = smBr.getRegex("id=gibberish>([^<]+)").getMatch(0);
        return challenge;
    }

    public Browser getBr() {
        return smBr;
    }

    /**
     * @default false
     * @parameter if true uses "https://api-secure.solvemedia.com" instead of "http://api.solvemedia.com"
     */
    public void setSecure(final boolean secure) {
        if (secure != this.secure) {
            this.secure = secure;
        }
    }

    /**
     * @default true
     * @parameter if false uses "_challenge.js" instead of "challenge.noscript" as url path
     */
    public void setNoscript(final boolean noscript) {
        if (noscript != this.noscript) {
            this.noscript = noscript;
        }
    }

    private void setServer() {
        server = "http://api.solvemedia.com";
        if (secure) {
            server = "https://api-secure.solvemedia.com";
        }
    }

    private void setPath() {
        path = "/papi/challenge.noscript?k=";
        if (!noscript) {
            path = "/papi/_challenge.js?k=";
        }
    }

    public void setChallengeKey(final String challengeKey) {
        this.challenge = challengeKey;
    }

    public String getChallengeUrl() {
        return server + path;
    }

    public String getChallengeId() {
        return challenge;
    }

    public String getCaptchaUrl() {
        return captchaAddress;
    }

    public void setClearReferer(final boolean clearReferer) {
        this.clearReferer = clearReferer;
    }
}