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
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.v2.challenge.areyouahuman.CaptchaHelperCrawlerPluginAreYouHuman;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ivpaste.com" }, urls = { "http://(www\\.)?ivpaste\\.com/(v/|view\\.php\\?id=)[A-Za-z0-9]+" })
public class IvPasteCom extends PluginForDecrypt {
    public IvPasteCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String RECAPTCHAFAILED = "(The reCAPTCHA wasn\\'t entered correctly\\.|Go back and try it again\\.)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        String ID = new Regex(parameter, "ivpaste\\.com/(v/|view\\.php\\?id=)([A-Za-z0-9]+)").getMatch(1);
        if (ID == null) {
            return null;
        }
        br.getPage("http://ivpaste.com/v/" + ID);
        if (br.containsHTML("NO Existe\\!")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        br.getPage("http://ivpaste.com/p/" + ID);
        if (br.containsHTML("<b>Acceda desde: <a")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        // Avoid unsupported captchatype by reloading the page
        int auto = 0;
        int i = 0;
        while (true) {
            i++;
            final Form form = br.getFormbyActionRegex(".*?/p/" + ID);
            if (form == null) {
                break;
            }
            if (i >= 5) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            if (form.containsHTML("pluscaptcha\\.com/") || /* ads captcha */form.containsHTML("api\\.minteye\\.com/|api\\.adscaptcha\\.com/")) {
                logger.info(i + "/3:Unsupported captchatype: " + parameter);
                sleep(1000l, param);
                br.getPage("http://ivpaste.com/p/" + ID);
            } else if (form.containsHTML("areyouahuman\\.com/")) {
                final String areweahuman = new CaptchaHelperCrawlerPluginAreYouHuman(this, br).getToken();
                form.put("session_secret", Encoding.urlEncode(areweahuman));
                form.put("soy_humano_btn", "Submit");
                br.submitForm(form);
            } else if (form.containsHTML("class=(\"|')g-recaptcha\\1") && form.containsHTML("google\\.com/recaptcha")) {
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                br.submitForm(form);
            } else if (form.containsHTML("api\\.recaptcha\\.net") || form.containsHTML("google\\.com/recaptcha/api/")) {
                final Recaptcha rc = new Recaptcha(br, this);
                String apiKey = br.getRegex("/recaptcha/api/(?:challenge|noscript)\\?k=([A-Za-z0-9%_\\+\\- ]+)").getMatch(0);
                if (apiKey == null) {
                    apiKey = br.getRegex("/recaptcha/api/(?:challenge|noscript)\\?k=([A-Za-z0-9%_\\+\\- ]+)").getMatch(0);
                    if (apiKey == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                rc.setForm(form);
                rc.setId(apiKey);
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode("recaptcha", cf, param);
                rc.setCode(c);
                if (br.containsHTML(RECAPTCHAFAILED)) {
                    br.getPage("http://ivpaste.com/p/" + ID);
                    continue;
                }
            } else if (form.containsHTML("KeyCAPTCHA code")) {
                String result = null;
                if (auto < 3) {
                    auto++;
                    result = handleCaptchaChallenge(new KeyCaptcha(this, br, createDownloadlink(parameter)).createChallenge(this));
                } else {
                    result = handleCaptchaChallenge(new KeyCaptcha(this, br, createDownloadlink(parameter)).createChallenge(true, this));
                }
                if (result == null || "CANCEL".equals(result)) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                br.postPage(br.getURL(), "capcode=" + Encoding.urlEncode(result) + "&save=&save=");
            } else if (form.containsHTML("solvemedia\\.com")) {
                org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                String code = "";
                String chid = sm.getChallenge();
                code = getCaptchaCode("solvemedia", cf, param);
                chid = sm.getChallenge(code);
                form.put("adcopy_challenge", chid);
                form.put("adcopy_response", Encoding.urlEncode(code));
                br.submitForm(form);
            } else {
                // this logic is bad, unsupported captcha will result in premature breaking and plugin defect.
                break;
            }
        }
        final String content = br.getRegex("<td nowrap align.*?pre>(.*?)</pre").getMatch(0);
        if (content == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String[] links = new Regex(content, "<a href=\"(.*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.info("Link offline (found no downloadable links): " + parameter);
            return decryptedLinks;
        }
        for (String dl : links) {
            String ID2 = new Regex(dl, "ivpaste\\.com/(v/|view\\.php\\?id=)([A-Za-z0-9]+)").getMatch(1);
            if (ID.equals(ID2)) {
                continue;
            }
            decryptedLinks.add(createDownloadlink(dl));
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }
}