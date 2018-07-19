//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

/**
 *
 * @author raztoki
 * @tags: similar to OuoIo
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "coinlink.co", "zlshorte.net", "igram.im", "bit-url.com", "adbilty.me", "linclik.com", "oke.io", "vivads.net", "cut-urls.com", "pnd.tl", "met.bz", "urlcloud.us" }, urls = { "https?://(?:www\\.)?(?:coinlink\\.co|adlink\\.guru|short\\.es|tmearn\\.com|ibly\\.co|adshort\\.(?:co|me|im)|brlink\\.in|urle\\.co|mitly\\.us|cutwin\\.com)/[A-Za-z0-9]+$", "https?://(?:www\\.)?zlshorte\\.net/[A-Za-z0-9]{4,}", "https?://(?:www\\.)?i?gram\\.im/[A-Za-z0-9]{4,}", "https?://(?:www\\.)?bit\\-url\\.com/[A-Za-z0-9]{4,}", "https?://(?:www\\.)?adbilty\\.me/[A-Za-z0-9]{4,}", "https?://(?:www\\.)?linclik\\.com/[A-Za-z0-9]{4,}", "https?://(?:www\\.)?oke\\.io/[A-Za-z0-9]{4,}", "https?://(?:www\\.)?vivads\\.net/[A-Za-z0-9]{4,}", "https?://(?:www\\.)?(?:cuon\\.io|curs\\.io|cut\\-urls\\.com)/[A-Za-z0-9]{4,}",
        "https?://(?:www\\.)?pnd\\.tl/[A-Za-z0-9]{4,}", "https?://met\\.bz/[A-Za-z0-9]{4,}", "https?://(?:www\\.)?urlcloud\\.us/[A-Za-z0-9]{4,}" })
public class MightyScriptAdLinkFly extends antiDDoSForDecrypt {
    @Override
    public String[] siteSupportedNames() {
        return new String[] { "coinlink.co", "adlink.guru", "short.es", "tmearn.com", "ibly.co", "adshort.co", "adshort.me", "adshort.im", "brlink.in", "urle.co", "mitly.us", "cutwin.com", "zlshorte.net", "igram.im", "bit-url.com", "adbilty.me", "linclik.com", "oke.io", "vivads.net", "cut-urls.com", "pnd.tl", "met.bz", "urlcloud.us" };
    }

    public enum CaptchaType {
        reCaptchaV2,
        solvemedia,
        WTF
    };

    public MightyScriptAdLinkFly(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    private String appVars = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String source_host = Browser.getHost(parameter);
        br.setFollowRedirects(false);
        getPage(parameter);
        final String redirect = br.getRedirectLocation();
        if (redirect != null && !redirect.contains(source_host + "/")) {
            /* 2018-07-18: Direct redirect without captcha or any Form e.g. vivads.net */
            decryptedLinks.add(this.createDownloadlink(redirect));
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        if (redirect != null) {
            br.getPage(redirect);
        }
        if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        appVars = br.getRegex("var (app_vars.*?)</script>").getMatch(0);
        Form form = getCaptchaForm();
        if (form == null) {
            return null;
        }
        if (form.hasInputFieldByName("captcha")) {
            /* original captcha/ VERY OLD way! [2018-07-18: Very rare or non existent anymore!] */
            final String code = getCaptchaCode("cp.php", param);
            form.put("captcha", Encoding.urlEncode(code));
            submitForm(form);
            if (br.containsHTML("<script>alert\\('(?:Empty Captcha|Incorrect Captcha)\\s*!'\\);")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            form = br.getForm(0);
            if (form == null) {
                return null;
            }
            // we want redirect off here
            br.setFollowRedirects(false);
            submitForm(form);
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                if (br.containsHTML("<script>alert\\('(?:Link not found)\\s*!'\\);")) {
                    // invalid link
                    logger.warning("Invalid link : " + parameter);
                    return decryptedLinks;
                }
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            /* 2018-07-18: Not all sites require a captcha to be solved - e.g. no captcha: met.bz */
            if (evalulateCaptcha()) {
                /* Captcha required */
                boolean requiresCaptchaWhichCanFail = false;
                boolean captchaFailed = false;
                for (int i = 0; i <= 2; i++) {
                    form = getCaptchaForm();
                    if (form == null) {
                        return null;
                    }
                    final CaptchaType captchaType = getCaptchaType();
                    if (captchaType == CaptchaType.reCaptchaV2) {
                        requiresCaptchaWhichCanFail = false;
                        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br) {
                            @Override
                            public String getSiteKey() {
                                final String key = getAppVarsResult("reCAPTCHA_site_key");
                                if (!StringUtils.isEmpty(key)) {
                                    return key;
                                }
                                /* Use general function */
                                return super.getSiteKey(br.toString());
                            }
                        }.getToken();
                        form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    } else if (captchaType == CaptchaType.solvemedia) {
                        final String solvemediaChallengeKey = this.getAppVarsResult("solvemedia_challenge_key");
                        if (StringUtils.isEmpty(solvemediaChallengeKey)) {
                            logger.warning("Failed to find solvemedia_challenge_key");
                            return null;
                        }
                        requiresCaptchaWhichCanFail = true;
                        final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                        sm.setChallengeKey(solvemediaChallengeKey);
                        File cf = null;
                        cf = sm.downloadCaptcha(getLocalCaptchaFile());
                        final String code = getCaptchaCode("solvemedia", cf, param);
                        final String chid = sm.getChallenge(code);
                        form.put("adcopy_challenge", chid);
                        form.put("adcopy_response", "manual_challenge");
                    } else {
                        /* Unsupported captchaType */
                        logger.warning("Unsupported captcha type!");
                        return null;
                    }
                    submitForm(form);
                    if (requiresCaptchaWhichCanFail && this.br.containsHTML("The CAPTCHA was incorrect")) {
                        captchaFailed = true;
                    } else {
                        captchaFailed = false;
                    }
                    if (!captchaFailed) {
                        /* Captcha success or we did not have to enter any captcha! */
                        break;
                    }
                }
                if (requiresCaptchaWhichCanFail && captchaFailed) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            }
            boolean skipWait = true;
            if (source_host.equalsIgnoreCase("adbilty.me") || source_host.equalsIgnoreCase("tmearn.com")) {
                /* 2018-07-18: Special case - we have to wait! */
                /** TODO: Fix waittime-detection for tmearn.com */
                skipWait = false;
            }
            /* 2018-07-18: It is important to keep this exact as some websites have "ad-forms" e.g. urlcloud.us */
            Form f2 = br.getFormbyKey("_Token[fields]");
            if (f2 == null) {
                f2 = br.getFormbyKey("_Token%5Bfields%5D");
            }
            if (f2 == null) {
                f2 = br.getFormbyAction("/links/go");
            }
            if (f2 == null) {
                f2 = br.getForm(0);
            }
            if (f2 != null) {
                br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                if (!skipWait) {
                    String waitStr = br.getRegex(">Please Wait (\\d+)s<").getMatch(0);
                    int wait = 10;
                    if (waitStr != null) {
                        wait = Integer.parseInt(waitStr) * +1;
                    }
                    this.sleep(wait * 1000, param);
                }
                submitForm(f2);
            }
            final String finallink = getFinallink();
            if (finallink == null) {
                if (br.containsHTML("<h1>Whoops, looks like something went wrong\\.</h1>")) {
                    logger.warning("Hoster has issue");
                    return decryptedLinks;
                }
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }
        return decryptedLinks;
    }

    private Form getCaptchaForm() {
        return br.getForm(0);
    }

    private boolean evalulateCaptcha() {
        // if ("yes" !== app_vars.enable_captcha) return !0;
        final String hasCaptcha = getAppVarsResult("enable_captcha");
        if ("yes".equals(hasCaptcha)) {
            return true;
        }
        return false;
    }

    private CaptchaType getCaptchaType() {
        final String captchaTypeStr = getAppVarsResult("captcha_type");
        if (StringUtils.isEmpty(captchaTypeStr)) {
            /* No captcha or plugin broken */
            return null;
        }
        if (captchaTypeStr.equalsIgnoreCase("recaptcha") || captchaTypeStr.equalsIgnoreCase("invisible-recaptcha")) {
            /*
             * 2018-07-18: For 'recaptcha', key is in "reCAPTCHA_site_key"; for 'invisible-recaptcha', key is in
             * "invisible_reCAPTCHA_site_key" --> But we always use "reCAPTCHA_site_key"! (tested with urle.co)
             */
            return CaptchaType.reCaptchaV2;
        } else if (captchaTypeStr.equalsIgnoreCase("solvemedia")) {
            return CaptchaType.solvemedia;
        } else {
            return CaptchaType.WTF;
        }
    }

    // private boolean evalulateRecaptchaV2(final Form form) {
    // final String captchaBtn = form.getRegex("<div [^>]*id=\"captchaShortlink\"[^>]*>").getMatch(-1);
    // if (captchaBtn != null) {
    // /*
    // * "recaptcha" === app_vars.captcha_type && ("" === app_vars.user_id && "1" === app_vars.captcha_short_anonymous &&
    // * $("#captchaShort").length && ($("#shorten .btn-captcha").attr("disabled", "disabled"), captchaShort =
    // * grecaptcha.render("captchaShort", {
    // */
    // /*
    // * yes" === app_vars.captcha_shortlink && $("#captchaShortlink").length && ($("#link-view
    // * .btn-captcha").attr("disabled", "disabled"), captchaShortlink = grecaptcha.render("captchaShortlink", {
    // */
    // final String captchaType = getAppVarsResult("captcha_type");
    // final String userId = getAppVarsResult("user_id");
    // if ("recaptcha".equals(captchaType) && "".equals(userId)) {
    // return true;
    // }
    // }
    // // fail over, some seem to be using this
    // if (form.containsHTML("(?:id|class)=(\"|')g-recaptcha\\1")) {
    // return true;
    // } else if (form.containsHTML("invisibleCaptchaShortlink")) {
    // /* 'Invisible' reCaptchaV2 */
    // return true;
    // }
    // return false;
    // }
    //
    // private boolean evalulateSolvemedia(final Form form) {
    // final String solvemediaChallengeKey =
    // br.getRegex("app_vars\\[\\'solvemedia_challenge_key\\'\\]\\s*?=\\s*?\\'([^<>\"\\']+)\\';").getMatch(0);
    // if (form.containsHTML("adcopy_response") && solvemediaChallengeKey != null) {
    // return true;
    // }
    // return false;
    // }
    private String getAppVarsResult(final String input) {
        String result = new Regex(this.appVars, "app_vars\\['" + Pattern.quote(input) + "'\\]\\s*=\\s*'([^']*)'").getMatch(0);
        if (result == null) {
            /* 2018-07-18: json e.g. adbilty.me */
            result = PluginJSonUtils.getJson(this.appVars, input);
        }
        return result;
    }

    private String getFinallink() {
        /* For >90%, this json-attempt should work! */
        String finallink = PluginJSonUtils.getJsonValue(br, "url");
        if (inValidate(finallink) || !finallink.startsWith("http")) {
            finallink = br.getRegex(".+<a href=(\"|')(.*?)\\1[^>]+>\\s*Get\\s+Link\\s*</a>").getMatch(1);
            if (inValidate(finallink)) {
                finallink = br.getRegex(".+<a\\s+[^>]*href=(\"|')(.*?)\\1[^>]*>Continue[^<]*</a>").getMatch(1);
            }
        }
        return finallink;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.MightyScript_AdLinkFly;
    }
}