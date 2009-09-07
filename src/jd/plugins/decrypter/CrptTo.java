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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

//by pspzockerscene
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "crypt.to" }, urls = { "http://[\\w\\.]*?(crypt\\.to|mamangu\\.com)/(fid|links),[0-9]+" }, flags = { 0 })
public class CrptTo extends PluginForDecrypt {

    public CrptTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        br.setFollowRedirects(false);

        /* Error handling */
        if (br.containsHTML("konnte leider nicht gefunden werden")) {
            logger.warning("The requested document was not found on this server.");
            logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            return new ArrayList<DownloadLink>();
        }

        if (br.containsHTML("Passwort bitte hier") || br.containsHTML("/captcha.inc.php")) {
            Form captchaForm = br.getForm(0);
            if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            String passCode = null;
            // Captcha handling
            if (br.containsHTML("/captcha.inc.php")) {
                String captchalink = "http://crypt.to/inc/captcha.inc.php";
                String code = getCaptchaCode(captchalink, param);
                captchaForm.put("pruefcode", code);
            }
            // Password handling
            if (br.containsHTML("Passwort bitte hier")) {
                passCode = Plugin.getUserInput("Password?", param);
                captchaForm.put("pw", passCode);
            }
            br.submitForm(captchaForm);
            // Wrong-captcha/PW errorhandling
            if (br.containsHTML("Passwort falsch oder das Captcha wurde nicht richtig")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        // "waittime"-check

        Form checkform = br.getForm(0);
        if (checkform != null) {
            br.submitForm(checkform);
        }

        // container handling (if no containers found, use webprotection)

        if (br.containsHTML("Links im rsdf-Format herunterladen")) {
            decryptedLinks = loadcontainer(br, "1");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }
        if (br.containsHTML("Links im dlc-Format herunterladen")) {
            decryptedLinks = loadcontainer(br, "0");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        if (br.containsHTML("Links im ccf-Format herunterladen")) {
            decryptedLinks = loadcontainer(br, "2");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        // Webprotection decryption
        String linkid = br.getRegex("window\\.setTimeout\\('out\\(\\\\'(.*?)\\\\'").getMatch(0);
        String[] links = br.getRegex("window\\.setTimeout\\('out\\(\\\\'[0-9a-z]+\\\\', \\\\'(\\d+)\\\\'").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String link : links) {
            String link0 = "http://crypt.to/iframe.php?linkkey=" + linkid + "&row=" + link;
            br.getPage(link0);
            String finallink = br.getRedirectLocation();
            if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            DownloadLink dl = createDownloadlink(finallink);
            decryptedLinks.add(dl);
            progress.increase(1);
        }
        return decryptedLinks;
    }

    // @Override
    private ArrayList<DownloadLink> loadcontainer(Browser br, String format) throws IOException, PluginException {
        Browser brc = br.cloneBrowser();
        String[] dlclinks = br.getRegex("(dlc://crypt.to/container,[0-9]+," + format + ".*?)\"").getColumn(0);
        if (dlclinks == null || dlclinks.length == 0) return null;
        for (String link : dlclinks) {
            String test0 = Encoding.htmlDecode(link);
            String test = test0.replace("dlc", "http");
            File file = null;
            URLConnectionAdapter con = brc.openGetConnection(link);
            if (con.getResponseCode() == 200) {
                file = JDUtilities.getResourceFile("tmp/cryptto/" + test.replace("http://crypt.to/", "") + "." + format);
                if (file == null) return null;
                file.deleteOnExit();
                brc.downloadConnection(file, con);
            } else {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            }

            if (file != null && file.exists() && file.length() > 100) {
                ArrayList<DownloadLink> decryptedLinks = JDUtilities.getController().getContainerLinks(file);
                if (decryptedLinks.size() > 0) return decryptedLinks;
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            }
        }
        return null;
    }

}
