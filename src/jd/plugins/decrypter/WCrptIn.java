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
import jd.http.URLConnectionAdapter;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wcrypt.in" }, urls = { "http://(www\\.)?wcrypt\\.in/folder/[a-z0-9]+/" }, flags = { 0 })
public class WCrptIn extends PluginForDecrypt {

    public WCrptIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    // private static final String RECAPTCHATEXT =
    // "(/recaptcha/api/challenge\\?k=|api\\.recaptcha\\.net)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<DownloadLink> dlclinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.containsHTML("<li>Der Ordner wurde nicht gefunden")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        String folderKey = new Regex(parameter, "wcrypt\\.in/folder/(.+)/").getMatch(0);
        // Captchas can be skipped atm. just activate the code if they change
        // that, please don't remove the code!
        // if (br.containsHTML(RECAPTCHATEXT)) {
        // logger.info("reCaptcha found...");
        // for (int i = 0; i <= 5; i++) {
        // PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        // jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP)
        // recplug).getReCaptcha(br);
        // rc.parse();
        // rc.load();
        // File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        // String c = getCaptchaCode(cf, param);
        // String getPage =
        // "http://wcrypt.in/php/ajax/index.php?module=recaptcha&recaptcha_challenge_field="
        // + rc.getChallenge() + "&recaptcha_response_field=" + c +
        // "&folder_key=" + folderKey;
        // br.getPage(getPage);
        // if (br.containsHTML("false")) {
        // logger.info("User didn't enter the correct captcha, retrying...");
        // br.getPage(parameter);
        // continue;
        // }
        // break;
        // }
        // if (br.containsHTML("false")) throw new
        // DecrypterException(DecrypterException.CAPTCHA);
        // }
        String fpName = br.getRegex("<div class=\"content_con\"><h1>(.*?)</h1>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<title>(.*?) - wCrypt\\.in</title>").getMatch(0);
        br.getPage("http://wcrypt.in/php/ajax/index.php?module=folder&key=" + folderKey);
        if (br.containsHTML("Ordner nicht gefunden\\!")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        dlclinks = loadcontainer();
        if (dlclinks != null && dlclinks.size() != 0) {
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(dlclinks);
            }
            return dlclinks;
        }
        logger.info("Failed to get the links via DLC, trying webdecryption...");
        String[] links = br.getRegex("onclick=\\'mark_as_downloaded\\(\"\\d+\"\\);\\' href=\\'(http://wcrypt.in/.*?)\\'").getColumn(0);
        if (links == null || links.length == 0) links = br.getRegex("\\'(http://wcrypt\\.in/folder/file/[a-z0-9]+/[a-z0-9]+/)\\' ").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String singleLink : links) {
            br.getPage(singleLink);
            String finallink = br.getRedirectLocation();
            if (finallink == null) return null;
            DownloadLink dl = createDownloadlink(finallink);
            decryptedLinks.add(dl);
            progress.increase(1);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> loadcontainer() throws IOException, PluginException {
        String containerlink = br.getRegex("onclick=\\'window\\.open\\(\"(http://wcrypt.in/cache/container/(dlc|ccf|rsdf)/.*?\\.(dlc|ccf|rsdf))\"\\);").getMatch(0);
        if (containerlink == null) return null;
        ArrayList<DownloadLink> decryptedLinks = null;
        Browser brc = br.cloneBrowser();
        File file = null;
        URLConnectionAdapter con = brc.openGetConnection(containerlink);
        if (con.getResponseCode() == 200) {
            file = JDUtilities.getResourceFile("tmp/wcryptin/" + containerlink.replaceAll("(:|/|=|\\?)", "") + containerlink.substring(containerlink.length() - 4, containerlink.length()));
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
