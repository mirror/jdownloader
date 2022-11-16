//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.hcaptcha.CaptchaHelperCrawlerPluginHCaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.controlling.PasswordUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

/**
 * NOTE: <br />
 * - UID case sensitive.<br />
 * - primary captcha is their own. I looked at this couple days earlier, and swear I got solvemedia. So I have placed that code as failover.
 *
 * @version raz_Template-pastebin-201508200000
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "spaste.com", "binbucks.com" }, urls = { "https?://(?:www\\.)?spaste\\.com/(?:(?:site/checkPasteUrl|p/?)\\?c=[a-zA-Z0-9]{10}|s/[a-zA-Z0-9]{6}|r/[a-zA-Z0-9]{6}\\?link=.+)", "https?://(?:www\\.)?(?:binbucks\\.com|binb\\.me)/(?:shrinker/)?[A-Za-z0-9]+" })
public class SpasteCom extends antiDDoSForDecrypt {
    public SpasteCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.PASTEBIN };
    }

    /* DEV NOTES */
    // Tags: pastebin
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String browserReferrer = getBrowserReferrer();
        if (browserReferrer != null) {
            br.setCurrentURL(browserReferrer);
        }
        getPage(param.getCryptedUrl());
        /* Error handling */
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br._getURL().getPath().equals("/site/index")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br._getURL().getPath().endsWith("/login")) {
            /* 2022-06-07: Binbucks.com e.g. binbucks.com/advertisement */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)Page Not Found|<h4>\\s*Oops\\s*!\\s*</h4>|>\\s*The requested paste has been deleted by")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.setFollowRedirects(false);
        int zz = -1;
        while (true) {
            zz++;
            Form form = null;
            for (final Form tmpForm : br.getForms()) {
                final String formID = tmpForm.getStringProperty("id");
                /* E.g. "binbucksCaptcha" or "spasteCaptcha" */
                if (formID != null && formID.matches("[a-z]+Captcha")) {
                    form = tmpForm;
                    break;
                }
            }
            if (form == null) {
                // need a way to break.
                if (zz == 0) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    break;
                }
            }
            if (StringUtils.equalsIgnoreCase(form.getAction(), "bj/site")) {
                /* 2021-11-09: Workaround for binbucks.com */
                form.setAction(br.getURL());
            }
            if (zz > 4) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            final InputField fieldOpen = form.getInputField("open");
            if (fieldOpen != null && fieldOpen.getValue() == null) {
                /* 2021-11-09: Small workaround for parser failure/bad upper handling. */
                fieldOpen.setValue("");
            }
            /**
             * possible 'captcha' field values: </br>
             * 2= captchaScript </br>
             * 6 = image
             */
            // they can have captcha, I've seen Solvemedia and their own
            String captchaScript = null;
            {
                final String[] mm = form.getRegex("<script[^>]*>.*?</script>").getColumn(-1);
                if (mm != null) {
                    for (final String m : mm) {
                        if (m.contains("var myCaptcha")) {
                            captchaScript = m;
                            break;
                        }
                    }
                }
            }
            final String captchaImageURL = br.getRegex("id=\"my-captcha-image\" src=\"(/site/captcha\\?v=[^\"]+)\"").getMatch(0);
            if (form.containsHTML("g-recaptcha")) {
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                // form only has one input...its static
                form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                br.submitForm(form);
            } else if (form.containsHTML("api\\.solvemedia\\.com/papi")) {
                final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                File cf = null;
                try {
                    cf = sm.downloadCaptcha(getLocalCaptchaFile());
                } catch (final Exception e) {
                    if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
                    }
                    throw e;
                }
                final String code = getCaptchaCode("solvemedia", cf, param);
                final String chid = sm.getChallenge(code);
                form.put("adcopy_response", "manual_challenge");
                form.put("adcopy_challenge", Encoding.urlEncode(chid));
                form.put("pasteUrlForm%5Bsubmit%5D", "submit");
                submitForm(form);
                break;
            } else if (this.containsHCaptcha(form)) {
                final String hcaptchaResponse = new CaptchaHelperCrawlerPluginHCaptcha(this, br).getToken();
                form.put("g-recaptcha-response", Encoding.urlEncode(hcaptchaResponse));
                form.put("h-captcha-response", Encoding.urlEncode(hcaptchaResponse));
                submitForm(form);
                break;
            } else if (captchaScript != null) {
                // hello!
                final String hash = getJS(captchaScript, "myCaptchaHash");
                final String captchaAnswerFieldName = getJS(captchaScript, "globalCaptchaHashName");
                final String[] getQuestion = getJSArray(captchaScript, "myCaptchaAns");
                final String[] getImgArray = getJSArray(captchaScript, "myCaptchaImages");
                if (hash == null || captchaAnswerFieldName == null || getQuestion == null || getImgArray == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // stupid
                String result = "";
                c: for (final String q : getQuestion) {
                    int count = -1;
                    for (final String i : getImgArray) {
                        ++count;
                        if (StringUtils.containsIgnoreCase(i, q)) {
                            result += count;
                            continue c;
                        }
                        // some times they do partial word within filename and more in hint, reverse lookup should solve this
                        final String filename = extractFileNameFromURL(i);
                        final String f = new Regex(filename, "\\d*([a-zA-Z]+)\\d*\\.jpg").getMatch(0);
                        if (StringUtils.containsIgnoreCase(q, f)) {
                            result += count;
                            continue c;
                        }
                        /* 2021-11-09: No idea what this was for */
                        if ("building".equalsIgnoreCase(q) && "14291865221429186522index.jpg".equals(filename)) {
                            result += count;
                            continue c;
                        }
                    }
                }
                if (result.length() != getQuestion.length) {
                    // refresh
                    getPage(param.getCryptedUrl());
                    continue;
                }
                /* E.g. "techCaptcha" or "sPasteCaptcha" */
                form.put(captchaAnswerFieldName, Encoding.urlEncode(hash));
                form.put("userEnterHashHere", result);
                form.put("pasteUrlForm%5Bsubmit%5D", "submit");
                submitForm(form);
            } else if (captchaImageURL != null) {
                /* Simple image captcha */
                final String code = this.getCaptchaCode(captchaImageURL, param);
                form.put("DynamicModel%5BverificationCode%5D", code);
                submitForm(form);
            } else {
                break;
            }
        }
        final String directRedirect = br.getRedirectLocation();
        if (directRedirect != null) {
            decryptedLinks.add(this.createDownloadlink(directRedirect));
        } else {
            /* Look for other/multiple URLs. */
            final String plaintxt;
            // /s links have a different format
            if (param.getCryptedUrl().contains("spaste.com/s/") || param.getCryptedUrl().contains("spaste.com/r/")) {
                // we need some info
                final String id = br.getRegex("\\$\\.post\\(\"/site/getRedirectLink\",\\{id:'(\\d+)'\\}").getMatch(0);
                if (id == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // ajax request
                br.getHeaders().put("Accept", "*/*");
                br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                br.getHeaders().put("Accept", "*/*");
                sleep(5000, param);
                postPage("/site/getRedirectLink", "id=" + Encoding.urlEncode(id));
                plaintxt = br.toString();
            } else {
                // look for content! within '/p/c=? + uid', by the way you cant just jump to it.
                plaintxt = br.getRegex("class=\"required input-block-level pasteContent\"(.*?)(?:</div>\\s*){3}").getMatch(0);
            }
            if (plaintxt == null) {
                // this isn't always an error! there might not be any links!
                logger.info("Could not find 'plaintxt' : " + param.getCryptedUrl());
                return decryptedLinks;
            }
            final Set<String> pws = PasswordUtils.getPasswords(plaintxt);
            final String[] links = HTMLParser.getHttpLinks(plaintxt, "");
            if (links == null || links.length == 0) {
                logger.info("Found no links[] from 'plaintxt' : " + param.getCryptedUrl());
                return decryptedLinks;
            }
            /* Avoid recursion */
            for (final String link : links) {
                if (!this.canHandle(link)) {
                    final DownloadLink dl = createDownloadlink(link);
                    if (pws != null && pws.size() > 0) {
                        dl.setSourcePluginPasswordList(new ArrayList<String>(pws));
                    }
                    decryptedLinks.add(dl);
                }
            }
        }
        return decryptedLinks;
    }

    private final String getJS(final String input, final String key) {
        final String result = new Regex(input, "var\\s+" + Pattern.quote(key) + "\\s*=\\s*('|\"|)(.*?)\\1;").getMatch(1);
        return result;
    }

    private final String[] getJSArray(final String input, final String key) {
        String[] result = null;
        final String array = new Regex(input, "var\\s+" + Pattern.quote(key) + "\\s*=\\s*\\[(.*?)\\];").getMatch(0);
        if (array != null) {
            result = new Regex(array, "('|\")(.*?)\\1,?").getColumn(1);
        }
        return result;
    }
}