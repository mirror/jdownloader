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
                if (br.getRedirectLocation().contains("/Warning/?err_num=15")) return decryptedLinks;
                return null;
            }
            fpName = br.getRegex("<td width=\"93%\"><h1>(.*?)</h1>").getMatch(0);
            String[] links = br.getRegex("<td width=\"10%\" align=\"center\"><a href=\"(/.*?)\"").getColumn(0);
            if (links == null || links.length == 0) links = br.getRegex("\"(/UserDownloads/.*?)\"").getColumn(0);
            if (links == null || links.length == 0) return null;
            for (String dl : links)
                decryptedLinks.add(createDownloadlink("http://tgf-services.com" + dl));
        } else {
            // Decrypt sub-links of normal downloadlinks
            String allIDs = br.getRegex("id=\"inner_ids\" value=\"([0-9,]+)\"").getMatch(0);
            if (allIDs != null) {
                fpName = br.getRegex("<td width=\"93%\"><h1>(.*?)</h1></td>").getMatch(0);
                String[] subFileIDs = allIDs.split(",");
                int counter = 1;
                for (String subFileID : subFileIDs) {
                    DownloadLink dl = createDownloadlink(br.getURL().replace("/UserDownloads/", "/UserDownloadsdecrypted" + subFileID + "/"));
                    dl.setProperty("startNumber", counter);
                    // Do available check directly, works very fast
                    String filename = br.getRegex("<td width=\"5%\" align=\"right\">" + counter + "\\.</td>[\t\n\r ]+<td width=\"55%\" class=\"left\">([^<>\"]+)</td>[\t\n\r ]+<td width=\"20%\" align=\"center\">[\t\n\r ]+<a class=\"free\\-download\" id=\"free\\-download\" href=\"javascript:void\\(0\\);\"></a>[\t\n\r ]+</td>[\t\n\r ]+<td width=\"20%\" align=\"center\">[\t\n\r ]+<a class=\"premium\\-download\" id=\"premium\\-download\" href=\"javascript:void\\(0\\);\"></a>[\t\n\r ]+</td>[\t\n\r ]+</tr>[\t\n\r ]+<tr>[\t\n\r ]+<\\!\\-\\-DOWNLOAD TR\\-\\->[\t\n\r ]+<td colspan=\"4\">[\t\n\r ]+<\\!\\-\\-PREMIUM\\-\\->[\t\n\r ]+<div align=\"center\" style=\"display: none;\" class=\"download\\-premium\\-window\" id=\"download\\-premium\\-window\\-" + subFileID + "\">").getMatch(0);
                    if (filename != null) {
                        dl.setFinalFileName(counter + "." + Encoding.htmlDecode(filename.trim() + ".mp3"));
                        dl.setAvailable(true);
                    }
                    decryptedLinks.add(dl);
                    counter++;
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

}
