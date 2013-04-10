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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tgf-services.com" }, urls = { "http://(www\\.)?tgf\\-services\\.com/(downloads/[a-zA-Z0-9_]+|UserDownloads/.+)" }, flags = { 0 })
public class TgfServicesComFolder extends PluginForDecrypt {

    public TgfServicesComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String fpName = null;
        // Decrypt folders
        if (br.getURL().contains("tgf-services.com/downloads/")) {
            if (br.getRedirectLocation() != null) {
                if (br.getRedirectLocation().contains("/Warning/?err_num=15")) {
                    logger.info("Link offline: " + parameter);
                    return decryptedLinks;
                }
                return null;
            }
            if (br.containsHTML("<h2>Bitrate: , Frequency: , Mode: </h2>")) {
                logger.info("Empty folder: " + parameter);
                return decryptedLinks;
            }
            fpName = br.getRegex("<td width=\"93%\"><h1>(.*?)</h1>").getMatch(0);
            String[] links = br.getRegex("<td width=\"10%\" align=\"center\"><a href=\"(/.*?)\"").getColumn(0);
            if (links == null || links.length == 0) links = br.getRegex("\"(/UserDownloads/.*?)\"").getColumn(0);
            if (links == null || links.length == 0) return null;
            for (String dl : links)
                decryptedLinks.add(createDownloadlink("http://tgf-services.com" + dl));
        } else {
            // Decrypt sub-links of normal downloadlinks
            String[][] allinfo = br.getRegex("id=\"button_download_(\\d+)\" onclick=\"\" value=\"\" class=\"btn btn\\-download\\-small2\">.*?<td width=\"5%\" align=\"right\">(\\d+)\\.</td>[\t\n\r ]+<td width=\"55%\" class=\"left\">([^<>\"]*?)</td>").getMatches();
            if (allinfo != null) {
                fpName = br.getRegex("<td width=\"93%\"><h1>(.*?)</h1></td>").getMatch(0);
                for (final String[] linkInfo : allinfo) {
                    final DownloadLink dl = createDownloadlink(br.getURL().replace("/UserDownloads/", "/UserDownloadsdecrypted" + linkInfo[0] + "/"));
                    dl.setProperty("startNumber", linkInfo[1]);
                    dl.setFinalFileName(linkInfo[1] + "." + Encoding.htmlDecode(linkInfo[2].trim()) + ".mp3");
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                }
            } else {
                decryptedLinks.add(createDownloadlink(parameter.replace("/UserDownloads/", "/UserDownloadsdecrypted/")));
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}