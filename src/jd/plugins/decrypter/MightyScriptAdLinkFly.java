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

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "coinlink.co" }, urls = { "https?://(?:www\\.)?(?:coinlink\\.co|adlink\\.guru|short\\.es|tmearn\\.com|cut-urls\\.com|ibly\\.co|adshort\\.co|brlink\\.in|urle\\.co|mitly\\.us)/[A-Za-z0-9]+" })
public class MightyScriptAdLinkFly extends antiDDoSForDecrypt {

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "coinlink.co", "adlink.guru", "short.es", "tmearn.com", "cut-urls.com", "ibly.co", "adshort.co", "brlink.in", "urle.co", "mitly.us" };
    }

    public MightyScriptAdLinkFly(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        Form form = br.getForm(0);
        if (form == null) {
            return null;
        }
        // original captcha
        if (form.hasInputFieldByName("captcha")) {
            final String code = getCaptchaCode("cp.php", param);
            form.put("captcha", Encoding.urlEncode(code));
            submitForm(form);
            if (br.containsHTML("<script>alert\\('(?:Empty Captcha|Incorrect Captcha)\\s*!'\\);")) {
                throw new DecrypterException(DecrypterException.CAPTCHA);
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
        } else if (evalulateCaptcha()) {
            if (evalulateRecaptchaV2(form)) {
                // recaptchav2 is different.
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br) {

                    @Override
                    public String getSiteKey() {
                        final String key = getAppVarsResult("reCAPTCHA_site_key");
                        if (!inValidate(key)) {
                            return key;
                        }
                        return getSiteKey(br.toString());
                    }

                }.getToken();
                form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            } else {
                // unsupported... I've seen reference to solvemedia
                return null;
            }
            submitForm(form);
            // 10 second wait in new version with possible another form
            final Form f2 = br.getForm(0);
            if (f2 != null) {
                br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
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

    private boolean evalulateSolvemedia(Form form) {
        return false;
    }

    private boolean evalulateCaptcha() {
        // if ("yes" !== app_vars.enable_captcha) return !0;
        final String hasCaptcha = getAppVarsResult("enable_captcha");
        if ("yes".equals(hasCaptcha)) {
            return true;
        }
        return false;
    }

    private boolean evalulateRecaptchaV2(final Form form) {
        final String captchaBtn = form.getRegex("<div [^>]*id=\"captchaShortlink\"[^>]*>").getMatch(-1);
        if (captchaBtn != null) {
            /*
             * "recaptcha" === app_vars.captcha_type && ("" === app_vars.user_id && "1" === app_vars.captcha_short_anonymous &&
             * $("#captchaShort").length && ($("#shorten .btn-captcha").attr("disabled", "disabled"), captchaShort =
             * grecaptcha.render("captchaShort", {
             */
            /*
             * yes" === app_vars.captcha_shortlink && $("#captchaShortlink").length && ($("#link-view
             * .btn-captcha").attr("disabled", "disabled"), captchaShortlink = grecaptcha.render("captchaShortlink", {
             */
            final String captchaType = getAppVarsResult("captcha_type");
            final String userId = getAppVarsResult("user_id");
            if ("recaptcha".equals(captchaType) && "".equals(userId)) {
                return true;
            }
        }
        // fail over, some seem to be using this
        if (form.containsHTML("(?:id|class)=(\"|')g-recaptcha\\1")) {
            return true;
        }

        return false;
    }

    private String getAppVarsResult(final String input) {
        final String result = br.getRegex("app_vars\\['" + Pattern.quote(input) + "'\\]\\s*=\\s*'([^']*)'").getMatch(0);
        return result;
    }

    private String getFinallink() {
        String finallink = PluginJSonUtils.getJsonValue(br, "url");
        if (inValidate(finallink)) {
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