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
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "stealth.to" }, urls = { "http://[\\w\\.]*?stealth\\.to/(\\?id\\=[\\w]+|index\\.php\\?id\\=[\\w]+|\\?go\\=captcha&id=[\\w]+)|http://[\\w\\.]*?stealth\\.to/folder/[\\w]+" }, flags = { 0 })
public class Stlth extends PluginForDecrypt {

    public Stlth(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception, DecrypterException {

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>(0);
        String url = param.toString();
        String stealthID;
        int idx = url.indexOf("=");
        if (idx > 0) {
            stealthID = url.substring(idx + 1, url.length());
        } else {
            stealthID = url.substring(url.lastIndexOf("/") + 1);
        }
        this.setBrowserExclusive();
        br.setDebug(true);
        br.setFollowRedirects(true);
        br.getPage(url);
        if (br.containsHTML("besucherpass.png")) {

            Form form = br.getFormBySubmitvalue("Weiter");
            form.put("access_pass", getUserInput(null, param));
            br.submitForm(form);

        }
        if (br.containsHTML("Sicherheitsabfrage")) {
            logger.fine("The current page is captcha protected, getting captcha ID...");
            int max = 3;
            for (int i = 0; i <= max; i++) {
                String recaptchaID = br.getRegex("k=([a-zA-Z0-9]+)\"").getMatch(0);
                Form captchaForm = br.getFormBySubmitvalue("Ordner+%C3%B6ffnen");
                if (recaptchaID == null || captchaForm == null) return null;
                logger.fine("The current recaptcha ID is '" + recaptchaID + "'");
                logger.fine("The current stealth ID is '" + stealthID + "'");
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.setId(recaptchaID);
                rc.setForm(captchaForm);
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, param);
                rc.setCode(c);
                if (br.containsHTML("api\\.recaptcha\\.net")) continue;
                break;
            }
            if (br.containsHTML("api\\.recaptcha\\.net")) throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        String containerDownloadLink = br.getRegex("\"(http://[a-z]+\\.stealth\\.to/dlc\\.php\\?name=[a-z0-9]+)\"").getMatch(0);
        if (containerDownloadLink == null) {
            logger.warning("containerDownloadLink equals null");
            return null;
        }
        String name = br.getRegex("<span class=\"Name\">(.*?)</span>").getMatch(0);
        String pass = br.getRegex("<span class=\".*?\">Passwort: (.*?)</span>").getMatch(0);
        Browser brc = br.cloneBrowser();
        File file = null;
        URLConnectionAdapter con = brc.openGetConnection(containerDownloadLink);
        if (con.getResponseCode() == 200) {
            file = JDUtilities.getResourceFile("tmp/stealthto/" + containerDownloadLink.replaceAll("(:|/|\\?|=)", "") + ".dlc");
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

        int numberOfDecryptedLinks = decryptedLinks.size();
        if (numberOfDecryptedLinks == 0) {
            logger.warning("There were no links obtained for the URL '" + url + "'");
            return null;
        }
        if (name != null || pass != null) {
            FilePackage fp = FilePackage.getInstance();
            if (name != null) fp.setName(name);
            if (pass != null) fp.setPassword(pass);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
