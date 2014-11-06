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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ivpaste.com" }, urls = { "http://(www\\.)?ivpaste\\.com/(v/|view\\.php\\?id=)[A-Za-z0-9]+" }, flags = { 0 })
public class IvPasteCom extends PluginForDecrypt {

    public IvPasteCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String RECAPTCHAFAILED = "(The reCAPTCHA wasn\\'t entered correctly\\.|Go back and try it again\\.)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
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
            if (br.containsHTML("pluscaptcha\\.com/")) {
                if (i >= 5) {
                    throw new DecrypterException(DecrypterException.CAPTCHA);
                }
                logger.info(i + "/3:Unsupported captchatype: " + parameter);
                sleep(1000l, param);
                br.getPage("http://ivpaste.com/p/" + ID);
            } else if (br.containsHTML("api\\.recaptcha\\.net") || br.containsHTML("google\\.com/recaptcha/api/")) {
                if (i >= 5) {
                    throw new DecrypterException(DecrypterException.CAPTCHA);
                }
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((jd.plugins.hoster.DirectHTTP) recplug).getReCaptcha(br);
                String apiKey = br.getRegex("/recaptcha/api/(?:challenge|noscript)\\?k=([A-Za-z0-9%_\\+\\- ]+)").getMatch(0);
                if (apiKey == null) {
                    apiKey = br.getRegex("/recaptcha/api/(?:challenge|noscript)\\?k=([A-Za-z0-9%_\\+\\- ]+)").getMatch(0);
                    if (apiKey == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                Form form = br.getForm(0);
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
            } else if (br.containsHTML("KeyCAPTCHA code")) {
                if (i >= 5) {
                    throw new DecrypterException(DecrypterException.CAPTCHA);
                }
                final PluginForDecrypt keycplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
                final jd.plugins.decrypter.LnkCrptWs.KeyCaptcha kc = ((jd.plugins.decrypter.LnkCrptWs) keycplug).getKeyCaptcha(br);
                String result = null;
                if (auto < 3) {
                    auto++;
                    result = kc.autoSolve(parameter);
                } else {
                    try {
                        result = kc.showDialog(parameter);
                    } catch (final Throwable e) {
                        result = null;
                    }
                }
                if (result == null || "CANCEL".equals(result)) {
                    throw new DecrypterException(DecrypterException.CAPTCHA);
                }
                br.postPage(br.getURL(), "capcode=" + Encoding.urlEncode(result) + "&save=&save=");
            } else if (br.containsHTML("solvemedia.com")) {
                if (i >= 5) {
                    throw new DecrypterException(DecrypterException.CAPTCHA);
                }
                Form form = br.getForm(0);
                PluginForDecrypt solveplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
                jd.plugins.decrypter.LnkCrptWs.SolveMedia sm = ((jd.plugins.decrypter.LnkCrptWs) solveplug).getSolveMedia(br);
                File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                String code = "";
                String chid = sm.getChallenge();
                code = getCaptchaCode(cf, param);
                chid = sm.getChallenge(code);
                form.put("adcopy_challenge", chid);
                form.put("adcopy_response", Encoding.urlEncode(code));
                br.submitForm(form);
            } else {
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