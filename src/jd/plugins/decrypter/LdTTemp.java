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
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.pluginUtils.Recaptcha;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "iload.to", "lof.cc" }, urls = { "http://[\\w\\.]*?links\\.iload\\.to/links/\\?lid=.+", "http://[\\w\\.]*?lof\\.cc/[!a-zA-Z0-9_]+" }, flags = { 0, 0 })
public class LdTTemp extends PluginForDecrypt {

    public LdTTemp(PluginWrapper wrapper) {
        super(wrapper);
    }

    // works again
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        for (int i = 0; i <= 5; i++) {
            Recaptcha rc = new Recaptcha(br);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, param);
            rc.setCode(c);
            if (br.containsHTML("(api.recaptcha.net|Das war leider Falsch)")) continue;
            if (br.containsHTML("das Falsche Captcha eingegeben")) {
                sleep(60 * 1001l, param);
                br.getHeaders().put("User-Agent", RandomUserAgent.generate());
                br.getPage(parameter);
                continue;
            }
            break;
        }
        if (br.containsHTML("(api.recaptcha.net|Das war leider Falsch|das Falsche Captcha eingegeben)")) throw new DecrypterException(DecrypterException.CAPTCHA);
        loadcontainer(br, param);
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> loadcontainer(Browser br, CryptedLink param) throws IOException, PluginException {
        ArrayList<DownloadLink> decryptedLinks = null;
        Browser brc = br.cloneBrowser();
        String thelink = param.toString() + "/dlc";
        /*
         * walk from end to beginning, so we load the all in one container first
         */
        String test = Encoding.htmlDecode(thelink);
        File file = null;
        URLConnectionAdapter con = brc.openGetConnection(thelink);
        if (con.getResponseCode() == 200) {
            file = JDUtilities.getResourceFile("tmp/ldttemp/" + test.replaceAll("(:|/)", "") + ".dlc");
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
            // decryptedLinks =
            // JDUtilities.getController().getContainerLinks(file);
            if (decryptedLinks.size() > 0) return decryptedLinks;
        } else {
            return null;
        }
        return null;
    }
}
