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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4megaupload.com" }, urls = { "http://[\\w\\.]*?4megaupload\\.com/download/.*?-\\d+\\.html" }, flags = { 0 })
public class FourMuCm extends PluginForDecrypt {

    public FourMuCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String CAPTCHATEXT = "code\\.php";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().equals("http://4megaupload.com/index.php")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            return null;
        }
        if (!br.containsHTML(CAPTCHATEXT)) return null;
        for (int i = 0; i <= 8; i++) {
            br.postPage(parameter, "c_code=" + getCaptchaCode("http://4megaupload.com/code.php", param) + "&act=+Download+");
            if (!br.containsHTML(CAPTCHATEXT)) break;
        }
        if (br.containsHTML(CAPTCHATEXT)) throw new DecrypterException(DecrypterException.CAPTCHA);
        String fpName = br.getRegex("name=\"description\" content=\"(.*?) megaupload downloads\"").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<title>(.*?) - download from Megaupload</title>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("DOWNLOAD:[\r\t\n ]+<h1>(.*?)</h1>").getMatch(0);
            }
        }
        String singleLink = br.getRegex("<td width=\"60\" align=\"center\"><a href=(.*?)  target=\"_blank\"").getMatch(0);
        if (singleLink == null) {
            singleLink = br.getRegex("class=download_link_dwn><a href=(.*?)  target=\"_blank\"").getMatch(0);
            if (singleLink == null) {
                singleLink = br.getRegex("<center><input class=\"layer\" value=\"(.*?)\" onclick=\"this\\.select\\(\\);\"").getMatch(0);
                if (singleLink == null) {
                    singleLink = br.getRegex("<td width=\"75\" align=\"center\" valign=\"middle\"><a href=(.*?)  target=\"_blank\"").getMatch(0);
                }
            }
        }
        if (singleLink != null) {
            logger.info("Single link found, adding it");
            decryptedLinks.add(createDownloadlink(singleLink));
        } else {
            logger.info("No single link found, trying to get all links...");
            String[] allLinks = br.getRegex("class=\"download_link_rel_download\"><a target=\"_blank\" href=\"(.*?)\" rel=\"nofollow\"").getColumn(0);
            if (allLinks == null || allLinks.length == 0) return null;
            for (String singleLnk : allLinks)
                decryptedLinks.add(createDownloadlink(singleLnk));
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
