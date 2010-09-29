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
import java.io.IOException;
import java.util.ArrayList;

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "safelinking.net" }, urls = { "http://[\\w\\.]*?safelinking\\.net/(p|d)/[a-z0-9]+" }, flags = { 0 })
public class SflnkgNt extends PluginForDecrypt {

    public SflnkgNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String RECAPTCHATEXT         = "api\\.recaptcha\\.net";
    private static String       CAPTCHAREGEX1         = "\"(http://safelinking\\.net/includes/captcha_factory/securimage/securimage_show\\.php\\?sid=[a-z0-9]+)\"";
    private static String       CAPTCHAREGEX2         = "\"(http://safelinking\\.net/includes/captcha_factory/3dcaptcha/3DCaptcha\\.php)\"";
    private static String       CAPTCHATEXT3          = "fancycaptcha\\.css\"";
    private static String       PASSWORDPROTECTEDTEXT = "type=\"password\" name=\"link-password\"";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.containsHTML("(\"This link does not exist\\.\"|ERROR - this link does not exist)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        if (br.containsHTML(">Not yet checked</span>")) throw new DecrypterException("Not yet checked");
        if (!parameter.contains("/d/")) {
            Form capForm = new Form();
            capForm.put("post-protect", "1");
            capForm.setMethod(MethodType.POST);
            capForm.setAction(parameter);
            for (int i = 0; i <= 5; i++) {
                if (br.containsHTML(PASSWORDPROTECTEDTEXT)) {
                    capForm.put("link-password", getUserInput(null, param));
                }
                if (br.containsHTML(RECAPTCHATEXT)) {
                    PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    rc.parse();
                    rc.load();
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    capForm.put("recaptcha_challenge_field", rc.getChallenge());
                    capForm.put("recaptcha_response_field", getCaptchaCode(cf, param));
                } else if (br.getRegex(CAPTCHAREGEX1).getMatch(0) != null) {
                    capForm.put("securimage_response_field", getCaptchaCode(br.getRegex(CAPTCHAREGEX1).getMatch(0), param));
                } else if (br.getRegex(CAPTCHAREGEX2).getMatch(0) != null) {
                    capForm.put("3dcaptcha_response_field", getCaptchaCode(br.getRegex(CAPTCHAREGEX2).getMatch(0), param));
                } else if (br.containsHTML(CAPTCHATEXT3)) {
                    Browser xmlbrowser = br.cloneBrowser();
                    xmlbrowser.getPage("http://safelinking.net/includes/captcha_factory/fancycaptcha.php");
                    capForm.put("fancy-captcha", xmlbrowser.toString().trim());
                }
                br.submitForm(capForm);
                if (br.containsHTML(RECAPTCHATEXT) || br.getRegex(CAPTCHAREGEX1).getMatch(0) != null || br.getRegex(CAPTCHAREGEX2).getMatch(0) != null || br.containsHTML(PASSWORDPROTECTEDTEXT)) continue;
                if (br.containsHTML(CAPTCHATEXT3)) {
                    logger.warning("Captcha3 captchahandling failed for link: " + parameter);
                    return null;
                }
                break;
            }
            if (br.containsHTML(RECAPTCHATEXT) || br.getRegex(CAPTCHAREGEX1).getMatch(0) != null || br.getRegex(CAPTCHAREGEX2).getMatch(0) != null) throw new DecrypterException(DecrypterException.CAPTCHA);
            if (br.containsHTML(PASSWORDPROTECTEDTEXT)) throw new DecrypterException(DecrypterException.PASSWORD);
            if (br.containsHTML(">All links are dead\\.<")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            // container handling (if no containers found, use webprotection
            if (br.containsHTML("\\.dlc")) {
                decryptedLinks = loadcontainer(".dlc", param);
                if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
            }

            if (br.containsHTML("\\.rsdf")) {
                decryptedLinks = loadcontainer(".rsdf", param);
                if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
            }

            if (br.containsHTML("\\.ccf")) {
                decryptedLinks = loadcontainer(".ccf", param);
                if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
            }
            decryptedLinks = new ArrayList<DownloadLink>();
            // Webprotection decryption
            String[] links = br.getRegex("class=\"linked\">(http://safelinking\\.net/d/.*?)</a>").getColumn(0);
            if (links == null || links.length == 0) {
                String allLinks = br.getRegex("class=\"link-box\" id=\"direct-links\" >(.*?<a href=\".*?)</div>").getMatch(0);
                if (allLinks != null) links = new Regex(allLinks, "<a href=\"(.*?)\"").getColumn(0);
                if (links == null || links.length == 0) {
                    links = br.getRegex("\"(http://safelinking\\.net/d/[a-z0-9]+)\"").getColumn(0);
                    if (links == null || links.length == 0) {
                        links = br.getRegex("class=\"linked\">(http://.*?)</a>").getColumn(0);
                    }
                }
            }
            if (links == null || links.length == 0) return null;
            progress.setRange(links.length);
            for (String link : links) {
                if (!link.contains("safelinking.net/")) {
                    decryptedLinks.add(createDownloadlink(link));
                } else {
                    br.getPage(link);
                    String finallink = br.getRedirectLocation();
                    if (finallink == null) {
                        logger.warning("Decrypter broken, decryption stopped at link: " + link);
                        return null;
                    }
                    if (!parameter.equals(finallink)) decryptedLinks.add(createDownloadlink(finallink));
                }
                progress.increase(1);
            }
        } else {
            if (br.getRedirectLocation() == null) {
                logger.warning("Error in single-link handling for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> loadcontainer(String format, CryptedLink param) throws IOException, PluginException {
        ArrayList<DownloadLink> decryptedLinks = null;
        Browser brc = br.cloneBrowser();
        String containerLink = br.getRegex("\"(http://safelinking\\.net/c/[a-z0-9]+" + format + ")").getMatch(0);
        if (containerLink == null) {
            logger.warning("Contailerlink for link " + param.toString() + " for format " + format + " could not be found.");
            return null;
        }
        String test = Encoding.htmlDecode(containerLink);
        File file = null;
        URLConnectionAdapter con = brc.openGetConnection(test);
        if (con.getResponseCode() == 200) {
            file = JDUtilities.getResourceFile("tmp/safelinknet/" + test.replaceAll("(:|/|\\?)", "") + format);
            if (file == null) return null;
            file.deleteOnExit();
            brc.downloadConnection(file, con);
            if (file != null && file.exists() && file.length() > 100) {
                decryptedLinks = JDUtilities.getController().getContainerLinks(file);
            }
        } else {
            con.disconnect();
            return null;
        }

        if (file != null && file.exists() && file.length() > 100) {
            if (decryptedLinks.size() > 0) return decryptedLinks;
        } else {
            return null;
        }
        return null;
    }
}
