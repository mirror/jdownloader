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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "crypturl.us" }, urls = { "http://[\\w\\.]*?crypturl\\.us/o/[a-z0-9]+" }, flags = { 0 })
public class CrptRlS extends PluginForDecrypt {

    public CrptRlS(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);

        /* File package handling */
        if (br.containsHTML("/imagecreate.php") || br.containsHTML("Passwort")) {
            for (int i = 0; i <= 5; i++) {
                Form captchaForm = br.getForm(0);
                if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
                if (br.containsHTML("/imagecreate.php")) {
                    File file = this.getLocalCaptchaFile();
                    Browser.download(file, br.cloneBrowser().openGetConnection("http://crypturl.us/imagecreate.php"));
                    int[] p = new jd.captcha.specials.GmdMscCm(file).getResult();
                    if (p == null) throw new DecrypterException(DecrypterException.CAPTCHA);
                    captchaForm.put("button.x", p[0] + "");
                    captchaForm.put("button.y", p[1] + "");
                    br.submitForm(captchaForm);
                }
                if (br.containsHTML("type=\"password\"")) {
                    String passCode = null;
                    passCode = Plugin.getUserInput("Password?", param);
                    captchaForm.put("pass", passCode);
                }
                br.submitForm(captchaForm);
                if (br.containsHTML("imagecreate.php") || br.containsHTML("type=\"password\"")) continue;
                break;
            }
        }
        if (br.containsHTML("imagecreate.php") || br.containsHTML("type=\"password\"")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);

        if (br.containsHTML(".dlc")) {
            decryptedLinks = loadcontainer(br, "dlc");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        if (br.containsHTML(".rsdf")) {
            decryptedLinks = loadcontainer(br, "rsdf");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        if (br.containsHTML(".ccf")) {
            decryptedLinks = loadcontainer(br, "ccf");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }
        String[] links = br.getRegex("\"(/link/[a-z0-9]+)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String link : links) {
            link = "http://www.crypturl.us" + link;
            br.getPage(link);
            String finallink = br.getRegex("frame src=\"(.*?)\"").getMatch(0);
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
        String containerlink = br.getRegex("href=\"(/cont/.*?)\"").getMatch(0);
        if (containerlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String containerid = new Regex(containerlink, "/cont/([a-z0-9]+)\\.").getMatch(0);
        if (containerid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String link = "http://crypturl.us/cont/" + containerid + "." + format;
        String test = Encoding.htmlDecode(link);
        File file = null;
        URLConnectionAdapter con = brc.openGetConnection(link);
        if (con.getResponseCode() == 200) {
            file = JDUtilities.getResourceFile("tmp/crypturl/" + test.replaceAll("(http://crypturl.us/|/)", ""));
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
        return null;
    }

}
