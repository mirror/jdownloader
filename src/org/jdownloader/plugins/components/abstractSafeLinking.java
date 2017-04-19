package org.jdownloader.plugins.components;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

/**
 * abstract class to handle sites similar to safelinking type sites. <br />
 * Google "Secure your links with a captcha, a password and much more" to find such sites
 *
 * @author raztoki - new json implementation.
 * @author raztoki - abstract & improvements
 * @author bismarck - parts of the original
 * @author psp - parts of the original
 */
public abstract class abstractSafeLinking extends antiDDoSForDecrypt {

    public abstractSafeLinking(PluginWrapper wrapper) {
        super(wrapper);
    }

    protected String parameter = null;
    protected String cType     = "notDetected";
    protected String uid       = null;

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            /* define custom browser headers and language settings */
            prepBr.setAllowedResponseCodes(new int[] { 422, 500 });
            prepBr.setReadTimeout(2 * 60 * 1000);
            prepBr.setConnectTimeout(2 * 60 * 1000);
        }
        return prepBr;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // link correction
        parameter = correctLink(param.toString());
        // setuid
        uid = getUID(parameter);
        // shortlink i assume
        if (parameter.matches(regexLinkShort())) {
            // currently https does not work with short links!
            getPage(parameter.replace("https://", "http://"));
            String newparameter = br.getRedirectLocation();
            if (newparameter == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* redirect goes to outside link or back own shortlink */
            if (!newparameter.matches(".+/(?:p|d)/[a-f0-9]{10}")) {
                decryptedLinks.add(createDownloadlink(newparameter));
                return decryptedLinks;
            }
            parameter = newparameter;
        }
        getPage(parameter);
        // /d/ should redirect, but now apparently it stays on the short url link..
        if (parameter.matches(".*?/d/([a-f0-9]{10}|" + regexBase58() + ")")) {
            final String link = br.getRedirectLocation();
            if (link != null) {
                decryptedLinks.add(createDownloadlink(link));
            }
        } else {
            // now comes the json
            ajaxPostPageRaw("/v1/protected", PluginJSonUtils.ammendJson(null, "hash", uid));
            final String message = PluginJSonUtils.getJsonValue(ajax, "message");
            if (StringUtils.containsIgnoreCase(message, "not found") || StringUtils.containsIgnoreCase(message, "not found")) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            // we could also here check status?? maybe trust them that files are offline?? this way no captchas needed to find the links and
            // confirm ourselves.
            final String security = PluginJSonUtils.getJsonNested(ajax, "security");
            final String useCaptcha = PluginJSonUtils.getJsonValue(security, "useCaptcha");
            final String captchaType = PluginJSonUtils.getJsonValue(security, "captchaType");
            final String usePassword = PluginJSonUtils.getJsonValue(security, "usePassword");
            if (!inValidate(security) && (PluginJSonUtils.parseBoolean(useCaptcha) || PluginJSonUtils.parseBoolean(usePassword))) {
                int tries = 4;
                for (int i = 0; i < tries; i++) {
                    String nextPost = null;
                    if (PluginJSonUtils.parseBoolean(usePassword)) {
                        // we need to get the password.
                        String psw = null;
                        if (i <= 1) {
                            // only use this twice, once for auto solvemedia, and second time with manual captcha!
                            psw = param.getDecrypterPassword();
                        }
                        if (psw == null || "".equals(psw)) {
                            psw = getUserInput(parameter, param);
                            if (psw == null || "".equals(psw)) {
                                throw new DecrypterException(DecrypterException.PASSWORD);
                            }
                        }
                        nextPost = PluginJSonUtils.ammendJson(nextPost, "password", psw);
                        if (!PluginJSonUtils.parseBoolean(useCaptcha)) {
                            nextPost = PluginJSonUtils.ammendJson(nextPost, "hash", uid);
                            ajaxPostPageRaw("/v1/captcha", nextPost);
                            if (ajax.getHttpConnection().getResponseCode() == 200) {
                                break;
                            } else if (ajax.getHttpConnection().getResponseCode() == 422) {
                                if ("true".equalsIgnoreCase(PluginJSonUtils.getJsonValue(ajax, "passwordFail"))) {
                                    if (i + 1 > tries) {
                                        throw new DecrypterException(DecrypterException.PASSWORD);
                                    }
                                    continue;
                                } else {
                                    // unknown error
                                    throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
                                }
                            }
                        }
                    }
                    if (PluginJSonUtils.parseBoolean(useCaptcha)) {
                        // captchas:[
                        // {name:"SolveMedia",id:0,isDefault:!0,enabled:!0,publicKey:solvemediaPublicKey,init:function(e){!function
                        // t(){window.ACPuzzle?window.ACPuzzle.create(e.publicKey,"captcha",{lang:"en",size:"standard"}):setTimeout(t,500)}()},getModel:function(){return{answer:window.ACPuzzle.get_response(),challengeId:window.ACPuzzle.get_challenge()}},refresh:function(){window.ACPuzzle.reload()}},
                        // {name:"Recaptcha",id:1,isDefaultBackup:!0,enabled:!0,publicKey:"6Lf5bAITAAAAABDTzSsLdgMDY1jeK6qE6IKGxvqk",init:function(e){window.renderRecaptchaCB=function(){grecaptcha.render(document.getElementById("recaptcha"),{sitekey:e.publicKey})},$.getScript("https://www.google.com/recaptcha/api.js?onload=renderRecaptchaCB&render=explicit",function(){})},getModel:function(e){return{answer:grecaptcha.getResponse(),challengeId:e.captcha2.publicKey}},refresh:function(){grecaptcha.reset()}},
                        // {name:"Basic captcha",id:2},
                        // {name:"3D captcha",id:3},
                        // {name:"Fancy
                        // captcha",id:4,enabled:!0,init:function(){$.getScript("/assets/components/plugins/fancy_captcha/jquery.captcha.js",function(){$("#fancy").fancy_captcha({captchaDir:"/",url:baseUrl+"/fancy_captcha",imagesDir:"/assets/images/fancycaptcha"})})},getModel:function(e){return
                        // window.fancyCaptcha?{answer:window.fancyCaptcha.answer}:null},refresh:function(){}},
                        // {name:"QapTcha",id:5,enabled:!0,init:function(e){e.enableQaptcha=!0},getModel:function(e){return{answer:e.security.qaptcha?e.security.captcha2.key:"",challengeId:e.security.captcha2.key}}},
                        // {name:"Simple Captcha",id:6},
                        // {name:"Dotty Captcha",id:7},
                        // {name:"Cool Captcha",id:8},
                        // {name:"Standard Captcha",id:9},
                        // {name:"Cats Captcha",id:10},
                        // {name:"Circle Captcha",id:11}]
                        // };
                        final Browser captchaBr = br.cloneBrowser();
                        // captcha handling yay!
                        // at this given time its always solvemedia by default, recaptcha seems to be secondary default for the
                        // refresh/toggle
                        switch (0) { // Integer.parseInt(captchaType)) {
                        case 0: {

                            org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                            sm.setChallengeKey(br.getHost().equalsIgnoreCase("safelinking.net") ? "OZ987i6xTzNs9lw5.MA-2Vxbc-UxFrLu" : "t62EJ1oSPvEIEl.tnmC0la5sdfLHDPsl");
                            File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                            String code = getCaptchaCode(cf, param);
                            String chid = sm.getChallenge(code);
                            nextPost = PluginJSonUtils.ammendJson(nextPost, "answer", code);
                            nextPost = PluginJSonUtils.ammendJson(nextPost, "challengeId", chid);
                            nextPost = PluginJSonUtils.ammendJson(nextPost, "type", 0);
                            break;
                        }
                        case 1: {
                            // recaptcha2 !!!
                            final String code = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, "6Lf5bAITAAAAABDTzSsLdgMDY1jeK6qE6IKGxvqk").getToken();
                            /* Sometimes that field already exists containing the value "manuel_challenge" */
                            nextPost = PluginJSonUtils.ammendJson(nextPost, "answer", code);
                            nextPost = PluginJSonUtils.ammendJson(nextPost, "challengeId", "6Lf5bAITAAAAABDTzSsLdgMDY1jeK6qE6IKGxvqk");
                            nextPost = PluginJSonUtils.ammendJson(nextPost, "type", 1);
                            break;
                        }
                        case 2: {
                            final String url = "";
                            final String code = getCaptchaCode(url, param);
                            nextPost = PluginJSonUtils.ammendJson(nextPost, "securimage_response_field", code);
                            nextPost = PluginJSonUtils.ammendJson(nextPost, "type", 2);
                            break;
                        }
                        case 3: {
                            final String url = "";
                            final String code = getCaptchaCode(url, param);
                            nextPost = PluginJSonUtils.ammendJson(nextPost, "3dcaptcha_response_field", code);
                            nextPost = PluginJSonUtils.ammendJson(nextPost, "type", 3);
                            break;
                        }
                        case 4: {
                            getPage(captchaBr, "/includes/captcha_factory/fancycaptcha.php?hash=" + uid);
                            nextPost = PluginJSonUtils.ammendJson(nextPost, "fancy-captcha", captchaBr.toString().trim());
                            nextPost = PluginJSonUtils.ammendJson(nextPost, "type", 4);
                            break;
                        }
                        case 5: {
                            captchaBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                            postPage(captchaBr, "/includes/captcha_factory/Qaptcha.jquery.php?hash=" + uid, "action=qaptcha");
                            if (!captchaBr.containsHTML("\"error\":false")) {
                                logger.warning("Decrypter broken for link: " + parameter + "\n");

                            }
                            nextPost = PluginJSonUtils.ammendJson(nextPost, "iQapTcha", "");
                            nextPost = PluginJSonUtils.ammendJson(nextPost, "type", 5);
                            break;
                        }
                        case 7:
                        case 8:
                        case 9:
                        case 10:
                        case 11: {
                            // unsupported types
                            // short wait to prevent hammering
                            sleep(2500, param);
                            // maybe also good to clear cookies?
                            getPage(br.getURL());
                            continue;
                        }
                        case 12: {
                            final String result = getCaptchaCode("/simplecaptcha/captcha.php", param);
                            nextPost = PluginJSonUtils.ammendJson(nextPost, "captchatype", "Simple");
                            nextPost = PluginJSonUtils.ammendJson(nextPost, "norobot", result);
                            nextPost = PluginJSonUtils.ammendJson(nextPost, "type", 12);
                            break;
                        }
                        }
                        nextPost = PluginJSonUtils.ammendJson(nextPost, "hash", uid);
                        ajaxPostPageRaw("/v1/captcha", nextPost);
                        if (ajax.getHttpConnection().getResponseCode() == 200) {
                            // this seems good. be aware that the security string is still presence in the successful task
                            break;
                        } else if (ajax.getHttpConnection().getResponseCode() == 422) {
                            if (PluginJSonUtils.parseBoolean(usePassword) && "true".equalsIgnoreCase(PluginJSonUtils.getJsonValue(ajax, "passwordFail"))) {
                                if (i + 1 > tries) {
                                    throw new DecrypterException(DecrypterException.PASSWORD);
                                }
                                continue;
                            } else if ("true".equalsIgnoreCase(PluginJSonUtils.getJsonValue(ajax, "captchaFail"))) {
                                if (i + 1 > tries) {
                                    throw new DecrypterException(DecrypterException.CAPTCHA);
                                }
                                // {"message":"SolveMedia response is not valid (checksum error).","captchaFail":true}
                                // {"message":"puzzle expired","captchaFail":true}
                                // session timed out (due to dialog been open for too long) or captcha solution is wrong!
                                continue;
                            } else {
                                // unknown error
                                throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
                            }
                        }
                    }
                }
            }
            final String linkss = PluginJSonUtils.getJsonArray(ajax, "links");
            // lets get links now, these should be in an Json array
            final String[] links = PluginJSonUtils.getJsonResultsFromArray(linkss);
            if (links != null) {
                for (final String link : links) {
                    // we want the url json
                    final String url = PluginJSonUtils.getJsonValue(link, "url");
                    if (url != null) {
                        decryptedLinks.add(createDownloadlink(url));
                    }
                }
            }
        }
        if (decryptedLinks.isEmpty()) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private Browser ajax = null;

    protected final void ajaxPostPageRaw(final String url, final String post) throws Exception {
        setAjaxForPost();
        postPageRaw(ajax, url, post, true);
    }

    private void setAjaxForGet() {
        ajax = br.cloneBrowser();
        // ajax.getHeaders().put("Accept", "*/*");
        // ajax.getHeaders().put("X-Request-With" , "XMLHttpRequest");
        ajax.getHeaders().put("Accept", "application/json, text/plain, */*");
    }

    private void setAjaxForPost() {
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "application/json, text/plain, */*");
        ajax.getHeaders().put("Content-Type", "application/json;charset=UTF-8");
        ajax.getHeaders().put("Origin", new Regex(br.getURL(), "https?://[^/]+").getMatch(-1));
    }

    public ArrayList<DownloadLink> decryptIt_oldStyle(final CryptedLink param, final ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // link correction
        parameter = correctLink(param.toString());
        // setuid
        uid = getUID(parameter);
        getPage(parameter);
        if (isOffline()) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        // shortlink i assume
        if (parameter.matches(regexLinkShort())) {
            String newparameter = br.getRedirectLocation();
            if (newparameter == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* redirect goes to outside link or back own shortlink */
            if (!newparameter.matches(regexLinkD() + "|" + regexLinkP())) {
                decryptedLinks.add(createDownloadlink(newparameter));
                return decryptedLinks;
            }
            parameter = correctLink(newparameter);
            getPage(parameter);
        }
        if (br.getRedirectLocation() != null && br.getRedirectLocation().matches("^.+" + regexSupportedDomains() + "/404$")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML(">This link does not exist")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        if (parameter.matches(regexLinkD())) {
            decryptedLinks.add(decryptSingleLink(br));
        } else {
            handleCaptcha(param);
            decryptedLinks.addAll(decryptMultipleLinks(param));
        }
        if (decryptedLinks.isEmpty()) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    protected String getUID(final String parameter) {
        return new Regex(parameter, "/([A-Za-z0-9]+)$").getMatch(0);
    }

    protected boolean isOffline() {
        return (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 404) || br.containsHTML(regexOffline());
    }

    private String regexOffline() {
        return ">404 Page/File not found<|>The page/file you've requested has been moved";
    }

    /**
     * Setter for when the site supports HTTPS
     *
     * @return
     */
    protected abstract boolean supportsHTTPS();

    /**
     * Setter for when the site enforces HTTPS
     *
     * @return
     */
    protected abstract boolean enforcesHTTPS();

    /**
     * Setter to use random User-Agent
     *
     * @return
     */
    protected abstract boolean useRUA();

    protected String regexSupportedDomains() {
        return Pattern.quote(this.getHost());
    }

    /**
     * doesn't always use primary host.
     *
     * @return
     */
    protected String regexLinkShort() {
        return "https?://[^/]*" + getShortHost() + "/[a-zA-Z0-9]+";
    }

    /**
     * according to safelinking admin
     *
     * @return
     */
    protected static final String regexBase58() {
        return "[123456789abcdefghijklmnopqrstuwxyzABCDEFGHIJKLMNPQRSTUWXYZ]{7}";
    }

    protected String regexLinkBase58() {
        return "https?://[^/]*" + regexSupportedDomains() + "/" + regexBase58();
    }

    protected String getShortHost() {
        return "";
    }

    protected String regexLinkP() {
        return "https?://[^/]*" + regexSupportedDomains() + "/p/[a-z0-9]+";
    }

    protected String regexLinkD() {
        return "https?://[^/]*" + regexSupportedDomains() + "/d/[a-z0-9]+";
    }

    protected String correctLink(final String string) {
        final String s = string.replaceFirst("^https?://", enforcesHTTPS() || supportsHTTPS() ? "https://" : "http://");
        return s;
    }

    public int getCaptchaTypeNumber() {
        if ("solvemedia".equals(cType)) {
            return 1;
        } else if ("recaptcha".equals(cType)) {
            return 2;
        } else if ("basic".equals(cType)) {
            return 3;
        } else if ("threeD".equals(cType)) {
            return 4;
        } else if ("fancy".equals(cType)) {
            return 5;
        } else if ("qaptcha".equals(cType)) {
            return 6;
        } else if ("simple".equals(cType)) {
            return 7;
        } else if ("dotty".equals(cType)) {
            return 8;
        } else if ("cool".equals(cType)) {
            return 9;
        } else if ("standard".equals(cType)) {
            return 10;
        } else if ("cats".equals(cType)) {
            return 11;
        } else if ("simplecaptcha".equals(cType)) {
            return 12;
        } else {
            // Not detected or other case
            return 0;
        }
    }

    protected String regexCaptchaSolveMedia() {
        return "api(-secure)?\\.solvemedia\\.com/(papi)?";
    }

    protected String regexCaptchaRecaptcha() {
        return "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)";
    }

    protected String regexCaptchaBasic() {
        return "(https?://[^/]*" + regexSupportedDomains() + "/includes/captcha_factory/securimage/securimage_(show\\.php\\?hash=[a-z0-9]+|register\\.php\\?hash=[^\"]+sid=[a-z0-9]{32}))";
    }

    protected String regexCaptchaThreeD() {
        return "\"(https?://[^/]*" + regexSupportedDomains() + "/includes/captcha_factory/3dcaptcha/3DCaptcha\\.php)\"";
    }

    protected String regexCaptchaFancy() {
        return "class=\"captcha_image ajax-fc-container\"";
    }

    protected String regexCaptchaQaptcha() {
        return "class=\"protected-captcha\"><div id=\"QapTcha\"";
    }

    protected String regexCaptchaSimplecaptcha() {
        return "\"https?://[^/]*" + regexSupportedDomains() + "/simplecaptcha/captcha\\.php\"";
    }

    protected String regexCaptchaCatAndDog() {
        return "\"https?://[^/]*" + regexSupportedDomains() + "/includes/captcha_factory/catsdogs/catdogcaptcha\\.php\\?";
    }

    protected void handleCaptcha(final CryptedLink param) throws Exception {
        br.setFollowRedirects(true);
        LinkedHashMap<String, String> captchaRegex = new LinkedHashMap<String, String>();
        captchaRegex.put("solvemedia", regexCaptchaSolveMedia());
        captchaRegex.put("recaptcha", regexCaptchaRecaptcha());
        captchaRegex.put("basic", regexCaptchaBasic());
        captchaRegex.put("threeD", regexCaptchaThreeD());
        captchaRegex.put("fancy", regexCaptchaFancy());
        captchaRegex.put("qaptcha", regexCaptchaQaptcha());
        captchaRegex.put("simplecaptcha", regexCaptchaSimplecaptcha());
        captchaRegex.put("cats", regexCaptchaCatAndDog());

        /* search for protected form */
        Form protectedForm = formProtected();
        if (protectedForm != null) {
            String psw = null;
            boolean password = formInputFieldContainsProperties(protectedForm, formPasswordInputProperties());
            prepareCaptchaAdress(protectedForm.getHtmlCode(), captchaRegex);

            final int repeat = 5;
            for (int i = 0; i < repeat; i++) {
                while (protectedForm.hasInputFieldByName("%5C")) {
                    protectedForm.remove("%5C");
                }
                if (password) {
                    if ((i <= 2 && isCaptchaSkipable()) || (i <= 1 && !isCaptchaSkipable())) {
                        // only use this twice, once for auto solvemedia, and second time with manual captcha!
                        psw = param.getDecrypterPassword();
                    }
                    if (psw == null || "".equals(psw)) {
                        psw = getUserInput(parameter, param);
                        if (psw == null || "".equals(psw)) {
                            throw new DecrypterException(DecrypterException.PASSWORD);
                        }
                    }
                    protectedForm.put(formPasswordInputKeyName(), Encoding.urlEncode(psw));
                }

                Browser captchaBr = null;
                if (!"notDetected".equals(cType)) {
                    captchaBr = br.cloneBrowser();
                }

                switch (getCaptchaTypeNumber()) {
                case 1:
                    if (i == 0 && isCaptchaSkipable()) {
                        long wait = 0;
                        while (wait < 3000) {
                            wait = 1272 * new Random().nextInt(6);
                        }
                        sleep(wait, param);
                        protectedForm.put("captchatype", "Simple");
                        protectedForm.put("used_captcha", "SolveMedia");
                        protectedForm.put("adcopy_challenge", "null");
                        protectedForm.put("adcopy_response", "");
                        break;
                    } else {
                        org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                        File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                        String code = getCaptchaCode(cf, param);
                        String chid = sm.getChallenge(code);
                        protectedForm.put("solvemedia_response", Encoding.urlEncode(code));
                        protectedForm.put("adcopy_challenge", Encoding.urlEncode(chid));
                        protectedForm.put("adcopy_response", Encoding.urlEncode(code));
                        break;
                    }
                case 2: {
                    final Recaptcha rc = new Recaptcha(br, this);
                    rc.parse();
                    rc.load();
                    File cfRe = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String code = getCaptchaCode(cfRe, param);
                    /* Sometimes that field already exists containing the value "manuel_challenge" */
                    protectedForm.remove("recaptcha_response_field");
                    protectedForm.put("recaptcha_challenge_field", rc.getChallenge());
                    protectedForm.put("recaptcha_response_field", Encoding.urlEncode(code));
                    break;
                }
                case 3: {
                    final String code = getCaptchaCode(br.getRegex(captchaRegex.get("basic")).getMatch(0), param);
                    protectedForm.put("securimage_response_field", Encoding.urlEncode(code));
                    break;
                }
                case 4: {
                    final String code = getCaptchaCode(br.getRegex(captchaRegex.get("threeD")).getMatch(0), param);
                    protectedForm.put("3dcaptcha_response_field", Encoding.urlEncode(code));
                    break;
                }
                case 5: {
                    captchaBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    getPage(captchaBr, getCaptchaFancyUrl());
                    protectedForm.put(getCaptchaFancyInputfieldName(), captchaBr.toString().trim());
                    break;
                }
                case 6: {
                    captchaBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    postPage(captchaBr, "/includes/captcha_factory/Qaptcha.jquery.php?hash=" + uid, "action=qaptcha");
                    if (!captchaBr.containsHTML("\"error\":false")) {
                        logger.warning("Decrypter broken for link: " + parameter + "\n");
                        logger.warning("Qaptcha handling broken");
                        if (password) {
                            throw new DecrypterException(DecrypterException.PASSWORD);
                        }
                    }
                    protectedForm.put("iQapTcha", "");
                    break;
                }
                case 0:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11: {
                    // kprotector.com, click to proceed step, prior to captcha.
                    if (protectedForm.getInputFieldByType("button") != null && "Click+To+Proceed".equals(protectedForm.getInputFieldByType("button").getValue())) {
                        submitForm(protectedForm);
                        protectedForm = formProtected();
                        prepareCaptchaAdress(protectedForm.getHtmlCode(), captchaRegex);
                        continue;
                    } else {
                        // unsupported types
                        // short wait to prevent hammering
                        sleep(2500, param);
                        // maybe also good to clear cookies?
                        getPage(br.getURL());
                        protectedForm = formProtected();
                        prepareCaptchaAdress(protectedForm.getHtmlCode(), captchaRegex);
                        continue;
                    }
                }
                case 12: {
                    final String result = getCaptchaCode("/simplecaptcha/captcha.php", param);
                    protectedForm.put("captchatype", "Simple");
                    protectedForm.put("norobot", Encoding.urlEncode(result));
                    break;
                }
                }
                if (captchaRegex.containsKey(cType) || password) {
                    submitForm(protectedForm);
                    if (br.getHttpConnection().getResponseCode() == 500) {
                        logger.warning("500 Internal Server Error. Link: " + parameter);
                        continue;
                    }
                    if (password) {
                        protectedForm = formProtected();
                        password = formInputFieldContainsProperties(protectedForm, formPasswordInputProperties());
                        if (password) {
                            if (i + 1 > repeat) {
                                throw new DecrypterException(DecrypterException.PASSWORD);
                            }
                            prepareCaptchaAdress(protectedForm.getHtmlCode(), captchaRegex);
                            continue;
                        }
                        break;
                    }
                    protectedForm = formProtected();
                    /*
                     * I doubt that the following is required, (confirmationCheck() || br.containsHTML(
                     * "<strong>Prove you are human</strong>")). it shouldn't be, but if the form is present even after successful
                     * captcha&|password step, we will need to reinstate. -raz 20150510
                     */
                    if (protectedForm != null) {
                        if (i + 1 > repeat) {
                            throw new DecrypterException(DecrypterException.CAPTCHA);
                        }
                        prepareCaptchaAdress(protectedForm.getHtmlCode(), captchaRegex);
                        continue;
                    }
                }
                break;
            }
        }
    }

    protected String getCaptchaFancyInputfieldName() {
        return "fancy-captcha";
    }

    protected String getCaptchaFancyUrl() {
        return "/includes/captcha_factory/fancycaptcha.php?hash=" + uid;
    }

    protected boolean isCaptchaSkipable() {
        return false;
    }

    protected boolean confirmationCheck() {
        return false;
    }

    protected String[][] formPasswordInputProperties() {
        return new String[][] { { "type", "password" }, { "name", formPasswordInputKeyName() } };
    }

    protected String formPasswordInputKeyName() {
        return "link-password";
    }

    /**
     * returns true when inputfield has property matching.
     *
     * @author raztoki
     * @param form
     * @param inputProperties
     * @return
     */
    protected final boolean formInputFieldContainsProperties(final Form form, final String[][] inputProperties) {
        if (form == null || inputProperties == null) {
            return false;
        }
        for (final InputField i : form.getInputFields()) {
            ArrayList<Boolean> v = new ArrayList<Boolean>(inputProperties.length);
            int d = -1;
            for (final String[] fpip : inputProperties) {
                d++;
                v.add(d, null);
                if ("type".equalsIgnoreCase(fpip[0]) && fpip[1].equalsIgnoreCase(i.getType())) {
                    v.add(d, Boolean.TRUE);
                } else if ("name".equalsIgnoreCase(fpip[0]) && fpip[1].equalsIgnoreCase(i.getKey())) {
                    v.add(d, Boolean.TRUE);
                } else if ("value".equalsIgnoreCase(fpip[0]) && fpip[1].equalsIgnoreCase(i.getValue())) {
                    v.add(d, Boolean.TRUE);
                } else {
                    final String property = i.getProperty(fpip[0], null);
                    if (fpip[1].equalsIgnoreCase(property)) {
                        v.add(d, Boolean.TRUE);
                    }
                }
            }
            Boolean isMapAllTrue = null;
            for (int x = 0; x != inputProperties.length; x++) {
                final Boolean b = v.get(x);
                if (Boolean.FALSE.equals(b) || b == null) {
                    isMapAllTrue = Boolean.FALSE;
                    break;
                } else if (Boolean.TRUE.equals(b)) {
                    isMapAllTrue = Boolean.TRUE;
                }
            }
            if (Boolean.TRUE.equals(isMapAllTrue)) {
                return isMapAllTrue;
            }
        }
        return false;
    }

    protected Form formProtected() {
        final Form f = br.getFormbyProperty("id", "protected-form");
        return f;
    }

    private void prepareCaptchaAdress(String captcha, LinkedHashMap<String, String> captchaRegex) {
        br.getRequest().setHtmlCode(captcha);

        // nullify cType, this is so feedback is correct on retries etc.
        cType = "notDetected";

        for (Entry<String, String> next : captchaRegex.entrySet()) {
            if (br.containsHTML(next.getValue())) {
                cType = next.getKey();
                break;
            }
        }

        logger.info("notDetected".equals(cType) ? "Captcha not detected." : "Detected captcha type \"" + cType + "\".");

        /* detect javascript */
        String javaScript = null;
        for (String js : br.getRegex("<script type=\"text/javascript\">(.*?)</script>").getColumn(0)) {
            if (!new Regex(js, captchaRegex.get(cType)).matches()) {
                continue;
            }
            javaScript = js;
        }
        if (javaScript == null) {
            return;
        }

        /* execute javascript */
        Object result = new Object();
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            /*
             * creating pseudo functions: document.location.protocol + document.write(value)
             */
            engine.eval("var document = { loc : function() { var newObj = new Object(); function protocol() { return \"" + new Regex(br.getURL(), "https?://").getMatch(-1) + "\"; } newObj.protocol = protocol(); return newObj; }, write : function(a) { return a; }}");
            engine.eval("document.location = document.loc();");
            result = engine.eval(javaScript);
        } catch (Throwable e) {
            return;
        }
        String res = result != null ? result.toString() : null;
        br.getRequest().setHtmlCode(res);
    }

    private ArrayList<DownloadLink> loadcontainer(final String format, final CryptedLink param) throws IOException, PluginException {
        ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        String containerLink = br.getRegex(regexContainer(format)).getMatch(0);
        if (containerLink == null) {
            return links;
        }
        Browser brc = br.cloneBrowser();
        final String test = Encoding.htmlDecode(containerLink);
        File file = null;
        URLConnectionAdapter con = null;
        try {
            con = openAntiDDoSRequestConnection(brc, brc.createGetRequest(test));
            if (con.getResponseCode() == 200) {
                try {
                    /* does not exist in 09581 */
                    file = org.appwork.utils.Application.getResource("tmp/generalsafelinking/" + JDHash.getSHA1(test) + format);
                } catch (Throwable e) {
                    file = JDUtilities.getResourceFile("tmp/generalsafelinking/" + JDHash.getSHA1(test) + format);
                }
                if (file == null) {
                    return links;
                }
                file.getParentFile().mkdirs();
                file.deleteOnExit();
                brc.downloadConnection(file, con);
                if (file != null && file.exists() && file.length() > 100) {
                    links.addAll(loadContainerFile(file));
                }
            }
        } catch (final Throwable t) {
            t.printStackTrace();
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
            if (file.exists()) {
                file.delete();
            }
        }
        return links;
    }

    protected String regexContainer(final String format) {
        return "\"(https?://[^/]*" + regexSupportedDomains() + "/c/[a-z0-9]+" + format + ")";
    }

    protected ArrayList<DownloadLink> decryptMultipleLinks(final CryptedLink param) throws Exception {
        ArrayList<String> cryptedLinks = new ArrayList<String>();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        // TODO: Add handling for offline links/containers
        if (supportsContainers()) {
            /* Container handling (if no containers found, use webprotection) */
            decryptedLinks.addAll(loadcontainer(regexContainerDlc(), param));
            if (!decryptedLinks.isEmpty()) {
                return decryptedLinks;
            }
            decryptedLinks.addAll(loadcontainer(regexContainerRsdf(), param));
            if (!decryptedLinks.isEmpty()) {
                return decryptedLinks;
            }
            decryptedLinks.addAll(loadcontainer(regexContainerCcf(), param));
            if (!decryptedLinks.isEmpty()) {
                return decryptedLinks;
            }
        }

        // TODO: don't think this is needed! confirm -raztoki, not required by keeplinks or safemylink
        /* Webprotection decryption */
        if (br.getRedirectLocation() != null && br.getRedirectLocation().equals(parameter)) {
            getPage(parameter);
        }

        for (String[] s : br.getRegex(regexLinks()).getMatches()) {
            for (String[] ss : new Regex(s[0], "<a href=\"(.*?)\"").getMatches()) {
                for (String sss : ss) {
                    if (parameter.equals(sss)) {
                        continue;
                    }
                    cryptedLinks.add(sss);
                }
            }
        }

        for (final String link : cryptedLinks) {
            DownloadLink dl = null;
            if (link.matches(".*" + regexSupportedDomains() + "/d/.+")) {
                Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(false);
                getPage(br2, link);
                dl = decryptSingleLink(br2);
                if (dl == null) {
                    continue;
                }
            } else {
                dl = createDownloadlink(link);
            }
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    protected boolean supportsContainers() {
        return true;
    }

    protected String regexContainerCcf() {
        return "\\.ccf";
    }

    protected String regexContainerRsdf() {
        return "\\.rsdf";
    }

    protected String regexContainerDlc() {
        return "\\.dlc";
    }

    /**
     * links to external source, and /d/ links. Single link will have different regex than list.
     *
     *
     * @return
     */
    protected String regexLinks() {
        return "<div class=\"links-container result-form\">(.*?)</div>";
    }

    protected String regexSingleLinkLocation() {
        return "location=\"(https?://[^\"]+)";
    }

    protected DownloadLink decryptSingleLink(final Browser br) {
        String finallink = br.getRedirectLocation();
        if (finallink == null) {
            finallink = br.getRegex(regexSingleLinkLocation()).getMatch(0);
        }
        if (finallink == null) {
            logger.warning("Sever issues? continuing...");
            logger.warning("Please confirm via browser, and report any bugs to JDownloader Developement Team. :" + parameter);
        }
        // prevent loop
        if (!parameter.equals(finallink)) {
            return createDownloadlink(finallink);
        } else {
            // Shouldn't happen
            return null;
        }
    }

}
