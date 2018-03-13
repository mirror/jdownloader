//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "canna.to" }, urls = { "http://(?:uu\\.canna\\.to|ru\\.canna\\.to|85\\.17\\.36\\.224)/(?:cpuser/)?links\\.php\\?action=[^<>\"/\\&]+\\&kat_id=\\d+\\&fileid=\\d+" })
public class CnnT extends PluginForDecrypt {
    public CnnT(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static Object LOCK = new Object();

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        final String host = new Regex(parameter, "https?://([^/]+)/").getMatch(0);
        final String kat_id = new Regex(parameter, "kat_id=(\\d+)").getMatch(0);
        final String fid = new Regex(parameter, "fileid=(\\d+)").getMatch(0);
        parameter = "http://uu.canna.to/links.php?action=popup&kat_id=" + kat_id + "&fileid=" + fid;
        boolean valid = false;
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML(">Versuche es in wenigen Minuten nochmals")) {
            logger.info("Site overloaded at the moment: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("Es existiert kein Eintrag zu dieser ID") || this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        synchronized (LOCK) {
            for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
                final Form[] allforms = br.getForms();
                Form captchaForm = br.getFormbyProperty("name", "download_form");
                if (captchaForm == null) {
                    captchaForm = br.getFormbyProperty("name", "download_form");
                }
                if (captchaForm == null) {
                    captchaForm = allforms[allforms.length - 1];
                }
                final String captchaUrlPart = br.getRegex("\"(securimage_show\\.php\\?sid=[a-z0-9]+)\"").getMatch(0);
                if (captchaUrlPart == null || captchaForm == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final String captchaurl;
                if (this.br.getURL().contains("/cpuser/")) {
                    captchaForm.setAction("/cpuser/links.php?action=load&fileid=" + fid);
                    captchaurl = "http://" + host + "/cpuser/" + captchaUrlPart;
                } else {
                    captchaForm.setAction("/links.php?action=load&fileid=" + fid);
                    captchaurl = "http://" + host + "/" + captchaUrlPart;
                }
                final String captchaCode = getCaptchaCode(captchaurl, param);
                captchaForm.put("cp_captcha", captchaCode);
                br.submitForm(captchaForm);
                if (br.containsHTML("Der Sicherheitscode ist falsch!")) {
                    /* Falscher Captcha, Seite neu laden */
                    br.getPage(parameter);
                } else {
                    valid = true;
                    String finallink = br.getRegex("URL=(.*?)\"").getMatch(0);
                    if (finallink != null) {
                        decryptedLinks.add(createDownloadlink(finallink));
                    }
                    String links[] = br.getRegex("<a target=\"_blank\" href=\"(.*?)\">").getColumn(0);
                    if (links != null && links.length != 0) {
                        for (String link : links) {
                            decryptedLinks.add(createDownloadlink(link));
                        }
                    }
                    break;
                }
            }
        }
        if (valid == false) {
            logger.info("Captcha for the following link was entered wrong for more than 5 times: " + parameter);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }
}