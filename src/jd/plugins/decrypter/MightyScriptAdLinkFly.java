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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

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

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

/**
 *
 * @author raztoki
 * @tags: similar to OuoIo
 * @examples: Without captcha: met.bz<br />
 *            With reCaptchaV2 (like most): geistlink.com
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class MightyScriptAdLinkFly extends antiDDoSForDecrypt {
    private static final String[]     domains                    = { "arabtvlink.com", "medche.com", "go-urls.tk", "btc4link.com", "topklink.com", "shorts-link.com", "rawabbet.com", "easy4earn.com", "linkat4all.com", "linkdrop.net", "shrtz.me", "ctkings.com", "linksad.net", "paylink.pro", "123link.pro", "donia2link.com", "cutpaid.com", "shortadz.org", "itiurl.co", "shortli.net", "cutearn.ca", "icutit.ca", "cut-one.com", "cll.press", "link-zero.com", "linktor.io", "cash4url.com", "cashat.net", "shortit.ca", "123short.com", "skip-url.me", "msms4.com", "empireshort.com", "loadurl.com", "shortmony.me", "geistlink.com", "cutt.us.com", "arabdollar.com", "shortenow.com", "kingurl.net", "best3link.com", "solo-link.com", "best5link.com", "lkky.co", "win4cut.com", "coinlink.co", "adlink.guru", "short.es", "tmearn.com", "ibly.co", "urle.co", "mitly.us", "zlshorte.net", "igram.im", "gram.im",
        "bit-url.com", "adbilty.me", "linclik.com", "oke.io", "vivads.net", "pnd.tl", "met.bz", "urlcloud.us",
        /** safelinku.com domains */
        "safelinku.com", "idsly.bid", "idsly.net",
        /** clicksfly.com domains */
        "clicksfly.com", "gifsis.com", "blogginggyanbox.com", "buyitonline.store", "clk.ink", "counsellingresult2016.in", "yourtechguider.com", "1921681254.tech",
        /** eglink.info domains */
        "eglink.info", "egyptiangy.net", "egypt-mix.com",
        /** cut-urls.com domains */
        "cut-urls.com", "curs.io", "cuon.io", "cuus.io",
        /** wicr.me domains */
        "wicr.me", "wi.cr",
        /** cutwin.com domains */
        "cutwin.com", "cutwin.us",
        /** adshort.co domains */
        "adshort.co", "adsrt.com", "adsrt.me", "adshort.me", "adshort.im" };
    /** List of services for which waittime is skippable. */
    private static final List<String> domains_waittime_skippable = Arrays.asList(new String[] {});
    // /** List of services for which captcha is skippable or not required. */
    /** TODO: Find a way to automatically detect this edge-case */
    private static final List<String> domains_captcha_skippable  = Arrays.asList(new String[] { "safelinku.com", "idsly.bid", "idsly.net" });

    /**
     * returns the annotation pattern array
     *
     */
    public static String[] getAnnotationUrls() {
        // construct pattern
        final String host = getHostsPattern();
        /* [a-zA-Z0-9]{4,} will work for 99% but site shortit.ca is not compatible. */
        return new String[] { host + "/[a-zA-Z0-9]{2,}" };
    }

    private static String getHostsPattern() {
        final StringBuilder pattern = new StringBuilder();
        for (final String name : domains) {
            pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
        }
        final String hosts = "https?://(?:www\\.)?" + "(?:" + pattern.toString() + ")";
        return hosts;
    }

    @Override
    public String[] siteSupportedNames() {
        return domains;
    }

    /**
     * Returns the annotations names array
     *
     * @return
     */
    public static String[] getAnnotationNames() {
        return new String[] { "cut-urls.com" };
    }

    public enum CaptchaType {
        reCaptchaV2,
        reCaptchaV2_invisible,
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

    private String correctURL(final String input) {
        final String output;
        if (input.contains("linkdrop.net")) {
            /* 2018-12-11: Their https is broken */
            output = input.replace("https://", "http://");
        } else {
            output = input;
        }
        return output;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = correctURL(param.toString());
        final String source_host = Browser.getHost(parameter);
        br.setFollowRedirects(false);
        getPage(parameter);
        String redirect = br.getRedirectLocation();
        if (redirect == null) {
            /* 2019-01-29: E.g. cuon.io */
            redirect = br.getRegex("<meta http\\-equiv=\"refresh\" content=\"\\d+;url=(https?://[^\"]+)\">").getMatch(0);
        }
        if (redirect != null && !redirect.contains(source_host + "/")) {
            /*
             * 2018-07-18: Direct redirect without captcha or any Form e.g. vivads.net OR redirect to other domain of same service e.g.
             * wi.cr --> wicr.me
             */
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
        // form.remove("_Token%5Bunlocked%5D");
        // form.put("_Token%5Bunlocked%5D", "adcopy_challenge%7Cadcopy_response%7Ccoinhive-captcha-token%7Cg-recaptcha-response");
        // final InputField ifield = form.getInputField("_Token%5Bfields%5D");
        // if (ifield != null) {
        // final String value = ifield.getValue();
        // final String valueNew = value.replace("%253Aref", "%3Aref");
        // form.remove("_Token%5Bfields%5D");
        // form.put("_Token%5Bfields%5D", valueNew);
        // }
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
            /* 2018-07-18: Not all sites require a captcha to be solved */
            final CaptchaType captchaType = getCaptchaType();
            if (evalulateCaptcha(captchaType, parameter)) {
                logger.info("Captcha required");
                boolean requiresCaptchaWhichCanFail = false;
                boolean captchaFailed = false;
                for (int i = 0; i <= 2; i++) {
                    form = getCaptchaForm();
                    if (form == null) {
                        return null;
                    }
                    /* Captcha type will usually stay the same even on bad solve attempts! */
                    // captchaType = getCaptchaType();
                    if (captchaType == CaptchaType.reCaptchaV2 || captchaType == CaptchaType.reCaptchaV2_invisible) {
                        requiresCaptchaWhichCanFail = false;
                        final String key;
                        if (captchaType == CaptchaType.reCaptchaV2) {
                            key = getAppVarsResult("reCAPTCHA_site_key");
                        } else {
                            key = getAppVarsResult("invisible_reCAPTCHA_site_key");
                        }
                        if (StringUtils.isEmpty(key)) {
                            logger.warning("Failed to find reCaptchaV2 key");
                            return null;
                        }
                        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, key) {
                            @Override
                            public TYPE getType() {
                                if (captchaType == CaptchaType.reCaptchaV2_invisible) {
                                    return TYPE.INVISIBLE;
                                } else {
                                    return TYPE.NORMAL;
                                }
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
            } else if (form != null) {
                /* 2019-01-30: E.g. "safelinku.com", "idsly.bid", "idsly.net" */
                logger.info("Captcha NOT required but we have a Form to submit");
                this.submitForm(form);
            }
            final boolean skipWait = waittimeIsSkippable(source_host);
            /** TODO: Fix waittime-detection for tmearn.com */
            /* 2018-07-18: It is very important to keep this exact as some websites have "ad-forms" e.g. urlcloud.us !! */
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
                br.getHeaders().put("Origin", "https://" + br.getHost());
                String waitStr = br.getRegex(">Please Wait (\\d+)s<").getMatch(0);
                if (waitStr == null) {
                    /* 2018-12-12: E.g. rawabbet.com */
                    waitStr = br.getRegex("class=\"timer\">\\s*?(\\d+)\\s*?<").getMatch(0);
                }
                if (!skipWait) {
                    int wait = 10;
                    if (waitStr != null) {
                        logger.info("Found waittime in html, waiting (seconds): " + waitStr);
                        wait = Integer.parseInt(waitStr) * +1;
                    } else {
                        logger.info("Failed to find waittime in html, waiting default waittime (seconds): " + wait);
                    }
                    this.sleep(wait * 1000, param);
                } else if (waitStr != null) {
                    logger.info("Skipping waittime (seconds): " + waitStr);
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

    private boolean waittimeIsSkippable(final String source_host) {
        if (StringUtils.isEmpty(source_host)) {
            /* WTF */
            return false;
        }
        if (domains_waittime_skippable.contains(source_host)) {
            /* Waittime is skippable for this host */
            return true;
        }
        /* Waittime is NOT skippable for all other hosts */
        return false;
    }

    private Form getCaptchaForm() {
        return br.getForm(0);
    }

    /**
     * true = captcha required, false = no captcha required <br />
     *
     * @param captchatype
     *            : Type of the captcha found in js/html - required in some rare cases.
     */
    private boolean evalulateCaptcha(final CaptchaType captchatype, final String url_source) {
        // if ("yes" !== app_vars.enable_captcha) return !0;
        final String source_host = Browser.getHost(url_source);
        boolean hasCaptcha;
        String captchaIndicatorValue = getAppVarsResult("enable_captcha");
        if (captchaIndicatorValue != null) {
            /* Most website will contain this boolean-like value telling us whether we need to solve a captcha or not. */
            if ("yes".equals(captchaIndicatorValue)) {
                hasCaptcha = true;
            } else {
                hasCaptcha = false;
            }
        } else {
            /*
             * In some cases, we have to check for the type of the captcha to find out whether there is a captcha or not (unsafe method,
             * only needed in rare cases, see example websites in header of this class!)
             */
            if (captchatype != null) {
                hasCaptcha = true;
            } else {
                hasCaptcha = false;
            }
        }
        if (hasCaptcha && domains_captcha_skippable.contains(source_host)) {
            logger.info("Captcha should be required but current host does not require captcha");
            hasCaptcha = false;
        }
        return hasCaptcha;
    }

    private CaptchaType getCaptchaType() {
        final String captchaTypeStr = getAppVarsResult("captcha_type");
        if (StringUtils.isEmpty(captchaTypeStr)) {
            /* 2018-12-11: Special case e.g. linkdrop.net */
            final String reCaptchaV2Key = getAppVarsResult("reCAPTCHA_site_key");
            if (reCaptchaV2Key != null) {
                return CaptchaType.reCaptchaV2;
            }
            /* No captcha or plugin broken */
            return null;
        }
        if (captchaTypeStr.equalsIgnoreCase("recaptcha")) {
            /*
             * 2018-07-18: For 'recaptcha', key is in "reCAPTCHA_site_key"; for 'invisible-recaptcha', key is in
             * "invisible_reCAPTCHA_site_key" --> We can usually use "reCAPTCHA_site_key" as well (tested with urle.co) ... but it is better
             * to use the correct one instead - especially because sometimes only that one is available (e.g. kingurl.net).
             */
            return CaptchaType.reCaptchaV2;
        } else if (captchaTypeStr.equalsIgnoreCase("invisible-recaptcha")) {
            return CaptchaType.reCaptchaV2_invisible;
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