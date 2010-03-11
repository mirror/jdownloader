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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "chomikuj.pl" }, urls = { "http://[\\w\\.]*?chomikuj\\.pl/.+" }, flags = { 0 })
public class ChoMikujPl extends PluginForDecrypt {

    public ChoMikujPl(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String fpName = br.getRegex("<title>(.*?) - .*? - Chomikuj\\.pl.*?</title>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("class=\"T_selected\">(.*?)</span>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<span id=\"ctl00_CT_FW_SelectedFolderLabel\" style=\"font-weight:bold;\">(.*?)</span>").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("").getMatch(0);
                }
            }
        }
        String subFolderID = br.getRegex("id=\"ctl00_CT_FW_SubfolderID\" value=\"(.*?)\"").getMatch(0);
        if (subFolderID == null) subFolderID = br.getRegex("name=\"ChomikSubfolderId\" type=\"hidden\" value=\"(.*?)\"").getMatch(0);
        // Find number of pages
        String[] lolpages = br.getRegex("href=\"javascript:;\">(\\d+)<").getColumn(0);
        if (lolpages == null || lolpages.length == 0 || subFolderID == null || fpName == null) return null;
        ArrayList<String> pages = new ArrayList<String>();
        // Remove double-entrys
        for (String page : lolpages) {
            if (!pages.contains(page)) pages.add(page);
        }
        progress.setRange(pages.size());
        for (int i = 0; i < pages.size(); ++i) {
            String postdata = "ctl00%24CT%24FW%24SubfolderID=" + subFolderID.trim() + "&GalSortType=0&GalPage=" + i + "&__EVENTTARGET=ctl00%24CT%24FW%24RefreshButton&__ASYNCPOST=true&";
            br.postPage(parameter, postdata);
            // Every full page has 24 links (pictures)
            // This regex finds all links to PICTUREs, the site also got .rar
            // files but the regex doesn't find them because you need to be
            // logged in to download them anyways
            String[] fileId = br.getRegex("href=\"/Image\\.aspx\\?id=(\\d+)\"").getColumn(0);
            String[] links = br.getRegex("(<a class=\"gallery\" href=\".*?\".*?<a class=\"photoLnk\" href=\".*?\")").getColumn(0);
            if (fileId == null || fileId.length == 0) return null;
            if (links == null || links.length == 0) return null;
            if (links.length != fileId.length) return null;

            for (int j = 0; j < fileId.length; ++j) {
                String id = fileId[j];
                String fileEnding = new Regex(links[j], "class=\"photoLnk\" href=\".*?(\\..{2,4})\"").getMatch(0);
                String finalLink = String.format("&id=%s&gallerylink=%s&", id, param.toString().replace("chomikuj.pl", "60423fhrzisweguikipo9re"));
                DownloadLink dl = createDownloadlink(finalLink);
                dl.setFinalFileName(fpName + "_" + fileId[j] + fileEnding);
                // All found links are 99,99% available as long as the decrypter
                // reaches this line of code^^
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
            progress.increase(1);
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName.trim());
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
