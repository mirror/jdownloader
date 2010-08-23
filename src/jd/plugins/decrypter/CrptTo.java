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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

//by pspzockerscene
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "crypt.to" }, urls = { "http://[\\w\\.]*?(crypt\\.to|mamangu\\.com)/(fid|links),[0-9a-zA-Z]+" }, flags = { 0 })
public class CrptTo extends PluginForDecrypt {

    public CrptTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
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
            Form captchaForm = null;
            String passCode = null;
            if (br.containsHTML("Passwort bitte hier")) {
                passCode = getUserInput(null, param);
            }
            for (int i = 0; i < 5; i++) {
                // Captcha handling
                long time = System.currentTimeMillis();
                captchaForm = br.getForm(0);
                if (passCode != null) captchaForm.put("pw", passCode);

                if (br.containsHTML("/captcha.inc.php")) {

                    String captchalink = "http://crypt.to/inc/captcha.inc.php";
                    String code = getCaptchaCode(captchalink, param);

                    captchaForm.put("pruefcode", code);
                }
                if (br.containsHTML("delaySubmit")) {
                    long delay = Long.parseLong(br.getRegex("delaySubmit\\(this, 1000, (\\d+)").getMatch(0)) - (System.currentTimeMillis() - time);
                    sleep(delay, param);
                }
                br.submitForm(captchaForm);
                if (br.containsHTML("Passwort falsch oder das Captcha")) {
                    br.getPage(parameter);
                } else
                    break;
            }
            if (br.containsHTML("Passwort bitte hier") || br.containsHTML("/captcha.inc.php")) throw new DecrypterException(DecrypterException.CAPTCHA);
            if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        }

        // container handling (if no containers found, use webprotection)

        if (br.containsHTML("Links im dlc-Format herunterladen")) {
            decryptedLinks = loadcontainer(br, 0, param);
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }
        if (br.containsHTML("Links im rsdf-Format herunterladen")) {
            decryptedLinks = loadcontainer(br, 1, param);
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }
        if (br.containsHTML("Links im ccf-Format herunterladen")) {
            decryptedLinks = loadcontainer(br, 2, param);
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        // Webprotection decryption
        String linkid = br.getRegex("window\\.setTimeout\\('out\\(\\\\'(.*?)\\\\'").getMatch(0);
        String[] links = br.getRegex("window\\.setTimeout\\('out\\(\\\\'[0-9a-z]+\\\\', \\\\'(\\d+)\\\\'").getColumn(0);
        if (links == null || links.length == 0 || linkid == null) return null;
        progress.setRange(links.length);
        decryptedLinks = new ArrayList<DownloadLink>();
        for (String link : links) {
            String link0 = "http://crypt.to/iframe.php?linkkey=" + linkid + "&row=" + link;
            Browser clone = br.cloneBrowser();
            clone.getPage(link0);
            String finallink = clone.getRedirectLocation();
            // System.out.println(finallink);

            // if (finallink == null) throw new
            // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            decryptedLinks.add(createDownloadlink(finallink));

            progress.increase(1);
            Thread.sleep(200);
        }
        if (decryptedLinks.size() == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return decryptedLinks;
    }

    /**
     * 0 = dlc 1 = rsdf 2 = ccf
     */
    private ArrayList<DownloadLink> loadcontainer(Browser br, int format, CryptedLink param) throws IOException, PluginException {
        Browser brc = br.cloneBrowser();
        String dlclink = param.toString().replace("/fid,", "/container,") + "," + format;
        logger.info("Containerlink = " + dlclink);
        String test0 = Encoding.htmlDecode(dlclink);
        String test = test0.replaceFirst("dlc", "http");
        File file = null;
        URLConnectionAdapter con = brc.openGetConnection(test);
        if (con.getResponseCode() == 200) {
            file = JDUtilities.getResourceFile("tmp/cryptto/" + test.replace("http://crypt.to/", "") + "." + (format == 0 ? "dlc" : format == 1 ? "rsdf" : "ccf"));
            if (file == null) return null;
            file.deleteOnExit();
            brc.downloadConnection(file, con);
        } else {
            con.disconnect();
        }

        if (file != null && file.exists() && file.length() > 100) {
            ArrayList<DownloadLink> decryptedLinks = JDUtilities.getController().getContainerLinks(file);
            if (decryptedLinks.size() > 0) return decryptedLinks;
        }
        return null;
    }

}
