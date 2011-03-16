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
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "blur.ws" }, urls = { "http://(www\\.)?blur\\.ws/view\\.php\\?id=\\w+" }, flags = { 0 })
public class BlurWs extends PluginForDecrypt {

    public BlurWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static final Object LOCK = new Object();
    private static final String RECAPTCHATEXT = "/recaptcha/api/challenge\\?k=";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        // Allow only 1 instance of this decrypter at the same time
        synchronized (LOCK) {
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            ArrayList<DownloadLink> dlclinks = new ArrayList<DownloadLink>();
            ArrayList<String> passwords = new ArrayList<String>();
            ArrayList<String> clearedlinks = new ArrayList<String>();
            br.getHeaders().put("User-Agent", RandomUserAgent.generate());
            String parameter = param.toString();
            br.setFollowRedirects(false);
            br.getPage(parameter);
            if (br.containsHTML(">Keine Daten vorhanden")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            if (br.containsHTML(RECAPTCHATEXT)) {
                logger.info("reCaptcha found...");
                for (int i = 0; i <= 5; i++) {
                    PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    rc.parse();
                    rc.load();
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    String c = getCaptchaCode(cf, param);
                    rc.setCode(c);
                    if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("wrongcaptcha")) {
                        logger.info("User didn't enter the correct captcha, retrying...");
                        br.getPage(parameter);
                        continue;
                    }
                    break;
                }
                if (br.containsHTML(RECAPTCHATEXT)) throw new DecrypterException(DecrypterException.CAPTCHA);
            }
            if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
            String fpName = br.getRegex("<title>blur :: Ordneransicht zu (.*?)</title>").getMatch(0);
            if (fpName == null) fpName = br.getRegex("<h3 class=\"center\">(.*?)</h3>").getMatch(0);
            String password = br.getRegex("<strong>Passwort:</strong>(.*?)</p>").getMatch(0);
            if (password == null) password = br.getRegex("type=\"hidden\" name=\"passwords\" value=\"(.*?)\">").getMatch(0);
            if (password != null && !password.equals("")) passwords.add(password);
            dlclinks = loadcontainer(parameter);
            if (dlclinks != null && dlclinks.size() != 0) {
                if (passwords != null && passwords.size() != 0) {
                    for (DownloadLink dlclink : dlclinks)
                        dlclink.setSourcePluginPasswordList(passwords);
                }
                if (fpName != null) {
                    FilePackage fp = FilePackage.getInstance();
                    fp.setName(fpName.trim());
                    fp.addLinks(dlclinks);
                }
                return dlclinks;
            }
            logger.info("Failed to get the links via DLC, trying webdecryption...");
            String[] links = br.getRegex("<li><a href=\"(http://.*?)\"").getColumn(0);
            if (links == null || links.length == 0) links = br.getRegex("\"(http://(www\\.)blur\\.ws/out\\.php\\?link=[0-9a-z-]+)\"").getColumn(0);
            if (links == null || links.length == 0) return null;
            for (String aLink : links)
                if (!clearedlinks.contains(aLink)) clearedlinks.add(aLink);
            progress.setRange(clearedlinks.size());
            for (String singleLink : clearedlinks) {
                br.getPage(singleLink);
                String finallink = br.getRedirectLocation();
                if (finallink == null) return null;
                if (!finallink.contains("/ref?id=")) {
                    DownloadLink dl = createDownloadlink(finallink);
                    if (passwords != null && passwords.size() != 0) dl.setSourcePluginPasswordList(passwords);
                    decryptedLinks.add(dl);
                }
                progress.increase(1);
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
            return decryptedLinks;
        }
    }

    private ArrayList<DownloadLink> loadcontainer(String theLink) throws IOException, PluginException {
        ArrayList<DownloadLink> decryptedLinks = null;
        Browser brc = br.cloneBrowser();
        theLink = Encoding.htmlDecode(theLink);
        File file = null;
        brc.getHeaders().put("Referer", theLink);
        URLConnectionAdapter con = brc.openGetConnection("http://www.blur.ws/out.php?link=dlc");
        if (con.getResponseCode() == 200) {
            file = JDUtilities.getResourceFile("tmp/blurws/" + theLink.replaceAll("(:|/|=|\\?)", "") + ".dlc");
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
