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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "protectbox.in" }, urls = { "http://[\\w\\.]*?protectbox\\.in/.*" }, flags = { 0 })
public class PrtctBxn extends PluginForDecrypt {

    public PrtctBxn(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        FilePackage fp = FilePackage.getInstance();
        br.getPage(parameter);
        for (int i = 0; i <= 3; i++) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, param);
            rc.setCode(c);
            if (br.containsHTML("Sie haben den Captcha leider falsch eingeben")) continue;
            break;
        }
        if (br.containsHTML("Sie haben den Captcha leider falsch eingeben")) throw new DecrypterException(DecrypterException.CAPTCHA);
        // container handling (if no containers found, use webprotection
        if (br.containsHTML(">DLC<")) {
            decryptedLinks = loadcontainer(br, "dlc");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        if (br.containsHTML(">RSDF<")) {
            decryptedLinks = loadcontainer(br, "rsdf");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        if (br.containsHTML(">CCF<")) {
            decryptedLinks = loadcontainer(br, "ccf");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        /* Password handling */
        String pass = br.getRegex("<b>Passwort:</b>.*?</td>.*?<td>(.*?)</td>").getMatch(0);
        String fpName = br.getRegex("<b>Ordnername:</b>.*?</td>.*?<td>(.*?)</td>").getMatch(0);
        if (fpName != null) fpName = fpName.trim();
        fp.setName(fpName);
        String[] links = br.getRegex("\"(out\\.php\\?id=.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String link : links) {
            link = "http://www.protectbox.in/" + link;
            br.getPage(link);
            String finallink = br.getRedirectLocation();
            if (finallink == null) throw new DecrypterException(DecrypterException.CAPTCHA);
            DownloadLink dl_link = createDownloadlink(finallink);
            if (pass != null && pass.length() != 0) {
                pass = pass.trim();
                dl_link.addSourcePluginPassword(pass);
            }
            decryptedLinks.add(dl_link);
            progress.increase(1);
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    // By Jiaz
    private ArrayList<DownloadLink> loadcontainer(Browser br, String format) throws IOException, PluginException {
        Browser brc = br.cloneBrowser();
        String[] dlclinks = br.getRegex("\"(container/[0-9a-zA-Z]+\\." + format + ".*?)\"").getColumn(0);
        if (dlclinks == null || dlclinks.length == 0) return null;
        for (String link : dlclinks) {
            link = "http://www.protectbox.in/" + link;
            String test = Encoding.htmlDecode(link);
            File file = null;
            URLConnectionAdapter con = brc.openGetConnection(link);
            if (con.getResponseCode() == 200) {
                file = JDUtilities.getResourceFile("tmp/protectbox/" + test.replace("http://www.protectbox.in/", "") + "." + format);
                if (file == null) return null;
                file.deleteOnExit();
                brc.downloadConnection(file, con);
            } else {
                con.disconnect();
                return null;
            }

            if (file != null && file.exists() && file.length() > 100) {
                ArrayList<DownloadLink> decryptedLinks = JDUtilities.getController().getContainerLinks(file);
                if (decryptedLinks.size() > 0) return decryptedLinks;
            } else {
                return null;
            }
        }
        return null;
    }

}
