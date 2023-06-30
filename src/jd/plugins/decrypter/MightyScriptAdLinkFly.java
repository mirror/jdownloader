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

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.jdownloader.captcha.v2.challenge.hcaptcha.CaptchaHelperCrawlerPluginHCaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
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
 * @examples: Without captcha: met.bz <br />
 *            With reCaptchaV2 (like most): sh2rt.com <br />
 *            With solvemedia: clik.pw
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public abstract class MightyScriptAdLinkFly extends antiDDoSForDecrypt {
    public enum CaptchaType {
        hCaptcha,
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

    /** Use this to correct URL added by user if necessary. */
    protected void correctURL(final CryptedLink param) {
        final ArrayList<String> deadDomains = this.getDeadDomains();
        if (deadDomains != null) {
            /* Change domain in added URL if we know that the domain inside added URL is dead. */
            final String domain = Browser.getHost(param.getCryptedUrl(), true);
            if (deadDomains.contains(domain)) {
                param.setCryptedUrl(param.getCryptedUrl().replaceFirst(org.appwork.utils.Regex.escape(domain) + "/", this.getHost() + "/"));
            }
        }
    }

    /**
     * Override this and add dead domains so upper handling can auto update added URLs and change domain if it contains a dead domain. This
     * way a lot of "old" URLs will continue to work in JD while they may fail in browser.
     */
    protected ArrayList<String> getDeadDomains() {
        return null;
    }

    protected boolean supportsHost(final String host) {
        return host.equalsIgnoreCase(this.getHost());
    }

    /** Returns true if current browsers state looks like it fits this script. */
    protected boolean looksLikeSupportedScript(final Browser br) {
        if (regexAppVars(this.br) != null) {
            return true;
        } else if (getBeforeCaptchaForm(br) != null) {
            return true;
        } else if (getLinksGoForm(null, br) != null) {
            return true;
        } else {
            return false;
        }
    }

    protected String regexAppVars(final Browser br) {
        return br.getRegex("var (app_vars.*?)</script>").getMatch(0);
    }

    protected Form getBeforeCaptchaForm(final Browser br) {
        return br.getFormbyProperty("id", "before-captcha");
    }

    protected Form getContinueForm(CryptedLink param, Form form, final Browser br) {
        /* 2018-07-18: It is very important to keep this exact as some websites have "ad-forms" e.g. urlcloud.us !! */
        Form f2 = br.getFormbyKey("_Token[fields]");
        if (f2 == null) {
            f2 = br.getFormbyKey("_Token%5Bfields%5D");
        }
        if (f2 == null) {
            f2 = getLinksGoForm(param, br);
        }
        if (f2 == null) {
            f2 = br.getForm(0);
        }
        return f2;
    }

    protected Form getLinksGoForm(final CryptedLink param, final Browser br) {
        return br.getFormbyAction("/links/go");
    }

    protected String handleRecaptcha(CaptchaType captchaType, Browser br, Form form) throws PluginException, InterruptedException, DecrypterException, MalformedURLException {
        if (captchaType == CaptchaType.reCaptchaV2 || captchaType == CaptchaType.reCaptchaV2_invisible) {
            final String key;
            if (captchaType == CaptchaType.reCaptchaV2) {
                key = getAppVarsResult("reCAPTCHA_site_key");
            } else {
                key = getAppVarsResult("invisible_reCAPTCHA_site_key");
            }
            /**
             * Some websites do not allow users to access the target URL directly but will require a certain Referer to be set. </br>
             * We pre-set this in our browser but if that same URL is opened in browser, it may redirect to another website as the Referer
             * is missing. In this case we'll use the main page to solve the captcha to prevent this from happening.
             */
            final String reCaptchaSiteURL;
            if (this.getSpecialReferer() != null) {
                /* Required e.g. for sh2rt.com. */
                reCaptchaSiteURL = br.getBaseURL();
            } else {
                /* Fine for most of all websites. */
                reCaptchaSiteURL = br.getURL();
            }
            final boolean isInvisible = captchaType == CaptchaType.reCaptchaV2_invisible;
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, key) {
                @Override
                public TYPE getType() {
                    if (isInvisible) {
                        return TYPE.INVISIBLE;
                    } else {
                        return TYPE.NORMAL;
                    }
                }

                @Override
                protected String getSiteUrl() {
                    return reCaptchaSiteURL;
                }
            }.getToken();
            form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            return recaptchaV2Response;
        } else {
            return null;
        }
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String sourceHost = Browser.getHost(param.getCryptedUrl());
        correctURL(param);
        ret.addAll(handlePreCrawlProcess(param));
        if (!ret.isEmpty()) {
            /* E.g. direct redirect */
            return ret;
        } else if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.toString().length() < 100) {
            /* 2020-05-29: E.g. https://uii.io/full */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.setFollowRedirects(true);
        /* 2022-07-29: E.g. exe.io */
        final Form beforeCaptcha = getBeforeCaptchaForm(br);
        if (beforeCaptcha != null) {
            /* 2019-10-30: E.g. exe.io */
            logger.info("Found pre-captcha Form");
            this.submitForm(beforeCaptcha);
        }
        handleLandingRedirect(param, br);
        appVars = regexAppVars(this.br);
        String recaptchaV2Response = null;
        /* 2018-07-18: Not all sites require a captcha to be solved */
        CaptchaType captchaType = getCaptchaType(null);
        Form lastSubmitForm = null;
        if (evalulateCaptcha(captchaType, param.getCryptedUrl())) {
            logger.info("Looks like captcha might be required");
            Boolean requiresCaptchaWhichCanFail = null;
            Boolean captchaFailed = null;
            captchaLoop: for (int i = 0; i <= 2; i++) {
                final Form form = getCaptchaForm(br, param);
                if (form == null) {
                    if (getLinksGoForm(param, br) != null) {
                        if (i == 0) {
                            logger.info("Captcha was not required");
                        }
                        captchaFailed = false;
                        break;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                captchaType = getCaptchaType(form);
                final InputField action = form.getInputField("action");
                if (action == null || !"continue".equals(action.getValue()) || "captcha".equals(action.getValue())) {
                    /* Captcha type will usually stay the same even on bad solve attempts! */
                    // captchaType = getCaptchaType();
                    if (captchaType == CaptchaType.hCaptcha) {
                        requiresCaptchaWhichCanFail = false;
                        final String key = getAppVarsResult("hcaptcha_checkbox_site_key");
                        if (StringUtils.isEmpty(key)) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find reCaptchaV2 key");
                        }
                        final String siteURL;
                        if (this.getSpecialReferer() != null) {
                            /* Required e.g. for sh2rt.com. */
                            siteURL = br.getBaseURL();
                        } else {
                            /* Fine for most of all websites. */
                            siteURL = br.getURL();
                        }
                        final String hCaptchaResponse = new CaptchaHelperCrawlerPluginHCaptcha(this, br, key) {
                            @Override
                            protected String getSiteUrl() {
                                return siteURL;
                            }
                        }.getToken();
                        form.put("g-recaptcha-response", Encoding.urlEncode(hCaptchaResponse));
                        form.put("h-captcha-response", Encoding.urlEncode(hCaptchaResponse));
                    } else if (captchaType == CaptchaType.reCaptchaV2 || captchaType == CaptchaType.reCaptchaV2_invisible) {
                        requiresCaptchaWhichCanFail = false;
                        handleRecaptcha(captchaType, br, form);
                    } else if (captchaType == CaptchaType.solvemedia) {
                        final String solvemediaChallengeKey = this.getAppVarsResult("solvemedia_challenge_key");
                        if (StringUtils.isEmpty(solvemediaChallengeKey)) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find solvemedia_challenge_key");
                        }
                        requiresCaptchaWhichCanFail = true;
                        final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                        sm.setChallengeKey(solvemediaChallengeKey);
                        final String code = getCaptchaCode("solvemedia", sm.downloadCaptcha(getLocalCaptchaFile()), param);
                        final String chid = sm.getChallenge(code);
                        form.put("adcopy_challenge", chid);
                        form.put("adcopy_response", "manual_challenge");
                    } else if (captchaType != null) {
                        /* This should never happen */
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unsupported captcha type:" + captchaType);
                    } else {
                        // * captchas(site) -> ads page -> no captcha(site)
                        logger.info("no captcha?!");
                    }
                }
                lastSubmitForm = form;
                submitForm(form);
                // refresh appVars!
                appVars = regexAppVars(this.br);
                captchaType = getCaptchaType(null);
                if (getLinksGoForm(param, br) != null) {
                    /* Fail-safe */
                    logger.info("Quit captcha loop because: Final Form is present in html");
                    captchaFailed = false;
                    break;
                } else if (captchaType == null) {
                    logger.info("Quit captcha loop because: No captcha needed");
                    captchaFailed = false;
                    break;
                } else if (Boolean.FALSE.equals(requiresCaptchaWhichCanFail)) {
                    logger.info("Quit captcha loop because: Required captcha only needs one attempt");
                    break;
                } else if (this.br.containsHTML("(?i)The CAPTCHA was incorrect")) {
                    captchaFailed = true;
                    continue;
                } else {
                    // another captcha round? iir.ai
                    continue;
                }
            }
            if (Boolean.TRUE.equals(requiresCaptchaWhichCanFail) && Boolean.TRUE.equals(captchaFailed)) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
        hookAfterCaptcha(this.br, lastSubmitForm);
        final boolean skipWait = waittimeIsSkippable(sourceHost);
        /** TODO: Fix waittime-detection for tmearn.com */
        Form f2 = getContinueForm(param, lastSubmitForm, br);
        if (f2 != null) {
            br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            br.getHeaders().put("Origin", "https://" + br.getHost());
            if (br.containsHTML(org.appwork.utils.Regex.escape("name:'gc_token', value:e}).appendTo('#go-link')")) && recaptchaV2Response != null) {
                /* 2021-08-06: Special: tny.so (and maybe others too) */
                f2.put("gc_token", Encoding.urlEncode(recaptchaV2Response));
            }
            if (f2.hasInputFieldByName("_csrfToken")) {
                /*
                 * 2021-03-29: E.g. tny.so will return error if this header is missing/wrong:
                 * {"message":"CSRF token mismatch.","url":"\/links\/go","code":403}
                 */
                final String csrftoken = f2.getInputField("_csrfToken").getValue();
                if (!StringUtils.isEmpty(csrftoken)) {
                    br.getHeaders().put("X-CSRF-Token", csrftoken);
                } else {
                    logger.warning("csrftoken fielld exists but no value present");
                }
            }
            String waitSecondsStr = br.getRegex("\"counter_value\"\\s*:\\s*\"?(\\d+)").getMatch(0);
            if (waitSecondsStr == null) {
                waitSecondsStr = br.getRegex("(?i)>\\s*Please Wait\\s*(\\d+)s\\s*<").getMatch(0);
                if (waitSecondsStr == null) {
                    /* 2018-12-12: E.g. rawabbet.com[RIP 2019-02-21] */
                    waitSecondsStr = br.getRegex("class=\"timer\">\\s*?(\\d+)\\s*?<").getMatch(0);
                }
                if (waitSecondsStr == null) {
                    /* 2021-01-12: E.g. bestcash2020.com */
                    waitSecondsStr = br.getRegex("id=\"timer\"[^>]+>\\s*?(\\d+)\\s*?<").getMatch(0);
                }
            }
            if (!skipWait) {
                if (waitSecondsStr != null) {
                    final int waitSeconds = Integer.parseInt(waitSecondsStr) + 1;
                    logger.info("Found waittime seconds in html: " + waitSecondsStr + " | Actually waiting seconds: " + waitSeconds);
                    this.sleep(waitSeconds * 1000, param);
                } else {
                    logger.info("Failed to find waittime in html");
                }
            } else {
                if (waitSecondsStr != null) {
                    logger.info("Skipping waittime (seconds): " + waitSecondsStr);
                } else {
                    logger.info("Skipping waittime");
                }
            }
            submitForm(f2);
        }
        String finallink = null;
        if (StringUtils.containsIgnoreCase(br.getRequest().getResponseHeader("Content-Type"), "application/json")) {
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
            finallink = (String) entries.get("url");
            logger.info("finallink from json: " + finallink);
            if (finallink != null && finallink.startsWith("/")) {
                logger.warning("finallink looks to be a relative URL --> Looks like something went wrong");
            }
        } else {
            finallink = br.getRegex("<a href=(\"|')(.*?)\\1[^>]+>\\s*Get\\s*Link\\s*</a>").getMatch(1);
            if (StringUtils.isEmpty(finallink)) {
                finallink = br.getRegex("<a[^>]*href=(\"|')(.*?)\\1[^>]*>Continue[^<]*</a>").getMatch(1);
            }
            logger.info("finallink from html: " + finallink);
        }
        /* 2020-02-03: clk.in: p.clk.in/?n=bla */
        if (!StringUtils.isEmpty(finallink) && finallink.matches("https?://p\\.[^/]+/\\?n=.+")) {
            /* TODO: 2022-06-23: This should not be needed anymore */
            logger.info("Special case: Finallink seems to lead to another step: " + finallink);
            this.getPage(finallink);
            /* 2020-110-6: E.g. clicksfly.com */
            finallink = br.getRegex("<div class=\"button\">\\s*<center>\\s*<a name=\"a\"[^>]*href=\"(http[^\"]+)\">").getMatch(0);
        }
        if (finallink == null) {
            if (br.containsHTML("(?i)<h1>Whoops, looks like something went wrong\\.</h1>")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        ret.add(createDownloadlink(finallink));
        return ret;
    }

    protected void handleLandingRedirect(final CryptedLink param, Browser br) throws Exception {
        final Form landingRedirect = br.getFormByRegex("id\\s*=\\s*\"landing\"");
        if (landingRedirect != null) {
            final String referer = br.getRequest().getHeaders().get("Referer");
            final InputField goField = landingRedirect.getInputField("go");
            if (goField != null) {
                String goValue = goField.getValue();
                goValue = Encoding.Base64Decode(goValue != null ? URLEncode.decodeURIComponent(goValue) : null);
                if (goValue == null || !canHandle(goValue)) {
                    goValue = referer;
                }
                if (goValue != null) {
                    final String landing = landingRedirect.getAction();
                    logger.info("fake landing:" + landing + " -> " + goValue);
                    br.setCurrentURL(landing);
                    getPage(goValue);
                }
            }
        }
    }

    /**
     * Override to do something after the captcha (also gets called when no captcha was needed).
     *
     * @param form
     *            TODO
     */
    protected void hookAfterCaptcha(final Browser br, Form form) throws Exception {
    }

    /** Accesses input URL and handles "Pre-AdLinkFly" redirects. */
    protected ArrayList<DownloadLink> handlePreCrawlProcess(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (getSpecialReferer() != null) {
            br.getHeaders().put("Referer", getSpecialReferer());
        }
        br.setFollowRedirects(false);
        getPage(param.getCryptedUrl());
        // 2019-11-13: http->https->different domain(https)
        // 2019-11-13: http->https->different domain(http)->different domain(https)
        String firstRedirect = null;
        int count = 1;
        while (true) {
            if (isAbort()) {
                throw new InterruptedException();
            }
            String redirect = br.getRedirectLocation();
            if (redirect == null) {
                /* 2019-01-29: E.g. cuon.io */
                redirect = br.getRegex("<meta http\\-equiv=\"refresh\" content=\"\\d+;url=(https?://[^\"]+)\">").getMatch(0);
            }
            if (redirect == null) {
                /* 2019-04-25: E.g. clkfly.pw */
                redirect = br.getHttpConnection().getHeaderField("Refresh");
                if (redirect != null && redirect.matches("^\\d+, http.+")) {
                    redirect = new Regex(redirect, "^\\d+, (http.+)").getMatch(0);
                }
            }
            if (redirect == null) {
                /* 2021-01-14 exe.io */
                /* document.getElementById('clickable').click() */
                redirect = br.containsHTML("\\('clickable'\\)\\.click\\(\\)") ? br.getRegex("<a\\s*href\\s*=\\s*\"(https?://[^\"]+)\"\\s*id\\s*=\\s*\"clickable\"").getMatch(0) : null;
            }
            if (redirect == null) {
                break;
            }
            getPage(redirect);
            if (!this.looksLikeSupportedScript(br)) {
                firstRedirect = redirect;
                break;
            }
            count++;
            if (count > 10) {
                throw new DecrypterException("Too many redirects!");
            }
        }
        if (firstRedirect != null) {
            /**
             * Check if this is redirect redirect or if it really is the one we expect. </br>
             * Some websites redirect e.g. to a fake blog and only redirect back to the usual handling if you re-access the main URL with
             * that fake blog as referer header e.g.: adshort.co, ez4short.com </br>
             * In some cases this special referer is pre-given via getSpecialReferer in which we do not have to re-check.
             */
            if (getSpecialReferer() != null) {
                /* Assume that redirect redirects to external website and use it as our final result. */
                ret.add(this.createDownloadlink(firstRedirect));
            } else {
                logger.info("Checking if redirect is external redirect or redirect to fake blog");
                br.getHeaders().put("Referer", firstRedirect);
                br.setFollowRedirects(false);
                getPage(param.getCryptedUrl());
                final String secondRedirect = br.getRedirectLocation();
                if (secondRedirect != null) {
                    if (!StringUtils.equalsIgnoreCase(firstRedirect, secondRedirect)) {
                        logger.warning("Got different redirect on 2nd attempt: First: " + firstRedirect + " | Second: " + secondRedirect);
                        ret.add(this.createDownloadlink(firstRedirect));
                        ret.add(this.createDownloadlink(secondRedirect));
                    } else {
                        logger.info("Same redirect happens even with Referer --> Returning final result: " + secondRedirect);
                        ret.add(this.createDownloadlink(firstRedirect));
                    }
                } else if (regexAppVars(this.br) == null) {
                    logger.warning("Result looks like plugin failure");
                } else {
                    logger.info("Referer auto handling successful | Correct referer: " + firstRedirect);
                }
            }
        }
        return ret;
    }

    protected boolean waittimeIsSkippable(final String source_host) {
        return false;
    }

    protected boolean captchaIsSkippable(final String source_host) {
        return false;
    }

    @Override
    protected void submitForm(Browser ibr, Form form) throws Exception {
        super.submitForm(ibr, form);
        // TODO: add support for special wordpress pages, exe.io
        // final Form landing = getLandingForm(ibr);
        // if (landing != null) {
        // submitForm(landing);
        // }
    }

    protected Form getLandingForm(final Browser br) {
        final Form[] forms = br.getForms();
        for (Form form : forms) {
            if (form.containsHTML("id\\s*=\\s*\"landing\"") && MethodType.POST.equals(form.getMethod())) {
                return form;
            }
        }
        return null;
    }

    protected Form getCaptchaForm(final Browser br, CryptedLink link) {
        final Form goLinksForm = getLinksGoForm(link, br);
        if (goLinksForm != null) {
            return null;
        }
        final Form[] forms = br.getForms();
        for (Form form : forms) {
            if (MethodType.POST.equals(form.getMethod())) {
                return form;
            }
        }
        if (forms.length > 0) {
            final Form maybe = forms[0];
            if (maybe.getInputFields().size() < 2) {
                return null;
            } else {
                return maybe;
            }
        } else {
            return null;
        }
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
        // String captchaIndicatorValue = getAppVarsResult("enable_captcha");
        final String captchaIndicator = getAppVarsResult("captcha_shortlink");
        Boolean hasCaptcha = null;
        if (captchaIndicator != null) {
            /* Most website will contain this boolean-like value telling us whether we need to solve a captcha or not. */
            logger.info("Found captchaIndicator:" + captchaIndicator);
            if ("yes".equals(captchaIndicator)) {
                logger.info("Positive captchaIndicator --> Captcha required(?):type=" + captchatype);
            } else {
                logger.info("Negative captchaIndicator --> No captcha required(?):type=" + captchatype);
                hasCaptcha = false;
            }
        } else {
            /*
             * In some cases, we have to check for the type of the captcha to find out whether there is a captcha or not (unsafe method,
             * only needed in rare cases, see example websites in header of this class!)
             */
            logger.info("No captchaIndicator --> Captcha required(?):type=" + captchatype);
        }
        hasCaptcha = hasCaptcha != null ? hasCaptcha : (captchatype != null);
        if (hasCaptcha && this.captchaIsSkippable(source_host)) {
            logger.info("Captcha should be required but current host does not require captcha");
            hasCaptcha = false;
        }
        return hasCaptcha;
    }

    protected CaptchaType getCaptchaType(Form form) {
        String captchaTypeStr = getAppVarsResult("captcha_type");
        if (form != null && ("recaptcha".equalsIgnoreCase(captchaTypeStr) || "invisible-recaptcha".equalsIgnoreCase(captchaTypeStr))) {
            /* First compare expected recaptcha type/key against key in our Form. */
            final String formRecaptchaSiteKey = form.getRegex("data-sitekey\\s*=\\s*\"(" + AbstractRecaptchaV2.apiKeyRegex + ")\"").getMatch(0);
            final String reCAPTCHA_site_key = getAppVarsResult("reCAPTCHA_site_key");
            final String invisible_reCAPTCHA_site_key = getAppVarsResult("invisible_reCAPTCHA_site_key");
            if (StringUtils.equals(formRecaptchaSiteKey, reCAPTCHA_site_key)) {
                captchaTypeStr = "recaptcha";
            } else if (StringUtils.equals(formRecaptchaSiteKey, invisible_reCAPTCHA_site_key)) {
                captchaTypeStr = "invisible-recaptcha";
            }
        }
        if (StringUtils.isEmpty(captchaTypeStr)) {
            /* No captcha or plugin broken */
            return null;
        }
        /* No Form there or no reCaptchaKey in existent Form or different captcha than reCaptcha -> Decide based on captchaTypeStr. */
        if (captchaTypeStr.equalsIgnoreCase("hcaptcha_checkbox")) {
            if (getAppVarsResult("hcaptcha_checkbox_site_key") != null) {
                return CaptchaType.hCaptcha;
            } else {
                return null;
            }
        } else if (captchaTypeStr.equalsIgnoreCase("recaptcha")) {
            /*
             * 2018-07-18: For 'recaptcha', key is in "reCAPTCHA_site_key"; for 'invisible-recaptcha', key is in
             * "invisible_reCAPTCHA_site_key" --> We can usually use "reCAPTCHA_site_key" as well (tested with urle.co) ... but it is better
             * to use the correct one instead - especially because sometimes only that one is available (e.g. kingurl.net).
             */
            final String reCAPTCHA_site_key = getAppVarsResult("reCAPTCHA_site_key");
            if (reCAPTCHA_site_key != null) {
                return CaptchaType.reCaptchaV2;
            } else {
                return null;
            }
        } else if (captchaTypeStr.equalsIgnoreCase("invisible-recaptcha")) {
            final String invisible_reCAPTCHA_site_key = getAppVarsResult("invisible_reCAPTCHA_site_key");
            if (invisible_reCAPTCHA_site_key != null) {
                return CaptchaType.reCaptchaV2_invisible;
            } else {
                return null;
            }
        } else if (captchaTypeStr.equalsIgnoreCase("solvemedia")) {
            if (getAppVarsResult("solvemedia_challenge_key") != null) {
                return CaptchaType.solvemedia;
            } else {
                return null;
            }
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
    protected String getAppVarsResult(final String input) {
        String result = new Regex(this.appVars, "app_vars\\['" + Pattern.quote(input) + "'\\]\\s*=\\s*'([^']*)'").getMatch(0);
        if (result == null) {
            /* 2018-07-18: json e.g. adbilty.me */
            result = PluginJSonUtils.getJson(this.appVars, input);
        }
        if (StringUtils.isEmpty(result)) {
            return null;
        } else {
            return result;
        }
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        /* Most of all providers will require the user to solve a reCaptchaV2. */
        return true;
    }

    /** Use this to define a Referer to be used for the first request. */
    protected String getSpecialReferer() {
        return null;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.MightyScript_AdLinkFly;
    }
}